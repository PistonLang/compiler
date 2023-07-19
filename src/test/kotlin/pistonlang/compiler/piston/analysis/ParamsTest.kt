package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.memberHierarchyIterator
import pistonlang.compiler.common.main.stl.stlTree
import kotlin.test.assertEquals

class ParamsTest {
    private val tree = virtualTree {
        data("foo.pi") {
            """
                trait Foo[T] {
                    def foo(a: T, b: T): Int32
                }
                
                class Bar <: Foo[Int32] {
                    def foo(a: Int32, b: Int32): Int32 = a + b
                    
                    def a = 10
                    def a_=(new: Int32) = println(new)
                }
            """.trimIndent()
        }
    }

    private val expected = """
        SingletonClass(FilePath(path=foo.pi), Bar, 0): Dependent([], [])
        Function(SingletonClass(FilePath(path=foo.pi), Bar, 0), foo, 0): Dependent([NodeLocation(pos=4..9, type=identifier): [MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)], NodeLocation(pos=14..19, type=identifier): [MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)]], [MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0), MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)])
        Getter(SingletonClass(FilePath(path=foo.pi), Bar, 0), a, 0): Dependent([], [])
        Setter(SingletonClass(FilePath(path=foo.pi), Bar, 0), a, 0): Dependent([NodeLocation(pos=6..11, type=identifier): [MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)]], [MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)])
        Trait(FilePath(path=foo.pi), Foo, 0): Dependent([], [])
        Function(Trait(FilePath(path=foo.pi), Foo, 0), foo, 0): Dependent([NodeLocation(pos=4..5, type=identifier): [TypeParam(Trait(FilePath(path=foo.pi), Foo, 0), 0)], NodeLocation(pos=10..11, type=identifier): [TypeParam(Trait(FilePath(path=foo.pi), Foo, 0), 0)]], [TypeParam(Trait(FilePath(path=foo.pi), Foo, 0), 0), TypeParam(Trait(FilePath(path=foo.pi), Foo, 0), 0)])
    """.trimIndent()

    @Test
    fun testParams() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)
        val interners = instance.interners
        instance.add(stlTree)

        instance.add(tree)
        val value = instance.access { queries ->
            interners
                .packIds[rootPackage]!!
                .memberHierarchyIterator(queries)
                .asSequence()
                .map { it to handler.params[it] }
                .joinToString(separator = "\n") { it.qualify(interners) }
        }

        assertEquals(expected, value)
    }
}