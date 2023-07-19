package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.memberHierarchyIterator
import pistonlang.compiler.common.main.stl.stlTree
import kotlin.test.assertEquals

class TypeParamsTest {
    private val tree = virtualTree {
        data("func.pi") {
            """
                def func[A where A <: Int32](a: A, b: Int32): Int32 = a + b
            """.trimIndent()
        }

        data("class.pi") {
            """
                class Foo[T](t: T) <: Bar {
                    val t: T = t
                    
                    def foo() = println(t.toString())
                }
            """.trimIndent()
        }
        data("trait.pi") {
            """
                trait Bar {
                    def foo()
                    
                    def bar: Int32 = 10
                }
            """.trimIndent()
        }
    }

    private val expected = """
        Function(FilePath(path=func.pi), func, 0): [(A, NodeLocation(pos=1..2, type=identifier))]
        MultiInstanceClass(FilePath(path=class.pi), Foo, 0): [(T, NodeLocation(pos=1..2, type=identifier))]
        Val(MultiInstanceClass(FilePath(path=class.pi), Foo, 0), t, 0): []
        Function(MultiInstanceClass(FilePath(path=class.pi), Foo, 0), foo, 0): []
        Trait(FilePath(path=trait.pi), Bar, 0): []
        Function(Trait(FilePath(path=trait.pi), Bar, 0), foo, 0): []
        Getter(Trait(FilePath(path=trait.pi), Bar, 0), bar, 0): []
    """.trimIndent()

    @Test
    fun testTypeParams() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)
        val interners = instance.interners
        instance.add(stlTree)

        instance.add(tree)
        val got = instance.access { queries ->
            interners
                .packIds[rootPackage]!!
                .memberHierarchyIterator(queries)
                .asSequence()
                .map { it to handler.typeParams[it] }
                .joinToString(separator = "\n") { it.qualify(interners) }
        }

        assertEquals(expected, got)
    }
}