package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import kotlin.test.assertEquals

class STLReferencesTest {
    private val expected = """
        Int8: NodeLocation(pos=0..197, type=classDef)
        Int16: NodeLocation(pos=199..432, type=classDef)
        Int32: NodeLocation(pos=434..669, type=classDef)
        Int64: NodeLocation(pos=671..878, type=classDef)
        Float32: NodeLocation(pos=880..1139, type=classDef)
        Float64: NodeLocation(pos=1141..1400, type=classDef)
        Char: NodeLocation(pos=0..50, type=classDef)
        String: NodeLocation(pos=0..87, type=classDef)
        Bool: NodeLocation(pos=0..45, type=classDef)
        Any: NodeLocation(pos=0..95, type=traitDef)
        Nothing: NodeLocation(pos=97..125, type=classDef)
        Unit: NodeLocation(pos=0..10, type=classDef)
        Pair: NodeLocation(pos=12..105, type=classDef)
        Triple: NodeLocation(pos=107..240, type=classDef)
        Array: NodeLocation(pos=0..84, type=classDef)
        Int8Array: NodeLocation(pos=129..220, type=classDef)
        Int16Array: NodeLocation(pos=222..315, type=classDef)
        Int32Array: NodeLocation(pos=317..410, type=classDef)
        Int64Array: NodeLocation(pos=412..505, type=classDef)
        Float32Array: NodeLocation(pos=507..604, type=classDef)
        Float64Array: NodeLocation(pos=606..703, type=classDef)
        CharArray: NodeLocation(pos=798..889, type=classDef)
        BoolArray: NodeLocation(pos=705..796, type=classDef)
        arrayOfNulls: NodeLocation(pos=86..127, type=functionDef)
        println: NodeLocation(pos=0..24, type=functionDef)
    """.trimIndent()

    @Test
    fun testSTLReferences() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)

        instance.add(stlTree)
        val res = instance.access {
            stlItems.asSequence().fold(StringBuilder()) { builder, (_, ref) ->
                builder.appendLine("${ref.name}: ${handler.parentRelativeLocation(ref)}")
            }.dropLast(1).toString()
        }
        assertEquals(expected, res)
    }
}