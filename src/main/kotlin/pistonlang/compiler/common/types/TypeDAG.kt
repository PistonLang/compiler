package pistonlang.compiler.common.types

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.items.handles.TypeParamHandle
import pistonlang.compiler.common.items.handles.asTypeVar
import pistonlang.compiler.common.main.MainInterners

data class TypeDAG(
    val lowest: PersistentSet<TypeId>,
    val parents: PersistentMap<TypeId, Set<TypeId>>,
    val variables: PersistentMap<TypeVarId, TypeInstance>
) : Qualifiable {
    override fun qualify(interners: MainInterners): String = "TypeDAG(${lowest.qualify(interners)}) ${
        parents.asSequence().map { (curr, parents) ->
            val memberId = interners.typeIds.getKey(curr)
            val args = qualifyTypeArgs(interners, memberId)
            val node = "Node($args, ${parents.qualify(interners)})"
            "${memberId.qualify(interners)}: $node"
        }.joinToString(separator = "\n    ", prefix = "{\n    ", postfix = "\n}")
    }"

    private fun qualifyTypeArgs(interners: MainInterners, memberId: MemberId) =
        (0..Int.MAX_VALUE).asSequence()
            .map { interners.typeParamIds[TypeParamHandle(memberId, it)] }
            .takeWhile { it != null }
            .map { interners.typeVars[it!!.asTypeVar()]!! }
            .map { variables.resolve(it) }
            .qualify(interners)

    fun isEmpty(): Boolean = lowest.isEmpty()
}

val emptyTypeDAG: TypeDAG = TypeDAG(persistentHashSetOf(), persistentHashMapOf(), persistentHashMapOf())

typealias TypeVarMap = Map<TypeVarId, TypeInstance>

fun TypeVarMap.resolve(typeVar: TypeVarId): TypeInstance =
    this[typeVar]?.let { resolve(it) } ?: typeVar.toInstance()

fun TypeVarMap.resolve(instance: TypeInstance): TypeInstance = instance.asTypeVar()?.let { newVar ->
    val res = resolve(newVar)
    if (instance.nullable && !res.nullable) res.copy(nullable = true) else res
} ?: instance.copy(args = instance.args.map { resolve(it) })