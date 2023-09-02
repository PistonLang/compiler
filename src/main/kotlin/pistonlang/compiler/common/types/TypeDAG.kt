package pistonlang.compiler.common.types

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.MainInterners

data class TypeDAGNode(
    val args: List<TypeVar>,
    val parents: PersistentSet<TypeId>
)

data class TypeDAG(
    val lowest: PersistentSet<TypeId>,
    val nodes: PersistentMap<TypeId, TypeDAGNode>,
    val variables: PersistentMap<TypeVar, TypeInstance>
) : Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "TypeDAG(${lowest.qualify(interners)}) ${
            nodes.asSequence().joinToString(separator = "\n    ", prefix = "{\n    ", postfix = "\n}") {
                val args = it.value.args.map { arg -> variables.resolve(arg) }
                val node = "Node(${args.qualify(interners)}, ${it.value.parents.qualify(interners)})"
                "${it.key.qualify(interners)}: $node"
            }
        }"

    fun isEmpty(): Boolean = lowest.isEmpty()
}

val emptyTypeDAG: TypeDAG = TypeDAG(persistentHashSetOf(), persistentHashMapOf(), persistentHashMapOf())

fun Map<TypeVar, TypeInstance>.resolve(typeVar: TypeVar): TypeInstance =
    this[typeVar]?.let { resolve(it) } ?: typeVar.toInstance()

fun Map<TypeVar, TypeInstance>.resolve(instance: TypeInstance): TypeInstance = instance.asTypeVar()?.let { newVar ->
    val res = resolve(newVar)
    if (instance.nullable && !res.nullable) res.copy(nullable = true) else res
} ?: instance.copy(args = instance.args.map { resolve(it) })