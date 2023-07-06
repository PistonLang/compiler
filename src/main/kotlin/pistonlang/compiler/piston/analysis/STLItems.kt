package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.files.VirtualPackageTree
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.*

val stlTree: VirtualPackageTree<String> = virtualTree {
    child("piston") {
        data("numbers.pi") {
            """
                class Int8(value: Int8) {
                    def plus(other: Int8): Int8
                    
                    def minus(other: Int8): Int8
                    
                    def mul(other: Int8): Int8
                    
                    def div(other: Int8): Int8
                    
                    def asInt16: Int16
                }
                
                class Int16(value: Int16) {
                    def plus(other: Int16): Int16
                    
                    def minus(other: Int16): Int16
                    
                    def mul(other: Int16): Int16
                    
                    def div(other: Int16): Int16
                    
                    def asInt8: Int8
                    
                    def asInt32: Int32
                }
                
                class Int32(value: Int32) {
                    def plus(other: Int32): Int32
                    
                    def minus(other: Int32): Int32
                    
                    def mul(other: Int32): Int32
                    
                    def div(other: Int32): Int32
                    
                    def asInt16: Int16
                    
                    def asInt64: Int64
                }
                
                class Int64(value: Int64) {
                    def plus(other: Int64): Int64
                    
                    def minus(other: Int64): Int64
                    
                    def mul(other: Int64): Int64
                    
                    def div(other: Int64): Int64
                    
                    def asInt32: Int32
                }
                
                class Float32(value: Float32) {
                    def plus(other: Float32): Float32
                    
                    def minus(other: Float32): Float32
                    
                    def mul(other: Float32): Float32
                    
                    def div(other: Float32): Float32
                    
                    def asInt32: Int32
                    
                    def asFloat64: Float64
                }
                
                class Float64(value: Float64) {
                    def plus(other: Float64): Float64
                    
                    def minus(other: Float64): Float64
                    
                    def mul(other: Float64): Float64
                    
                    def div(other: Float64): Float64
                    
                    def asInt64: Int64
                    
                    def asFloat32: Float32
                }
            """.trimIndent()
        }
        data("chars.pi") {
            """
                class Char(value: Char) {
                    def asInt32: Int32
                }
            """.trimIndent()
        }
        data("strings.pi") {
            """
                class String(value: String) {
                    def length: Int32
                    def apply(index: Int32): Char
                }
            """.trimIndent()
        }
        data("bools.pi") {
            """
                class Bool(value: Bool) {
                    def not: Bool
                }
            """.trimIndent()
        }
        data("special.pi") {
            """
                trait Any {
                    def toString(): String
                    def hashCode: Int
                    def equals(other: Any): Bool
                }
                
                class Nothing(self: Nothing)
            """.trimIndent()
        }
        data("tuples.pi") {
            """
                class Unit
                
                class Pair[A, B](first: A, second: B) {
                    val first: A = first
                    val second: B = second
                }
                
                class Triple[A, B, C](first: A, second: B, third: C) {
                    val first: A = first
                    val second: B = second
                    val third: C = third
                }
            """.trimIndent()
        }
        data("arrays.pi") {
            """
                class Array[T](value: Array[T]) {
                    def size: Int32
                    def apply(index: Int): T
                }
                
                def arrayOfNulls[T](size: Int): Array[T?]
                
                class Int8Array(size: Int32) {
                    def size: Int32 = size
                    def apply(index: Int): Int8
                }
                
                class Int16Array(size: Int32) {
                    def size: Int32 = size
                    def apply(index: Int): Int16
                }
                
                class Int32Array(size: Int32) {
                    def size: Int32 = size
                    def apply(index: Int): Int32
                }
                
                class Int64Array(size: Int32) {
                    def size: Int32 = size
                    def apply(index: Int): Int64
                }
                
                class Float32Array(size: Int32) {
                    def size: Int32 = size
                    def apply(index: Int): Float32
                }
                
                class Float64Array(size: Int32) {
                    def size: Int32 = size
                    def apply(index: Int): Float64
                }
                
                class BoolArray(size: Int32) {
                    def size: Int32 = size
                    def apply(index: Int): Bool
                }
                
                class CharArray(size: Int32) {
                    def size: Int32 = size
                    def apply(index: Int): Char
                }
            """.trimIndent()
        }
        child("io") {
            data("console.pi") {
                """
                    def println(str: String)
                """.trimIndent()
            }
        }
    }
}

private val numbers = FileHandle("piston/numbers.pi")
private val chars = FileHandle("piston/chars.pi")
private val strings = FileHandle("piston/strings.pi")
private val bools = FileHandle("piston/bools.pi")
private val special = FileHandle("piston/special.pi")
private val tuples = FileHandle("piston/tuples.pi")
private val arrays = FileHandle("piston/arrays.pi")
private val console = FileHandle("piston/io/console.pi")

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
        FunctionHandle(arrays, "arrayOfNulls",  0),
        FunctionHandle(console, "println",  0),
    ).asSequence().map { it.name to it }.toMap()
}

val nullableAnyInstance = TypeInstance(anyHandle, emptyList(), true)
val anyInstance = TypeInstance(anyHandle, emptyList(), false)
val nullableNothingInstance = TypeInstance(nothingHandle, emptyList(), true)
val nothingInstance = TypeInstance(nothingHandle, emptyList(), false)
val unitInstance = TypeInstance(unitHandle, emptyList(), false)