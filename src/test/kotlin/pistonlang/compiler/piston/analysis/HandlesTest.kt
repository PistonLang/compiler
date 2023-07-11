package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pistonlang.compiler.common.items.FileHandle
import pistonlang.compiler.common.items.MemberList
import pistonlang.compiler.common.items.MemberType
import pistonlang.compiler.common.items.memberListOf
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.piston.parser.PistonType
import kotlin.test.assertEquals

class HandlesTest {
    private val reference = FileHandle("test.pi")

    private val expectations: List<Pair<String, Map<String, MemberList<PistonType>>>> = listOf(
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
            "foo" to memberListOf(
                MemberType.Function to NodeLocation(0..32, PistonType.functionDef)
            ),
            "bar" to memberListOf(
                MemberType.Getter to NodeLocation(34..46, PistonType.functionDef),
                MemberType.Setter to NodeLocation(48..83, PistonType.functionDef),
            ),
            "a" to memberListOf(
                MemberType.Val to NodeLocation(85..95, PistonType.propertyDef),
            ),
            "b" to memberListOf(
                MemberType.Var to NodeLocation(97..107, PistonType.propertyDef)
            ),
            "A" to memberListOf(
                MemberType.Trait to NodeLocation(109..152, PistonType.traitDef)
            ),
            "B" to memberListOf(
                MemberType.MultiInstanceClass to NodeLocation(154..231, PistonType.classDef)
            ),
            "C" to memberListOf(
                MemberType.SingletonClass to NodeLocation(233..281, PistonType.classDef)
            ),
        ),
        """
        import {
            foo.a               // function
            bar: { a, b, c }    // val, var, package
        }
        
        def useAll() = a(a + b) - c.d
        """.trimIndent() to mapOf(
            "useAll" to memberListOf(
                MemberType.Function to NodeLocation(93..122, PistonType.functionDef)
            )
        )
    )

    @Test
    fun testHandles() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)

        instance.access {
            assertAll(expectations.map { (code, expected) ->
                {
                    instance.addFile(reference, code)
                    assertEquals(expected, handler.fileItems[reference])
                }
            })
        }
    }
}