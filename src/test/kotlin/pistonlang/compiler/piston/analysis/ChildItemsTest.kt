package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.hierarchyMemberIterator
import pistonlang.compiler.common.main.stl.stlTree
import kotlin.test.assertEquals

class ChildItemsTest {
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
        MemberList(list=[{}, {}, {}, {}, {}, {}, {}, {}])
        MemberList(list=[{}, {}, {}, {t=[NodeLocation(pos=32..44, type=propertyDef)]}, {}, {foo=[NodeLocation(pos=54..87, type=functionDef)]}, {}, {}])
        MemberList(list=[{}, {}, {}, {}, {}, {foo=[NodeLocation(pos=16..35, type=functionDef)]}, {bar=[NodeLocation(pos=35..54, type=functionDef)]}, {}])
    """.trimIndent()

    @Test
    fun testChildItems() {
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
                .map { it to handler.childItems[it] }
                .joinToString(separator = "\n") { it.qualify(interners) }
        }

        assertEquals(got, expected)
    }
}