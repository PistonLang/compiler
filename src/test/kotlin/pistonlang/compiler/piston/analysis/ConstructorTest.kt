package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.MemberType
import pistonlang.compiler.common.items.MultiInstanceClassHandle
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.piston.parser.PistonType
import kotlin.test.assertEquals

class ConstructorTest {
    private val tree = virtualTree<Pair<String, List<NodeLocation<PistonType>>>> {
        data("func.pi") {
            """
                def func[A where A <: Int32](a: A, b: Int32): Int32 = a + b
            """.trimIndent() to emptyList()
        }

        data("class.pi") {
            """
                class Foo[T](t: T) <: Bar {
                    val t: T = t
                    
                    def foo() = println(t.toString())
                }
            """.trimIndent() to listOf(NodeLocation(pos = 12..18, PistonType.functionParams))
        }
        data("trait.pi") {
            """
                trait Bar {
                    def foo()
                    
                    def bar: Int32 = 10
                }
            """.trimIndent() to emptyList()
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
                    handler.fileItems[file].iteratorFor(MemberType.MultiInstanceClass).forEach { (name) ->
                        val ref = MultiInstanceClassHandle(file, name, 0)
                        assertEquals(expected, handler.constructors[ref])
                    }
                }
            })
        }
    }
}