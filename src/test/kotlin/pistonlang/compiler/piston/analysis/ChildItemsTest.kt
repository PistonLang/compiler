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
            """.trimIndent() to "{}"
        }

        data("class.pi") {
            """
                class Foo[T](t: T) <: Bar {
                    val t: T = t
                    
                    def foo() = println(t.toString())
                }
            """.trimIndent() to "{t=MemberList(list=[[], [], [], [NodeLocation(pos=32..44, type=propertyDef)], [], [], [], []]), foo=MemberList(list=[[], [], [], [], [], [NodeLocation(pos=54..87, type=functionDef)], [], []])}"
        }
        data("trait.pi") {
            """
                trait Bar {
                    def foo()
                    
                    def bar: Int32 = 10
                }
            """.trimIndent() to "{foo=MemberList(list=[[], [], [], [], [], [NodeLocation(pos=16..35, type=functionDef)], [], []]), bar=MemberList(list=[[], [], [], [], [], [], [NodeLocation(pos=35..54, type=functionDef)], []])}"
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
                    handler.fileItems[file].forEach { (name, list) ->
                        MemberType.entries.forEach inner@{ type ->
                            if (!list.iteratorFor(type).hasNext()) return@inner
                            val ref = type.buildHandle(file, name, 0)
                            assertEquals(expected, handler.childItems[ref].toString())
                        }
                    }
                }
            })
        }
    }
}