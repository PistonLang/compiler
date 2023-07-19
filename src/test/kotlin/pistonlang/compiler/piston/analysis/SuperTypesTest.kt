package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.stl.stlTree
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
        MultiInstanceClass(FilePath(path=foo.pi), Test, 0): Dependent([NodeLocation(pos=0..3, type=identifier): [Trait(FilePath(path=foo.pi), Foo, 0)], NodeLocation(pos=4..5, type=identifier): [TypeParam(MultiInstanceClass(FilePath(path=foo.pi), Test, 0), 0)]], [Trait(FilePath(path=foo.pi), Foo, 0)[TypeParam(MultiInstanceClass(FilePath(path=foo.pi), Test, 0), 0)]])
        Trait(FilePath(path=foo.pi), Empty, 0): Dependent([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        Trait(FilePath(path=foo.pi), Foo, 0): Dependent([NodeLocation(pos=0..5, type=identifier): [Trait(FilePath(path=foo.pi), Empty, 0)]], [Trait(FilePath(path=foo.pi), Empty, 0)])
        Trait(FilePath(path=letters.pi), A, 0): Dependent([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        Trait(FilePath(path=letters.pi), B, 0): Dependent([NodeLocation(pos=0..1, type=identifier): [Trait(FilePath(path=letters.pi), A, 0)], NodeLocation(pos=2..7, type=identifier): [MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)]], [Trait(FilePath(path=letters.pi), A, 0)[MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)]])
        Trait(FilePath(path=letters.pi), C, 0): Dependent([NodeLocation(pos=0..1, type=identifier): [Trait(FilePath(path=letters.pi), A, 0)], NodeLocation(pos=2..3, type=identifier): [TypeParam(Trait(FilePath(path=letters.pi), C, 0), 0)]], [Trait(FilePath(path=letters.pi), A, 0)[TypeParam(Trait(FilePath(path=letters.pi), C, 0), 0)]])
        Trait(FilePath(path=letters.pi), D, 0): Dependent([NodeLocation(pos=0..1, type=identifier): [Trait(FilePath(path=letters.pi), B, 0)], NodeLocation(pos=4..5, type=identifier): [Trait(FilePath(path=letters.pi), C, 0)], NodeLocation(pos=6..11, type=identifier): [MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)], NodeLocation(pos=13..17, type=identifier): [MultiInstanceClass(FilePath(path=piston.bools.pi), Bool, 0)]], [Trait(FilePath(path=letters.pi), B, 0), Trait(FilePath(path=letters.pi), C, 0)[MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0), MultiInstanceClass(FilePath(path=piston.bools.pi), Bool, 0)]])
 """.trimIndent()

    @Test
    fun testChildItems() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)
        val interners = instance.interners
        instance.add(stlTree)

        instance.add(tree)
        val value = instance.access { queries ->
            queries
                .packageItems[interners.packIds[rootPackage]!!]
                .asSequence()
                .flatMap { (_, values) ->
                    values.mapNotNull { key ->
                        val memberId = key.asMember ?: return@mapNotNull null
                        val typeId = interners.typeIds[memberId] ?: return@mapNotNull null
                        key to handler.supertypes[typeId]
                    }
                }
                .joinToString(separator = "\n") { it.qualify(interners) }
        }

        assertEquals(expected, value)
    }
}