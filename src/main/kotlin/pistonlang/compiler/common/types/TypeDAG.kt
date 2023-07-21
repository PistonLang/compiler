package pistonlang.compiler.common.types

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.items.handles.asType
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.MainInterners

data class TypeDAGNode(
    val args: List<TypeInstance>,
    val parents: PersistentSet<TypeId>
) : Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "Node(${args.qualify(interners)}, ${parents.qualify(interners)})"
}

val emptyTypeDAGNode = TypeDAGNode(emptyList(), persistentSetOf())

data class TypeDAG(
    val lowest: PersistentSet<TypeId>,
    val nodes: PersistentMap<TypeId, TypeDAGNode>
) : Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "TypeDAG(${lowest.qualify(interners)}) ${
            nodes.asSequence().joinToString(separator = "\n\t", prefix = "{\n\t", postfix = "\n}") {
                "${it.key.qualify(interners)}: ${it.value.qualify(interners)}"
            }
        }"

    fun isEmpty(): Boolean = lowest.isEmpty()
}

val emptyTypeDAG: TypeDAG = TypeDAG(persistentSetOf(), persistentMapOf())

tailrec fun Map<TypeId, TypeDAGNode>.resolveParam(
    param: TypeParamId,
    nullable: Boolean,
    interners: MainInterners,
): TypeInstance {
    val paramHandle = interners.typeParamIds.getKey(param)
    val parent = paramHandle.parent

    val id = interners.typeIds[parent] ?: return TypeInstance(param.asType(), emptyList(), nullable)

    if (id !in this)
        return TypeInstance(param.asType(), emptyList(), nullable)

    val instance = this[id]!!.args[paramHandle.index]
    val type = instance.type
    return resolveParam(type.asTypeParam ?: return instance, nullable || instance.nullable, interners)
}