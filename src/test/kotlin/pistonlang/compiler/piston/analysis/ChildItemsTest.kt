package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.ItemReference
import pistonlang.compiler.common.items.ItemType
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing
import kotlin.test.assertEquals

class ChildItemsTest {
    private val tree = virtualTree {
        data("func.pi") {
            """
                def func[A where A <: Int32](a: A, b: Int32): Int32 = a + b
            """.trimIndent() to "{A=ItemList(list=[[], [], [], [], [], [], [], [], [NodeLocation(pos=9..10, type=identifier)]])}"
        }

        data("class.pi") {
            """
                class Foo[T](t: T) <: Bar {
                    val t = t
                    
                    def foo() = println(t.toString())
                }
            """.trimIndent() to "{T=ItemList(list=[[], [], [], [], [], [], [], [], [NodeLocation(pos=10..11, type=identifier)]]), t=ItemList(list=[[], [], [], [NodeLocation(pos=32..41, type=propertyDef)], [], [], [], [], []]), foo=ItemList(list=[[], [], [], [], [], [NodeLocation(pos=51..84, type=functionDef)], [], [], []])}"
        }
        data("trait.pi") {
            """
                trait Bar {
                    def foo()
                    
                    def bar: Int32 = 10
                }
            """.trimIndent() to "{foo=ItemList(list=[[], [], [], [], [], [NodeLocation(pos=16..35, type=functionDef)], [], [], []]), bar=ItemList(list=[[], [], [], [], [], [], [NodeLocation(pos=35..54, type=functionDef)], [], []])}"
        }
    }

    @Test
    fun testChildItems() {
        val instance = CompilerInstance(QueryVersionData())
        val handler = PistonLanguageHandler(::PistonLexer, PistonParsing::parseFile, instance)
        instance.addHandler(handler)

        instance.add(tree.mapValues { it.first })
        assertAll(tree.map { (file, data) ->
            {
                val expected = data.second
                handler.fileItems[file].value.forEach { (name, list) ->
                    ItemType.values().forEach inner@ { type ->
                        if (!list.iteratorFor(type).hasNext()) return@inner
                        val ref = ItemReference(file, name, type, 0)
                        assertEquals(expected, handler.childItems[ref].value.toString())
                    }
                }
            }
        })
    }
}