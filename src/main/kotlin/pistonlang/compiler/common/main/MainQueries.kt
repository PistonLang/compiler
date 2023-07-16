package pistonlang.compiler.common.main

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import pistonlang.compiler.common.files.PackageTreeNode
import pistonlang.compiler.common.items.*
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
    private val fileHandler: Query<FileHandle, LanguageHandler<*>?> = DependentQuery(versionData) { key ->
        val ext = key.path.substringAfterLast('.')
        inputs.postfixHandler[ext]
    }

    private val childHandler: Query<MemberHandle, LanguageHandler<*>?> =
        DependentQuery(versionData, computeFn = ::findChildHandler)

    context(QueryAccessor)
    private fun findChildHandler(handle: MemberHandle): LanguageHandler<*>? {
        val parent = handle.parent
        return if (parent.isFile) fileHandler[parent as FileHandle]
        else childHandler[parent as MemberHandle]
    }

    val packageHandleNode: Query<PackageHandle, PackageTreeNode?> = DependentQuery(
        versionData,
        equalityFun = { old, new -> old?.lastUpdated == new?.lastUpdated },
        computeFn = { key -> inputs.packageTree[Unit].packages[key] }
    )

    val packageItems: Query<PackageHandle, Map<String, List<ItemHandle>>> = DependentQuery(versionData) fn@{ key ->
        val node = packageHandleNode[key] ?: return@fn emptyMap<String, List<ItemHandle>>()
        val res = mutableMapOf<String, MutableList<ItemHandle>>()
        node.children.forEach { handle ->
            res.getOrPut(handle.suffix) { mutableListOf() }.add(handle)
        }
        node.files.forEach { file ->
            val handler = fileHandler[file] ?: return@forEach
            handler.fileItems[file].forEach { (name, list) ->
                MemberType.entries.forEach { type ->
                    list.iteratorFor(type).withIndex().forEach { (index, _) ->
                        res.getOrPut(name) { mutableListOf() }.add(type.buildHandle(file, name, index))
                    }
                }
            }
        }
        res
    }

    val childItems: Query<MemberHandle, Map<String, List<ItemHandle>>> = DependentQuery(versionData) fn@{ key ->
        val res = mutableMapOf<String, MutableList<ItemHandle>>()
        val handler = childHandler[key] ?: return@fn emptyMap<String, List<ItemHandle>>()
        handler.childItems[key].forEach { (name, list) ->
            MemberType.entries.forEach { type ->
                list.iteratorFor(type).withIndex().forEach { (index, _) ->
                    res.getOrPut(name) { mutableListOf() }.add(type.buildHandle(key, name, index))
                }
            }
        }
        res
    }

    val typeParams: Query<MemberHandle, Map<String, List<TypeParamHandle>>> = DependentQuery(versionData) fn@{ key ->
        val handler = childHandler[key] ?: return@fn emptyMap<String, List<TypeParamHandle>>()
        handler.typeParams[key].withIndex().groupBy({ it.value.first }) {
            TypeParamHandle(key, it.index)
        }
    }

    val constructors: Query<MultiInstanceClassHandle, List<ConstructorHandle>> = DependentQuery(versionData) fn@{ key ->
        val handler = childHandler[key] ?: return@fn emptyList<ConstructorHandle>()
        handler.constructors[key].indices.map { ConstructorHandle(key, it) }
    }

    val defaultInstance: Query<NewTypeHandle, TypeInstance> = DependentQuery(versionData) fn@{ key ->
        val handler = childHandler[key] ?: return@fn errorInstance
        val args = 0
            .until(handler.typeParams[key].size)
            .map { TypeInstance(TypeParamHandle(key, 0), emptyList(), false) }

        TypeInstance(key, args, false)
    }


    // TODO: Handle cycles
    val supertypeDAG: Query<NewTypeHandle, SupertypeDAG> = DependentQuery(versionData) fn@{ key ->
        val handler = childHandler[key] ?: return@fn emptyTypeDAG
        val superTypes = handler.supertypes[key].data
        val typeArgs = defaultInstance[key].args

        val withoutCurr = superTypes.fold<TypeInstance, SupertypeDAG>(emptyTypeDAG) { acc, value ->
            val forCurrent = newDAGFor(value) ?: return@fold acc
            mergeDAGs(acc, forCurrent)
        }
        SupertypeDAG(persistentSetOf(key), withoutCurr.nodes.put(key, TypeDAGNode(typeArgs, withoutCurr.lowest)))
    }

    context(QueryAccessor)
    private fun newDAGFor(instance: TypeInstance): SupertypeDAG? {
        val type = instance.type as? NewTypeHandle ?: return null
        val oldDAG = supertypeDAG[type]
        return TypeDAG(
            oldDAG.lowest,
            oldDAG.nodes.put(type, TypeDAGNode(instance.args, oldDAG.nodes[type]!!.parents))
        )
    }

    private fun mergeDAGs(left: SupertypeDAG, right: SupertypeDAG): SupertypeDAG {
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

        return SupertypeDAG(newLowest, nodes)
    }

    private fun checkArgs(
        left: List<TypeInstance>,
        leftNodes: Map<TypeHandle, TypeDAGNode<NewTypeHandle>>,
        right: List<TypeInstance>,
        rightNodes: Map<TypeHandle, TypeDAGNode<NewTypeHandle>>
    ): List<TypeInstance> = left.zip(right) { l, r ->
        if (l.nullable != r.nullable) return@zip errorInstance

        val newLeft = (l.type as? TypeParamHandle)?.let { leftNodes.resolveParam(it, l.nullable) } ?: l
        val newRight = (r.type as? TypeParamHandle)?.let { rightNodes.resolveParam(it, r.nullable) } ?: r

        if (newLeft.type == newRight.type) {
            val args = checkArgs(newLeft.args, leftNodes, newRight.args, rightNodes)
            TypeInstance(newLeft.type, args, newLeft.nullable)
        } else errorInstance
    }
}

context(QueryAccessor)
fun PackageHandle.hierarchyIterator(queries: GeneralQueries) = object : Iterator<MemberHandle> {
    private var iterStack = Stack<Iterator<MemberHandle>>()
    private var iter = queries
        .packageItems[this@hierarchyIterator]
        .asSequence()
        .flatMap { (_, list) -> list }
        .filterIsInstance<MemberHandle>().iterator()

    override fun hasNext(): Boolean = iter.hasNext()

    private fun findNext(handle: MemberHandle) {
        val next = queries
            .childItems[handle]
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

    override fun next(): MemberHandle {
        val res = iter.next()
        findNext(res)
        return res
    }
}