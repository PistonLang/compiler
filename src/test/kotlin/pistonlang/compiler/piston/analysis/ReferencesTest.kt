package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.handles.*
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing
import pistonlang.compiler.piston.parser.PistonType
import kotlin.test.assertEquals

class ReferencesTest {
    val code = """
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
    """.trimIndent()

    val handle = FileHandle("test.pi")

    fun expected(handler: PistonLanguageHandler): Map<String, ItemList<PistonType>> = mapOf(
        "foo" to itemListOf(
            ItemReference(ItemType.Function, handler, NodeLocation(0..32, PistonType.functionDef), handle),
        ),
        "bar" to itemListOf(
            ItemReference(ItemType.Getter, handler, NodeLocation(32..46, PistonType.functionDef), handle),
            ItemReference(ItemType.Setter, handler, NodeLocation(46..83, PistonType.functionDef), handle),
        ),
        "a" to itemListOf(
            ItemReference(ItemType.Val, handler, NodeLocation(83..95, PistonType.propertyDef), handle),
        ),
        "b" to itemListOf(
            ItemReference(ItemType.Var, handler, NodeLocation(95..107, PistonType.propertyDef), handle),
        ),
        "A" to itemListOf(
            ItemReference(ItemType.Trait, handler, NodeLocation(107..152, PistonType.traitDef), handle),
        ),
        "B" to itemListOf(
            ItemReference(ItemType.MultiInstanceClass, handler, NodeLocation(152..231, PistonType.classDef), handle),
        ),
        "C" to itemListOf(
            ItemReference(ItemType.SingletonClass, handler, NodeLocation(231..281, PistonType.classDef), handle),
        ),
        )

    @Test
    fun testReferences() {
        val instance = CompilerInstance(QueryVersionData())
        val handler = PistonLanguageHandler(::PistonLexer, PistonParsing::parseFile, instance)
        instance.addHandler(handler)

        instance.addFile(handle, code)
        assertEquals(expected(handler), handler.fileItems[handle].value)
    }
}