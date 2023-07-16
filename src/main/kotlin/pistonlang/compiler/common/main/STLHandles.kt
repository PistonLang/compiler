package pistonlang.compiler.common.main

import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.types.TypeInstance

private val numbers = FileHandle("piston.numbers.pi")
private val chars = FileHandle("piston.chars.pi")
private val strings = FileHandle("piston.strings.pi")
private val bools = FileHandle("piston.bools.pi")
private val special = FileHandle("piston.special.pi")
private val tuples = FileHandle("piston.tuples.pi")
private val arrays = FileHandle("piston.arrays.pi")
private val console = FileHandle("piston.io.console.pi")

val int8Handle = MultiInstanceClassHandle(numbers, "Int8", 0)
val int16Handle = MultiInstanceClassHandle(numbers, "Int16", 0)
val int32Handle = MultiInstanceClassHandle(numbers, "Int32", 0)
val int64Handle = MultiInstanceClassHandle(numbers, "Int64", 0)
val float32Handle = MultiInstanceClassHandle(numbers, "Float32", 0)
val float64Handle = MultiInstanceClassHandle(numbers, "Float64", 0)
val charHandle = MultiInstanceClassHandle(chars, "Char", 0)
val stringHandle = MultiInstanceClassHandle(strings, "String", 0)
val boolHandle = MultiInstanceClassHandle(bools, "Bool", 0)
val anyHandle = TraitHandle(special, "Any", 0)
val nothingHandle = MultiInstanceClassHandle(special, "Nothing", 0)
val unitHandle = SingletonClassHandle(tuples, "Unit", 0)

val stlItems = run {
    listOf(
        int8Handle,
        int16Handle,
        int32Handle,
        int64Handle,
        float32Handle,
        float64Handle,
        charHandle,
        stringHandle,
        boolHandle,
        anyHandle,
        nothingHandle,
        unitHandle,
        MultiInstanceClassHandle(tuples, "Pair", 0),
        MultiInstanceClassHandle(tuples, "Triple", 0),
        MultiInstanceClassHandle(arrays, "Array", 0),
        MultiInstanceClassHandle(arrays, "Int8Array", 0),
        MultiInstanceClassHandle(arrays, "Int16Array", 0),
        MultiInstanceClassHandle(arrays, "Int32Array", 0),
        MultiInstanceClassHandle(arrays, "Int64Array", 0),
        MultiInstanceClassHandle(arrays, "Float32Array", 0),
        MultiInstanceClassHandle(arrays, "Float64Array", 0),
        MultiInstanceClassHandle(arrays, "CharArray", 0),
        MultiInstanceClassHandle(arrays, "BoolArray", 0),
        FunctionHandle(arrays, "arrayOfNulls", 0),
        FunctionHandle(console, "println", 0),
    ).asSequence().map { it.name to it }.toMap()
}

val nullableAnyInstance = TypeInstance(anyHandle, emptyList(), true)
val anyInstance = TypeInstance(anyHandle, emptyList(), false)
val nullableNothingInstance = TypeInstance(nothingHandle, emptyList(), true)
val nothingInstance = TypeInstance(nothingHandle, emptyList(), false)
val unitInstance = TypeInstance(unitHandle, emptyList(), false)