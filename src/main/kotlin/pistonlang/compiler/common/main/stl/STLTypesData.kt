package pistonlang.compiler.common.main.stl

import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.handles.TypeHandle
import pistonlang.compiler.common.types.*

data class STLTypes(
    val int8: TypeHandle,
    val int16: TypeHandle,
    val int32: TypeHandle,
    val int64: TypeHandle,
    val float32: TypeHandle,
    val float64: TypeHandle,
    val char: TypeHandle,
    val string: TypeHandle,
    val bool: TypeHandle,
    val any: TypeHandle,
    val nothing: TypeHandle,
    val unit: TypeHandle
) {
    val nullableAnyInstance = TypeInstance(any, emptyList(), true)
    val anyInstance = TypeInstance(any, emptyList(), false)
    val nullableNothingInstance = TypeInstance(nothing, emptyList(), true)
    val nothingInstance = TypeInstance(nothing, emptyList(), false)
    val unitInstance = TypeInstance(unit, emptyList(), false)
    val anyDAG = any.asType?.let { any ->
        TypeDAG(
            persistentHashSetOf(any),
            persistentHashMapOf<TypeId, Set<TypeId>>().put(any, persistentHashSetOf(any)),
            persistentHashMapOf()
        )
    } ?: emptyTypeDAG
    val anyParamBounds = TypeParamBounds(true, anyDAG, persistentHashSetOf(), persistentHashSetOf())
}