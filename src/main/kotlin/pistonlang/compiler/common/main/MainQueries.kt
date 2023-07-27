package pistonlang.compiler.common.main

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import pistonlang.compiler.common.files.PackagePath
import pistonlang.compiler.common.files.PackageTreeNode
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.items.handles.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.main.stl.STLTypes
import pistonlang.compiler.common.queries.*
import pistonlang.compiler.common.types.*
import pistonlang.compiler.util.SingletonIterator
import java.util.*
import kotlin.collections.ArrayDeque


interface MainQueries {
    val stlTypes: SingletonQuery<STLTypes>
    val packageHandleNode: Query<PackageId, PackageTreeNode>
    val packageItems: Query<PackageId, Map<String, List<ItemHandle>>>
    val childItems: Query<MemberId, Map<String, List<MemberId>>>
    val typeParams: Query<MemberId, TypeParamData>
    val defaultInstance: Query<TypeId, TypeInstance>
    val supertypeDAG: Query<TypeId, SupertypeDAG>
    val params: Query<MemberId, List<TypeInstance>>
    val returnType: Query<MemberId, TypeInstance>
    val typeParamBounds: Query<MemberId, List<TypeParamBounds>>
    val virtualMembers: Query<TypeId, VirtualMembers>
    context(QueryAccessor)
    fun packageTreeIterator(startId: PackageId): Iterator<PackageId>
}

internal class DefaultMainQueries(
    versionData: QueryVersionData,
    private val interners: InstanceInterners,
    private val inputs: InputQueries,
) : MainQueries {
    private val childHandler: Query<MemberId, LanguageHandler<*>> =
        DependentQuery(versionData, computeFn = ::findChildHandler)

    override val stlTypes: SingletonQuery<STLTypes> = DependentSingletonQuery(versionData) {
        val items = interners.packIds[PackagePath("piston")]
            ?.let { packageItems[it] }

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

    private fun getSTLType(name: String, items: Map<String, List<ItemHandle>>?): TypeHandle =
        items?.get(name)?.get(0)?.asMember?.let { interners.typeIds[it]?.asType() } ?: missingSTLType

    context(QueryAccessor)
    private fun findChildHandler(id: MemberId): LanguageHandler<*> = interners.memberIds.getKey(id).parent.match(
        onFile = { inputs.fileHandler[it] },
        onMember = { childHandler[it] }
    )

    override val packageHandleNode: Query<PackageId, PackageTreeNode> = DependentQuery(
        versionData,
        equalityFun = { old, new -> old.lastUpdated == new.lastUpdated },
        computeFn = { key -> inputs.packageTree.value.nodes[key]!! }
    )

    override val packageItems: Query<PackageId, Map<String, List<ItemHandle>>> = DependentQuery(versionData) fn@{ key ->
        val node = packageHandleNode[key]
        val res = mutableMapOf<String, MutableList<ItemHandle>>()
        node.children.forEach { (name, handle) ->
            res.getOrPut(name) { mutableListOf() }.add(handle.asItem())
        }
        node.files.forEach { file ->
            val handler = inputs.fileHandler[file]
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

    override val childItems: Query<MemberId, Map<String, List<MemberId>>> = DependentQuery(versionData) fn@{ key ->
        val res = mutableMapOf<String, MutableList<MemberId>>()
        val handler = childHandler[key]
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

    override val typeParams: Query<MemberId, TypeParamData> = DependentQuery(versionData) fn@{ key ->
        val handler = childHandler[key]
        val mapped = handler.typeParams[key].mapIndexed { index, (name) ->
            val id = interners.typeParamIds.getOrAdd(TypeParamHandle(key, index))
            name to id
        }

        TypeParamData(mapped.map { it.second }, mapped.groupBy({ it.first }) { it.second })
    }

    override val defaultInstance: Query<TypeId, TypeInstance> = DependentQuery(versionData) fn@{ key ->
        val memberId = interners.typeIds.getKey(key)
        val args = typeParams[memberId].ids.map { TypeInstance(it.asType(), emptyList(), false) }

        TypeInstance(key.asType(), args, false)
    }


    override val supertypeDAG: Query<TypeId, SupertypeDAG> = DependentQuery(
        versionData,
        cycleHandler = { SupertypeDAG(emptyTypeDAG, setOf(it), emptySet()) },
        computeFn = fn@{ key ->
            val memberId = interners.typeIds.getKey(key)
            val handler = childHandler[memberId]
            val superTypes = handler.supertypes[key].data
            val typeArgs = defaultInstance[key].args
            val excluding = mutableSetOf<TypeId>()
            val dependent = mutableSetOf<TypeId>()

            val withoutCurr = superTypes
                .asSequence()
                .mapNotNull { newDAGFor(it, key) }
                .fold(stlTypes.value.anyDAG) { acc, value ->
                    excluding.addAll(value.excluding)
                    dependent.addAll(value.dependent)
                    mergeDAGs(acc, value.dag)
                }

            val simpleDAG = TypeDAG(
                persistentSetOf(key),
                withoutCurr.nodes.put(key, TypeDAGNode(typeArgs, withoutCurr.lowest))
            )

            val newDependent = mutableSetOf<TypeId>()

            val dag = dependent.fold(simpleDAG) { acc, curr ->
                val (dag, toAdd) = updateDependent(curr, acc)
                if (toAdd) newDependent.add(curr)
                dag
            }

            if (typeArgs.isNotEmpty()) newDependent.add(key)

            SupertypeDAG(dag, excluding, newDependent)
        })

    context(QueryAccessor)
    private fun updateDependent(id: TypeId, dag: TypeDAG): Pair<TypeDAG, Boolean> {
        val node = dag.nodes[id]!!
        var dependent = false
        val newArgs = node.args.map {
            val (curr, stillDependent) = updateInstanceArgs(it, dag)
            dependent = dependent || stillDependent
            curr
        }
        return dag.copy(nodes = dag.nodes.put(id, node.copy(args = newArgs))) to dependent
    }

    context(QueryAccessor)
    private fun updateInstanceArgs(instance: TypeInstance, dag: TypeDAG): Pair<TypeInstance, Boolean> {
        val type = instance.type
        return type.asTypeParam?.let {
            val param = interners.typeParamIds.getKey(it)
            val parentId = interners.typeIds[param.parent]!!
            val newInstance = dag.nodes[parentId]!!.args[param.index]
            newInstance to (newInstance.type.type == TypeType.TypeParam)
        } ?: run {
            var toAdd = false
            val newArgs = instance.args.map {
                val (new, dependent) = updateInstanceArgs(it, dag)
                toAdd = toAdd || dependent
                new
            }
            instance.copy(args = newArgs) to toAdd
        }
    }

    override val params: Query<MemberId, List<TypeInstance>> = DependentQuery(versionData) { key ->
        childHandler[key].params[key].data
    }

    override val returnType: Query<MemberId, TypeInstance> = DependentQuery(versionData) { key ->
        childHandler[key].returnType[key].data
    }

    context(QueryAccessor)
    private fun newDAGFor(instance: TypeInstance, on: TypeId): SupertypeDAG? {
        val type = instance.type.asType ?: return null
        val oldData = supertypeDAG[type]
        val oldDAG = oldData.dag

        if (oldDAG.isEmpty()) return oldData

        return if (oldData.excluding.contains(on)) null else oldData.copy(
            dag = TypeDAG(
                oldDAG.lowest,
                oldDAG.nodes.put(type, TypeDAGNode(instance.args, oldDAG.nodes[type]!!.parents))
            )
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
        leftNodes: Map<TypeId, TypeDAGNode>,
        right: List<TypeInstance>,
        rightNodes: Map<TypeId, TypeDAGNode>
    ): List<TypeInstance> = left.zip(right) { l, r ->
        if (l.nullable != r.nullable) return@zip conflictingArgumentInstance

        val newLeft = l.type.asTypeParam?.let { leftNodes.resolveParam(it, l.nullable, interners) } ?: l
        val newRight = r.type.asTypeParam?.let { rightNodes.resolveParam(it, r.nullable, interners) } ?: r

        if (newLeft.type == newRight.type) {
            val args = checkArgs(newLeft.args, leftNodes, newRight.args, rightNodes)
            TypeInstance(newLeft.type, args, newLeft.nullable)
        } else conflictingArgumentInstance
    }

    override val typeParamBounds: Query<MemberId, List<TypeParamBounds>> =
        DependentQuery(versionData) { key ->
            val bounds = childHandler[key].typeParamBounds[key].data
            val params = typeParams[key].ids
            val baseBounds = stlTypes.value.anyParamBounds
            val res = MutableList(params.size) { baseBounds }

            bounds.forEach { (on, bound) ->
                addBound(res, on, bound, key)
            }

            res
        }

    // TODO: Keep track of errors
    context(QueryAccessor)
    private fun addBound(
        res: MutableList<TypeParamBounds>,
        id: TypeParamId,
        bound: TypeInstance,
        key: MemberId
    ) {
        val currentHandle = interners.typeParamIds.getKey(id)
        val current = res[currentHandle.index]
        val nullable = current.canBeNullable && bound.nullable
        bound.type.match(
            onType = { typeId ->
                if (typeId == stlTypes.value.nothing.asType) return
                val boundDAG = supertypeDAG[typeId].dag
                val boundNode = boundDAG.nodes[typeId]!!
                val modifiedDAG = boundDAG.copy(
                    nodes = boundDAG.nodes.put(typeId, boundNode.copy(args = bound.args))
                )
                res[currentHandle.index] = current.copy(
                    lowerTypeBounds = mergeDAGs(current.lowerTypeBounds, modifiedDAG)
                )
            },
            onTypeParam = { param ->
                val handle = interners.typeParamIds.getKey(param)
                if (handle.parent == key && hitBoundCycle(res, current, param, key)) return
                val boundCurrent = res[handle.index]
                res[handle.index] = boundCurrent.copy(
                    upperVarBounds = boundCurrent.upperVarBounds.add(id)
                )
                res[currentHandle.index] = current.copy(
                    lowerVarBounds = current.lowerVarBounds.add(param),
                    canBeNullable = nullable
                )
            },
            onError = { return }
        )
    }

    private fun hitBoundCycle(
        bounds: List<TypeParamBounds>,
        current: TypeParamBounds,
        adding: TypeParamId,
        key: MemberId,
    ): Boolean = current.lowerVarBounds.any { id ->
        if (id == adding) return@any true
        val handle = interners.typeParamIds.getKey(id)
        handle.parent == key && hitBoundCycle(bounds, bounds[handle.index], adding, key)
    }

    context(QueryAccessor)
    override fun packageTreeIterator(startId: PackageId) = object : Iterator<PackageId> {
        val stack = Stack<Iterator<PackageId>>()
        var iter: Iterator<PackageId> = SingletonIterator(startId)

        override fun hasNext(): Boolean = iter.hasNext()

        private tailrec fun findNext(): Unit = if (iter.hasNext() || stack.isEmpty()) Unit else {
            iter = stack.pop()
            findNext()
        }

        private fun findNext(id: PackageId) {
            val newIter = inputs.packageTree.value.nodes[id]!!.children.values.iterator()
            if (newIter.hasNext())
                iter = newIter
            else findNext()
        }

        override fun next(): PackageId {
            val pack = iter.next()
            findNext(pack)
            return pack
        }
    }

    override val virtualMembers: Query<TypeId, VirtualMembers> = DependentQuery(versionData) { key ->
        val parents = supertypeDAG[key].dag

        val functions = mutableMapOf<String, List<MemberId>>()
        val getters = mutableMapOf<String, List<MemberId>>()
        val setters = mutableMapOf<String, List<MemberId>>()
        val overriders = persistentMapOf<MemberId, MemberId>()
        val overrides = mutableMapOf<MemberId, List<MemberId>>()
        val unimplemented = mutableSetOf<MemberId>()

        // TODO

        VirtualMembers(functions, getters, setters, overriders, overrides, unimplemented)
    }
}

context(QueryAccessor)
fun PackageId.memberHierarchyIterator(queries: MainQueries) = object : Iterator<MemberId> {
    private var iterStack = Stack<Iterator<MemberId>>()
    private var iter: Iterator<MemberId> = queries
        .packageItems[this@memberHierarchyIterator]
        .asSequence()
        .flatMap { (_, list) -> list }
        .mapNotNull { it.asMember }
        .iterator()

    override fun hasNext(): Boolean = iter.hasNext()

    private fun findNext(id: MemberId) {
        val next = queries
            .childItems[id]
            .asSequence()
            .flatMap { (_, list) -> list }
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
        val res = iter.next()
        findNext(res)
        return res
    }
}