package pistonlang.compiler.common.main

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import pistonlang.compiler.common.files.PackagePath
import pistonlang.compiler.common.files.PackageTreeNode
import pistonlang.compiler.common.items.MemberId
import pistonlang.compiler.common.items.MemberType
import pistonlang.compiler.common.items.PackageId
import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.handles.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.main.stl.STLTypes
import pistonlang.compiler.common.queries.*
import pistonlang.compiler.common.types.*
import java.util.*
import kotlin.collections.ArrayDeque

class MainQueries internal constructor(
    versionData: QueryVersionData,
    private val interners: InstanceInterners,
    private val inputs: InputQueries,
) {
    private val childHandler: Query<MemberId, LanguageHandler<*>?> =
        DependentQuery(versionData, computeFn = ::findChildHandler)

    val stlTypes: SingletonQuery<STLTypes> = DependentSingletonQuery(versionData) {
        val pack = interners.packIds[PackagePath("piston")]
        val items = packageItems[pack]

        STLTypes(
            int8 = getSTLType("Int8", items),
            int16 = getSTLType("Int16", items),
            int32 = getSTLType("Int32", items),
            int64 = getSTLType("Int64", items),
            float32 = getSTLType("Float32", items),
            float64 = getSTLType("Float64", items),
            bool = getSTLType("Bool", items),
            char = getSTLType("Char", items),
            string = getSTLType("String", items),
            any = getSTLType("Any", items),
            nothing = getSTLType("Nothing", items),
            unit = getSTLType("Unit", items),
        )
    }

    private fun getSTLType(name: String, items: Map<String, List<ItemHandle>>) =
        items[name]!![0].asMember!!.let { interners.typeIds[it] }

    context(QueryAccessor)
    private fun findChildHandler(id: MemberId): LanguageHandler<*>? = interners.memberIds.getKey(id).parent.match(
        onFile = { inputs.fileHandler[it] },
        onMember = { childHandler[it] }
    )

    val packageHandleNode: Query<PackageId, PackageTreeNode> = DependentQuery(
        versionData,
        equalityFun = { old, new -> old.lastUpdated == new.lastUpdated },
        computeFn = { key -> inputs.packageTree.value.packages[key]!! }
    )

    val packageItems: Query<PackageId, Map<String, List<ItemHandle>>> = DependentQuery(versionData) fn@{ key ->
        val node = packageHandleNode[key]
        val res = mutableMapOf<String, MutableList<ItemHandle>>()
        node.children.forEach { (name, handle) ->
            res.getOrPut(name) { mutableListOf() }.add(handle.asItem())
        }
        node.files.forEach { file ->
            val handler = inputs.fileHandler[file] ?: return@forEach
            val items = handler.fileItems[file]
            MemberType.entries.forEach { type ->
                items.iteratorFor(type).forEach { (name, list) ->
                    list.indices.forEach { index ->
                        val reference = MemberHandle(file.asParent(), type, name, index)
                        val refId = interners.memberIds.getOrAdd(reference)
                        if (reference.type.newType) interners.typeIds.add(refId)
                        res.getOrPut(name) { mutableListOf() }.add(refId.asItem())
                    }
                }
            }
        }
        res
    }

    val childItems: Query<MemberId, Map<String, List<MemberId>>> = DependentQuery(versionData) fn@{ key ->
        val res = mutableMapOf<String, MutableList<MemberId>>()
        val handler = childHandler[key] ?: return@fn emptyMap<String, List<MemberId>>()
        MemberType.entries.forEach { type ->
            handler.childItems[key].iteratorFor(type).forEach { (name, list) ->
                list.indices.forEach { index ->
                    val reference = MemberHandle(key.asParent(), type, name, index)
                    val refId = interners.memberIds.getOrAdd(reference)
                    if (reference.type.newType) interners.typeIds.add(refId)
                    res.getOrPut(name) { mutableListOf() }.add(refId)
                }
            }
        }
        res
    }

    val typeParams: Query<MemberId, TypeParamData> = DependentQuery(versionData) fn@{ key ->
        val handler = childHandler[key] ?: return@fn emptyTypeParamData
        val mapped = handler.typeParams[key].mapIndexed { index, (name) ->
            val id = interners.typeParamIds.getOrAdd(TypeParamHandle(key, index))
            name to id
        }

        TypeParamData(mapped.map { it.second }, mapped.groupBy({ it.first }) { it.second })
    }

    val defaultInstance: Query<TypeId, TypeInstance> = DependentQuery(versionData) fn@{ key ->
        val memberId = interners.typeIds.getKey(key)
        val args = typeParams[memberId].ids.map { TypeInstance(it.asType(), emptyList(), false) }

        TypeInstance(key.asType(), args, false)
    }


    // TODO: Handle cycles
    val supertypeDAG: Query<TypeId, TypeDAG> = DependentQuery(versionData) fn@{ key ->
        val memberId = interners.typeIds.getKey(key)
        val handler = childHandler[memberId] ?: return@fn emptyTypeDAG
        val superTypes = handler.supertypes[key].data
        val typeArgs = defaultInstance[key].args
        val handle = key.asType()

        val withoutCurr =
            superTypes.fold(emptyTypeDAG) { acc: TypeDAG, value: TypeInstance ->
                val forCurrent = newDAGFor(value) ?: return@fold acc
                mergeDAGs(acc, forCurrent)
            }
        TypeDAG(persistentSetOf(handle), withoutCurr.nodes.put(handle, TypeDAGNode(typeArgs, withoutCurr.lowest)))
    }

    context(QueryAccessor)
    private fun newDAGFor(instance: TypeInstance): TypeDAG? {
        val type = instance.type.asType ?: return null
        val handle = type.asType()
        val oldDAG = supertypeDAG[type]
        return TypeDAG(
            oldDAG.lowest,
            oldDAG.nodes.put(handle, TypeDAGNode(instance.args, oldDAG.nodes[handle]!!.parents))
        )
    }

    private fun mergeDAGs(left: TypeDAG, right: TypeDAG): TypeDAG {
        if (left.nodes.size < right.nodes.size) return mergeDAGs(right, left)

        val seen = HashSet(left.lowest)
        val newLowest = (left.lowest.asSequence().filter { it !in right.nodes || it in right.lowest } +
                right.lowest.asSequence().filter { it !in left.nodes }).toPersistentSet()
        var nodes = left.nodes
        val q = ArrayDeque(right.lowest)

        while (q.isNotEmpty()) {
            val curr = q.removeFirst()
            if (curr in nodes) {
                val leftNode = nodes[curr]!!
                val rightNode = right.nodes[curr]!!
                nodes = nodes.put(
                    curr,
                    TypeDAGNode(
                        checkArgs(leftNode.args, nodes, rightNode.args, right.nodes),
                        leftNode.parents
                    )
                )
            } else {
                val node = right.nodes[curr]!!
                nodes = nodes.put(curr, node)
                node.parents.asSequence().filter { it !in seen }.forEach {
                    seen.add(it)
                    q.add(it)
                }
            }
        }

        return TypeDAG(newLowest, nodes)
    }

    private fun checkArgs(
        left: List<TypeInstance>,
        leftNodes: Map<TypeHandle, TypeDAGNode>,
        right: List<TypeInstance>,
        rightNodes: Map<TypeHandle, TypeDAGNode>
    ): List<TypeInstance> = left.zip(right) { l, r ->
        if (l.nullable != r.nullable) return@zip unknownInstance

        val newLeft = l.type.asTypeParam?.let { leftNodes.resolveParam(it, l.nullable, interners) } ?: l
        val newRight = r.type.asTypeParam?.let { rightNodes.resolveParam(it, r.nullable, interners) } ?: r

        if (newLeft.type == newRight.type) {
            val args = checkArgs(newLeft.args, leftNodes, newRight.args, rightNodes)
            TypeInstance(newLeft.type, args, newLeft.nullable)
        } else unknownInstance
    }
}

context(QueryAccessor)
fun PackageId.hierarchyMemberIterator(interners: MainInterners, queries: MainQueries) = object : Iterator<MemberId> {
    private var iterStack = Stack<Iterator<MemberHandle>>()
    private var iter = queries
        .packageItems[this@hierarchyMemberIterator]
        .asSequence()
        .flatMap { (_, list) -> list }
        .filterIsInstance<MemberHandle>().iterator()

    override fun hasNext(): Boolean = iter.hasNext()

    private fun findNext(id: MemberId) {
        val next = queries
            .childItems[id]
            .asSequence()
            .flatMap { (_, list) -> list }
            .filterIsInstance<MemberHandle>()
            .iterator()

        if (next.hasNext()) {
            iterStack.push(iter)
            iter = next
        } else findNext()
    }

    private tailrec fun findNext() {
        if (iter.hasNext() || iterStack.isEmpty()) return
        iter = iterStack.pop()
        findNext()
    }

    override fun next(): MemberId {
        val res = interners.memberIds[iter.next()]
        findNext(res)
        return res
    }
}