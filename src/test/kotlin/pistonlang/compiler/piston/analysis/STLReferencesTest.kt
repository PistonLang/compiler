package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.main.stlItems
import pistonlang.compiler.common.main.stlTree
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing
import kotlin.test.assertEquals

class STLReferencesTest {
    private val expected = """
        Int8: NodeLocation(pos=0..197, type=classDef)
        Int16: NodeLocation(pos=197..432, type=classDef)
        Int32: NodeLocation(pos=432..669, type=classDef)
        Int64: NodeLocation(pos=669..878, type=classDef)
        Float32: NodeLocation(pos=878..1139, type=classDef)
        Float64: NodeLocation(pos=1139..1400, type=classDef)
        Char: NodeLocation(pos=0..50, type=classDef)
        String: NodeLocation(pos=0..87, type=classDef)
        Bool: NodeLocation(pos=0..45, type=classDef)
        Any: NodeLocation(pos=0..95, type=traitDef)
        Nothing: NodeLocation(pos=95..125, type=classDef)
        Unit: NodeLocation(pos=0..10, type=classDef)
        Pair: NodeLocation(pos=10..105, type=classDef)
        Triple: NodeLocation(pos=105..240, type=classDef)
        Array: NodeLocation(pos=0..84, type=classDef)
        Int8Array: NodeLocation(pos=127..220, type=classDef)
        Int16Array: NodeLocation(pos=220..315, type=classDef)
        Int32Array: NodeLocation(pos=315..410, type=classDef)
        Int64Array: NodeLocation(pos=410..505, type=classDef)
        Float32Array: NodeLocation(pos=505..604, type=classDef)
        Float64Array: NodeLocation(pos=604..703, type=classDef)
        CharArray: NodeLocation(pos=796..889, type=classDef)
        BoolArray: NodeLocation(pos=703..796, type=classDef)
        arrayOfNulls: NodeLocation(pos=84..127, type=functionDef)
    """.trimIndent()

    @Test
    fun testSTLReferences() {
        val instance = CompilerInstance(QueryVersionData())
        val handler = PistonLanguageHandler(::PistonLexer, PistonParsing::parseFile, instance)
        instance.addHandler(handler)
        instance.add(stlTree)
        val res = stlItems.asSequence().fold(StringBuilder()) { builder, (_, ref) ->
            builder.appendLine("${ref.name}: ${handler.nodeFromItemRef(ref)?.location}")
        }.dropLast(1).toString()
        assertEquals(expected, res)
    }
}