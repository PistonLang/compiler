package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.NewTypeHandle
import pistonlang.compiler.common.items.rootPackage
import kotlin.test.assertEquals

class SuperTypesTest {
    private val tree = virtualTree {
        data("foo.pi") {
            """
                trait Empty
                
                trait Foo[T] <: Empty
                
                class Test[A, B](a: A, b: B) <: Foo[A]
            """.trimIndent()
        }
        data("letters.pi") {
            """
                trait A[T]
                                
                trait B <: A[Int32]
                
                trait C[T, S] <: A[T]
                
                trait D <: B & C[Int32, Bool]                
            """.trimIndent()
        }
    }

    private val expected = """
        (TraitHandle(parent=FileHandle(path=foo.pi), name=Empty, id=0), Dependent(dependencies=[], data=NonEmptyList(nested=[TypeInstance(type=TraitHandle(parent=FileHandle(path=piston.special.pi), name=Any, id=0), args=[], nullable=false)])))
        (TraitHandle(parent=FileHandle(path=foo.pi), name=Foo, id=0), Dependent(dependencies=[HandleData(location=NodeLocation(pos=0..5, type=identifier), handles=NonEmptyList(nested=[TraitHandle(parent=FileHandle(path=foo.pi), name=Empty, id=0)]))], data=NonEmptyList(nested=[TypeInstance(type=TraitHandle(parent=FileHandle(path=foo.pi), name=Empty, id=0), args=[], nullable=false)])))
        (MultiInstanceClassHandle(parent=FileHandle(path=foo.pi), name=Test, id=0), Dependent(dependencies=[HandleData(location=NodeLocation(pos=0..3, type=identifier), handles=NonEmptyList(nested=[TraitHandle(parent=FileHandle(path=foo.pi), name=Foo, id=0)])), HandleData(location=NodeLocation(pos=4..5, type=identifier), handles=NonEmptyList(nested=[TypeParamHandle(parent=MultiInstanceClassHandle(parent=FileHandle(path=foo.pi), name=Test, id=0), id=0)]))], data=NonEmptyList(nested=[TypeInstance(type=TraitHandle(parent=FileHandle(path=foo.pi), name=Foo, id=0), args=[TypeInstance(type=TypeParamHandle(parent=MultiInstanceClassHandle(parent=FileHandle(path=foo.pi), name=Test, id=0), id=0), args=[], nullable=false)], nullable=false)])))
        (TraitHandle(parent=FileHandle(path=letters.pi), name=A, id=0), Dependent(dependencies=[], data=NonEmptyList(nested=[TypeInstance(type=TraitHandle(parent=FileHandle(path=piston.special.pi), name=Any, id=0), args=[], nullable=false)])))
        (TraitHandle(parent=FileHandle(path=letters.pi), name=B, id=0), Dependent(dependencies=[HandleData(location=NodeLocation(pos=0..1, type=identifier), handles=NonEmptyList(nested=[TraitHandle(parent=FileHandle(path=letters.pi), name=A, id=0)])), HandleData(location=NodeLocation(pos=2..7, type=identifier), handles=NonEmptyList(nested=[MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0)]))], data=NonEmptyList(nested=[TypeInstance(type=TraitHandle(parent=FileHandle(path=letters.pi), name=A, id=0), args=[TypeInstance(type=MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0), args=[], nullable=false)], nullable=false)])))
        (TraitHandle(parent=FileHandle(path=letters.pi), name=C, id=0), Dependent(dependencies=[HandleData(location=NodeLocation(pos=0..1, type=identifier), handles=NonEmptyList(nested=[TraitHandle(parent=FileHandle(path=letters.pi), name=A, id=0)])), HandleData(location=NodeLocation(pos=2..3, type=identifier), handles=NonEmptyList(nested=[TypeParamHandle(parent=TraitHandle(parent=FileHandle(path=letters.pi), name=C, id=0), id=0)]))], data=NonEmptyList(nested=[TypeInstance(type=TraitHandle(parent=FileHandle(path=letters.pi), name=A, id=0), args=[TypeInstance(type=TypeParamHandle(parent=TraitHandle(parent=FileHandle(path=letters.pi), name=C, id=0), id=0), args=[], nullable=false)], nullable=false)])))
        (TraitHandle(parent=FileHandle(path=letters.pi), name=D, id=0), Dependent(dependencies=[HandleData(location=NodeLocation(pos=0..1, type=identifier), handles=NonEmptyList(nested=[TraitHandle(parent=FileHandle(path=letters.pi), name=B, id=0)])), HandleData(location=NodeLocation(pos=4..5, type=identifier), handles=NonEmptyList(nested=[TraitHandle(parent=FileHandle(path=letters.pi), name=C, id=0)])), HandleData(location=NodeLocation(pos=6..11, type=identifier), handles=NonEmptyList(nested=[MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0)])), HandleData(location=NodeLocation(pos=13..17, type=identifier), handles=NonEmptyList(nested=[MultiInstanceClassHandle(parent=FileHandle(path=piston.bools.pi), name=Bool, id=0)]))], data=NonEmptyList(nested=[TypeInstance(type=TraitHandle(parent=FileHandle(path=letters.pi), name=B, id=0), args=[], nullable=false), TypeInstance(type=TraitHandle(parent=FileHandle(path=letters.pi), name=C, id=0), args=[TypeInstance(type=MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0), args=[], nullable=false), TypeInstance(type=MultiInstanceClassHandle(parent=FileHandle(path=piston.bools.pi), name=Bool, id=0), args=[], nullable=false)], nullable=false)])))
    """.trimIndent()

    @Test
    fun testChildItems() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)

        instance.add(tree)
        val value = instance.access { queries ->
            queries
                .packageItems[rootPackage]
                .asSequence()
                .flatMap { (_, values) ->
                    values
                        .filter { it.itemType.type }
                        .map { key -> key to handler.supertypes[key as NewTypeHandle] }
                }
                .joinToString(separator = "\n")
        }

        assertEquals(expected, value)
    }
}