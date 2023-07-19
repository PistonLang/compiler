package pistonlang.compiler.common.types

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.items.handles.TypeHandle
import pistonlang.compiler.common.items.handles.asType
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.MainInterners

data class TypeDAGNode(
    val args: List<TypeInstance>,
    val parents: PersistentSet<TypeHandle>
) : Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "Node(${args.qualify(interners)}, ${parents.qualify(interners)})"
}

val emptyTypeDAGNode = TypeDAGNode(emptyList(), persistentSetOf())

data class TypeDAG(
    val lowest: PersistentSet<TypeHandle>,
    val nodes: PersistentMap<TypeHandle, TypeDAGNode>
) : Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "TypeDAG(${lowest.qualify(interners)}) ${
            nodes.asSequence().joinToString(separator = "\n\t", prefix = "{\n\t", postfix = "\n}") {
                "${it.key.qualify(interners)}: ${it.value.qualify(interners)}"
            }
        }"
}

val emptyTypeDAG: TypeDAG = TypeDAG(persistentSetOf(), persistentMapOf())

tailrec fun Map<TypeHandle, TypeDAGNode>.resolveParam(
    param: TypeParamId,
    nullable: Boolean,
    interners: MainInterners,
): TypeInstance {
    val paramHandle = interners.typeParamIds.getKey(param)
    val parent = paramHandle.parent

    val id = interners.typeIds.get(parent) ?: return TypeInstance(param.asType(), emptyList(), nullable)

    val handle = id.asType()

    if (handle !in this)
        return TypeInstance(param.asType(), emptyList(), nullable)

    val instance = this[handle]!!.args[paramHandle.index]
    val type = instance.type
    return resolveParam(type.asTypeParam ?: return instance, nullable || instance.nullable, interners)
}