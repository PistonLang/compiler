package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.main.hierarchyMemberIterator
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
                .packIds[rootPackage]
                .hierarchyMemberIterator(interners, queries)
                .asSequence()
                .mapNotNull { interners.typeIds.getOrNull(it) }
                .map { handler.constructors[it] }
                .joinToString(separator = "\n")
        }

        assertEquals(expected, got)
    }
}