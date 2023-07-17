package pistonlang.compiler.common.main

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import pistonlang.compiler.common.files.PackageTreeNode
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.items.handles.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.queries.DependentQuery
import pistonlang.compiler.common.queries.Query
import pistonlang.compiler.common.queries.QueryAccessor
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.common.types.*
import java.util.*
import kotlin.collections.ArrayDeque

class GeneralQueries internal constructor(
    versionData: QueryVersionData,
    private val inputs: InputQueries,
) {
    private val mutableInterners = DefaultInterners()

    val interners: MainInterners
        get() = mutableInterners

    private val childHandler: Query<MemberId, LanguageHandler<*>?> =
        DependentQuery(versionData, computeFn = ::findChildHandler)

    context(QueryAccessor)
    private fun findChildHandler(id: MemberId): LanguageHandler<*>? = interners.memberIds[id].parent.match(
        onFile = { inputs.fileHandler[it] },
        onMember = { childHandler[it] }
    )

    val packageHandleNode: Query<PackageId, PackageTreeNode> = DependentQuery(
        versionData,
        equalityFun = { old, new -> old.lastUpdated == new.lastUpdated },
        computeFn = { key -> inputs.packageTree[UnitId].packages[key]!! }
    )

    val packageItems: Query<PackageId, Map<String, List<ItemHandle>>> = DependentQuery(versionData) fn@{ key ->
        val node = packageHandleNode[key]
        val res = mutableMapOf<String, MutableList<ItemHandle>>()
        node.children.forEach { (name, handle) ->
            res.getOrPut(name) { mutableListOf() }.add(ItemHandle(handle))
        }
        node.files.forEach { file ->
            val handler = inputs.fileHandler[file] ?: return@forEach
            MemberType.entries.forEach { type ->
                handler.fileItems[file].iteratorFor(type).forEach { (name, list) ->
                    list.indices.forEach { index ->
                        val reference = MemberHandle(ParentHandle(file), type, name, index)
                        val refId = mutableInterners.memberIds.getOrAdd(reference)
                        if (reference.type.newType) mutableInterners.typeIds.add(refId)
                        res.getOrPut(name) { mutableListOf() }.add(ItemHandle(refId))
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
                    val reference = MemberHandle(ParentHandle(key), type, name, index)
                    val refId = mutableInterners.memberIds.getOrAdd(reference)
                    if (reference.type.newType) mutableInterners.typeIds.add(refId)
                    res.getOrPut(name) { mutableListOf() }.add(refId)
                }
            }
        }
        res
    }

    val typeParams: Query<MemberId, TypeParamData> = DependentQuery(versionData) fn@{ key ->
        val handler = childHandler[key] ?: return@fn emptyTypeParamData
        val mapped = handler.typeParams[key].mapIndexed { index, (name) ->
            val id = mutableInterners.typeParamIds.getOrAdd(TypeParamHandle(key, index))
            name to id
        }

        TypeParamData(mapped.map { it.second }, mapped.groupBy({ it.first }) { it.second })
    }

    val defaultInstance: Query<TypeId, TypeInstance> = DependentQuery(versionData) fn@{ key ->
        val memberId = interners.typeIds[key]
        val args = typeParams[memberId].ids.map { TypeInstance(TypeHandle(it), emptyList(), false) }

        TypeInstance(TypeHandle(key), args, false)
    }


    // TODO: Handle cycles
    val supertypeDAG: Query<TypeId, TypeDAG> = DependentQuery(versionData) fn@{ key ->
        val memberId = interners.typeIds[key]
        val handler = childHandler[memberId] ?: return@fn emptyTypeDAG
        val superTypes = handler.supertypes[key].data
        val typeArgs = defaultInstance[key].args
        val handle = TypeHandle(key)

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
        val handle = TypeHandle(type)
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
fun PackageId.hierarchyIterator(queries: GeneralQueries) = object : Iterator<MemberId> {
    private var iterStack = Stack<Iterator<MemberHandle>>()
    private var iter = queries
        .packageItems[this@hierarchyIterator]
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
        val res = queries.interners.memberIds[iter.next()]
        findNext(res)
        return res
    }
}