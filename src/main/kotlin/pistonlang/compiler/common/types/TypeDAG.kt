package pistonlang.compiler.common.types

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.items.handles.TypeHandle
import pistonlang.compiler.common.main.MainInterners

data class TypeDAGNode(
    val args: List<TypeInstance>,
    val parents: PersistentSet<TypeHandle>
)

data class TypeDAG(
    val lowest: PersistentSet<TypeHandle>,
    val nodes: PersistentMap<TypeHandle, TypeDAGNode>
)

val emptyTypeDAG: TypeDAG = TypeDAG(persistentSetOf(), persistentMapOf())

tailrec fun Map<TypeHandle, TypeDAGNode>.resolveParam(
    param: TypeParamId,
    nullable: Boolean,
    interners: MainInterners,
): TypeInstance {
    val paramHandle = interners.typeParamIds[param]
    val parent = paramHandle.parent

    if (interners.memberIds[parent].type.newType)
        return TypeInstance(TypeHandle(param), emptyList(), nullable)

    val handle = TypeHandle(interners.typeIds[parent])

    if (handle !in this)
        return TypeInstance(TypeHandle(param), emptyList(), nullable)

    val instance = this[handle]!!.args[paramHandle.index]
    val type = instance.type
    return resolveParam(type.asTypeParam ?: return instance, nullable || instance.nullable, interners)
}