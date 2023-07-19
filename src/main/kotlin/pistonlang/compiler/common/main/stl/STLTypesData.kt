package pistonlang.compiler.common.main.stl

import pistonlang.compiler.common.items.handles.TypeHandle
import pistonlang.compiler.common.types.TypeInstance

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
}