package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.MemberType
import kotlin.test.assertEquals

class ChildItemsTest {
    private val tree = virtualTree {
        data("func.pi") {
            """
                def func[A where A <: Int32](a: A, b: Int32): Int32 = a + b
            """.trimIndent() to "MemberList(list=[{}, {}, {}, {}, {}, {}, {}, {}])"
        }

        data("class.pi") {
            """
                class Foo[T](t: T) <: Bar {
                    val t: T = t
                    
                    def foo() = println(t.toString())
                }
            """.trimIndent() to "MemberList(list=[{}, {}, {}, {t=[NodeLocation(pos=32..44, type=propertyDef)]}, {}, {foo=[NodeLocation(pos=54..87, type=functionDef)]}, {}, {}])"
        }
        data("trait.pi") {
            """
                trait Bar {
                    def foo()
                    
                    def bar: Int32 = 10
                }
            """.trimIndent() to "MemberList(list=[{}, {}, {}, {}, {}, {foo=[NodeLocation(pos=16..35, type=functionDef)]}, {bar=[NodeLocation(pos=35..54, type=functionDef)]}, {}])"
        }
    }

    @Test
    fun testChildItems() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)

        instance.add(tree.mapValues { it.first })
        instance.access {
            assertAll(tree.map { (file, data) ->
                {
                    val expected = data.second
                    MemberType.entries.forEach inner@{ type ->
                        handler.fileItems[file].iteratorFor(type).forEach { (name) ->
                            val ref = type.buildHandle(file, name, 0)
                            assertEquals(expected, handler.childItems[ref].toString())
                        }
                    }
                }
            })
        }
    }
}