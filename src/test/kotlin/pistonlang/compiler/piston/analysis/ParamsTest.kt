package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.hierarchyMemberIterator
import pistonlang.compiler.common.main.stl.stlTree
import kotlin.test.assertEquals

class ParamsTest {
    private val tree = virtualTree {
        data("foo.pi") {
            """
                trait Foo[T] {
                    def foo(a: T, b: T): Int32
                }
                
                class Bar <: Foo[Int32] {
                    def foo(a: Int32, b: Int32): Int32 = a + b
                    
                    def a = 10
                    def a_=(new: Int32) = println(new)
                }
            """.trimIndent()
        }
    }

    private val expected = """
        (FunctionHandle(parent=SingletonClassHandle(parent=FileHandle(path=foo.pi), name=Bar, id=0), name=foo, id=0), Dependent(dependencies=[HandleData(location=NodeLocation(pos=4..9, type=identifier), handles=NonEmptyList(nested=[MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0)])), HandleData(location=NodeLocation(pos=14..19, type=identifier), handles=NonEmptyList(nested=[MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0)]))], data=[TypeInstance(type=MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0), args=[], nullable=false), TypeInstance(type=MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0), args=[], nullable=false)]))
        (SetterHandle(parent=SingletonClassHandle(parent=FileHandle(path=foo.pi), name=Bar, id=0), name=a, id=0), Dependent(dependencies=[HandleData(location=NodeLocation(pos=6..11, type=identifier), handles=NonEmptyList(nested=[MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0)]))], data=[TypeInstance(type=MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0), args=[], nullable=false)]))
        (FunctionHandle(parent=TraitHandle(parent=FileHandle(path=foo.pi), name=Foo, id=0), name=foo, id=0), Dependent(dependencies=[HandleData(location=NodeLocation(pos=4..5, type=identifier), handles=NonEmptyList(nested=[TypeParamHandle(parent=TraitHandle(parent=FileHandle(path=foo.pi), name=Foo, id=0), id=0)])), HandleData(location=NodeLocation(pos=10..11, type=identifier), handles=NonEmptyList(nested=[TypeParamHandle(parent=TraitHandle(parent=FileHandle(path=foo.pi), name=Foo, id=0), id=0)]))], data=[TypeInstance(type=TypeParamHandle(parent=TraitHandle(parent=FileHandle(path=foo.pi), name=Foo, id=0), id=0), args=[], nullable=false), TypeInstance(type=TypeParamHandle(parent=TraitHandle(parent=FileHandle(path=foo.pi), name=Foo, id=0), id=0), args=[], nullable=false)]))
    """.trimIndent()

    @Test
    fun testParams() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)
        val interners = instance.interners
        instance.add(stlTree)

        instance.add(tree)
        val value = instance.access { queries ->
            interners
                .packIds[rootPackage]
                .hierarchyMemberIterator(interners, queries)
                .asSequence()
                .map { it to handler.params[it] }
                .joinToString(separator = "\n") { it.qualify(interners) }
        }

        assertEquals(expected, value)
    }
}