package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pistonlang.compiler.common.files.FilePath
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.main.stl.stlTree
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.piston.parser.PistonType
import kotlin.test.assertEquals

class HandlesTest {
    private val reference = FilePath("test.pi")

    private val expectations: List<Pair<String, MemberList<PistonType>>> = listOf(
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
        """.trimIndent() to MutableMemberList<PistonType>().apply {
            add(MemberType.Function, "foo", NodeLocation(0..32, PistonType.functionDef))
            add(MemberType.Getter, "bar", NodeLocation(34..46, PistonType.functionDef))
            add(MemberType.Setter, "bar", NodeLocation(48..83, PistonType.functionDef))
            add(MemberType.Val, "a", NodeLocation(85..95, PistonType.propertyDef))
            add(MemberType.Var, "b", NodeLocation(97..107, PistonType.propertyDef))
            add(MemberType.Trait, "A", NodeLocation(109..152, PistonType.traitDef))
            add(MemberType.MultiInstanceClass, "B", NodeLocation(154..231, PistonType.classDef))
            add(MemberType.SingletonClass, "C", NodeLocation(233..281, PistonType.classDef))
        }.toImmutable(),
        """
        import {
            foo.a               // function
            bar: { a, b, c }    // val, var, package
        }
        
        def useAll() = a(a + b) - c.d
        """.trimIndent() to MutableMemberList<PistonType>().apply {
            add(MemberType.Function, "useAll", NodeLocation(93..122, PistonType.functionDef))
        }.toImmutable()
    )

    @Test
    fun testHandles() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)
        instance.add(stlTree)

        instance.access {
            assertAll(expectations.map { (code, expected) ->
                {
                    instance.addFile(rootPackage, reference, code)
                    val id = instance.interners.fileIds[reference]!!
                    assertEquals(expected, handler.fileItems[id])
                }
            })
        }
    }
}