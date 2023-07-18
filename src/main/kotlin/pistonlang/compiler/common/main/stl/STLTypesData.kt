package pistonlang.compiler.common.main.stl

import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.handles.asType
import pistonlang.compiler.common.types.TypeInstance

data class STLTypes(
    val int8: TypeId,
    val int16: TypeId,
    val int32: TypeId,
    val int64: TypeId,
    val float32: TypeId,
    val float64: TypeId,
    val char: TypeId,
    val string: TypeId,
    val bool: TypeId,
    val any: TypeId,
    val nothing: TypeId,
    val unit: TypeId
) {
    val nullableAnyInstance = TypeInstance(any.asType(), emptyList(), true)
    val anyInstance = TypeInstance(any.asType(), emptyList(), false)
    val nullableNothingInstance = TypeInstance(nothing.asType(), emptyList(), true)
    val nothingInstance = TypeInstance(nothing.asType(), emptyList(), false)
    val unitInstance = TypeInstance(unit.asType(), emptyList(), false)
}