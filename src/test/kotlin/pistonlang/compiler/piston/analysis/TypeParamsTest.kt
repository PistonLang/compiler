package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.MemberType
import kotlin.test.assertEquals

class TypeParamsTest {
    private val tree = virtualTree {
        data("func.pi") {
            """
                def func[A where A <: Int32](a: A, b: Int32): Int32 = a + b
            """.trimIndent() to "[(A, NodeLocation(pos=1..2, type=identifier))]"
        }

        data("class.pi") {
            """
                class Foo[T](t: T) <: Bar {
                    val t: T = t
                    
                    def foo() = println(t.toString())
                }
            """.trimIndent() to "[(T, NodeLocation(pos=1..2, type=identifier))]"
        }
        data("trait.pi") {
            """
                trait Bar {
                    def foo()
                    
                    def bar: Int32 = 10
                }
            """.trimIndent() to "[]"
        }
    }

    @Test
    fun testTypeParams() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)

        instance.add(tree.mapValues { it.first })
        instance.access {
            assertAll(tree.map { (file, data) ->
                {
                    val expected = data.second
                    MemberType.entries.forEach { type ->
                        handler.fileItems[file].iteratorFor(type).forEach { (name) ->
                            val ref = type.buildHandle(file, name, 0)
                            assertEquals(expected, handler.typeParams[ref].toString())
                        }
                    }
                }
            })
        }
    }
}