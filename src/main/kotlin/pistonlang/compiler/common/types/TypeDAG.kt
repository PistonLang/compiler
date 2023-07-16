package pistonlang.compiler.common.types

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import pistonlang.compiler.common.items.NewTypeHandle
import pistonlang.compiler.common.items.TypeHandle
import pistonlang.compiler.common.items.TypeParamHandle

data class TypeDAGNode<out HandleType>(
    val args: List<TypeInstance>,
    val parents: PersistentSet<HandleType>
)

data class TypeDAG<out HandleType : TypeHandle>(
    val lowest: PersistentSet<HandleType>,
    val nodes: PersistentMap<TypeHandle, TypeDAGNode<HandleType>>
)

typealias SupertypeDAG = TypeDAG<NewTypeHandle>

val emptyTypeDAG: TypeDAG<Nothing> = TypeDAG(persistentSetOf(), persistentMapOf())

tailrec fun <T : TypeHandle> Map<TypeHandle, TypeDAGNode<T>>.resolveParam(
    param: TypeParamHandle,
    nullable: Boolean
): TypeInstance {
    val parent = param.parent

    if (parent !is TypeHandle || parent !in this)
        return TypeInstance(param, emptyList(), nullable)

    val instance = this[parent]!!.args[param.id]
    val type = instance.type
    return if (type is TypeParamHandle) resolveParam(type, nullable || instance.nullable) else instance
}