package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pistonlang.compiler.common.items.FileReference
import pistonlang.compiler.common.items.ItemList
import pistonlang.compiler.common.items.ItemType
import pistonlang.compiler.common.items.itemListOf
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing
import pistonlang.compiler.piston.parser.PistonType
import kotlin.test.assertEquals

class ReferencesTest {
    private val reference = FileReference("test.pi")

    private fun codeExpectationMap(): List<Pair<String, Map<String, ItemList<PistonType>>>> = listOf(
        """
        def foo[T](list: List[T]) = Unit
        
        def bar = 10
        
        def bar_=(num: Int) = num.println()
        
        val a = 10
        
        var b = 15
        
        trait A {
            def print() = "A".println()
        }
        
        class B(num: Int) <: A {
            val num = num
        
            def print() = num.println()
        }
        
        class C <: A {
            def print() = 'C'.println()
        }
        """.trimIndent() to mapOf(
            "foo" to itemListOf(
                ItemType.Function to NodeLocation(0..32, PistonType.functionDef)
            ),
            "bar" to itemListOf(
                ItemType.Getter to NodeLocation(34..46, PistonType.functionDef),
                ItemType.Setter to NodeLocation(48..83, PistonType.functionDef),
            ),
            "a" to itemListOf(
                ItemType.Val to NodeLocation(85..95, PistonType.propertyDef),
            ),
            "b" to itemListOf(
                ItemType.Var to NodeLocation(97..107, PistonType.propertyDef)
            ),
            "A" to itemListOf(
                ItemType.Trait to NodeLocation(109..152, PistonType.traitDef)
            ),
            "B" to itemListOf(
                ItemType.MultiInstanceClass to NodeLocation(154..231, PistonType.classDef)
            ),
            "C" to itemListOf(
                ItemType.SingletonClass to NodeLocation(233..281, PistonType.classDef)
            ),
        ),
        """
        import {
            foo.a               // function
            bar: { a, b, c }    // val, var, package
        }
        
        def useAll() = a(a + b) - c.d
        """.trimIndent() to mapOf(
            "useAll" to itemListOf(
                ItemType.Function to NodeLocation(93..122, PistonType.functionDef)
            )
        )
    )

    @Test
    fun testReferences() {
        val instance = CompilerInstance(QueryVersionData())
        val handler = PistonLanguageHandler(::PistonLexer, PistonParsing::parseFile, instance)
        instance.addHandler(handler)

        assertAll(codeExpectationMap().map { (code, expected) ->
            {
                instance.addFile(reference, code)
                assertEquals(expected, handler.fileItems[reference].value)
            }
        })
    }
}