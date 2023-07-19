package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.memberHierarchyIterator
import pistonlang.compiler.common.main.stl.stlTree
import kotlin.test.assertEquals

class TypeBoundsTest {
    private val tree = virtualTree {
        data("classes.pi") {
            """
                trait Comparable[T] {
                    def compare(other: T): Int32
                }
                
                trait Bar
                
                class Foo[T, S where T <: Comparable[T], T <: S, S <: Bar](foo: T)
                
                def foo[T where T <: Comparable[T]](foo: T, bar: T): Bool = foo < bar
            """.trimIndent()
        }
    }

    private val expected = """
        MultiInstanceClass(FilePath(path=classes.pi), Foo, 0): Dependent(dependencies=[HandleData(location=NodeLocation(pos=6..7, type=identifier), handles=NonEmptyList(nested=[TypeParamId(value=0)])), HandleData(location=NodeLocation(pos=11..21, type=identifier), handles=NonEmptyList(nested=[MemberId(value=1)])), HandleData(location=NodeLocation(pos=22..23, type=identifier), handles=NonEmptyList(nested=[TypeParamId(value=0)])), HandleData(location=NodeLocation(pos=26..27, type=identifier), handles=NonEmptyList(nested=[TypeParamId(value=0)])), HandleData(location=NodeLocation(pos=31..32, type=identifier), handles=NonEmptyList(nested=[TypeParamId(value=1)])), HandleData(location=NodeLocation(pos=34..35, type=identifier), handles=NonEmptyList(nested=[TypeParamId(value=1)])), HandleData(location=NodeLocation(pos=39..42, type=identifier), handles=NonEmptyList(nested=[MemberId(value=2)]))], data=[[TypeInstance(type=TypeId(value=1), args=[TypeInstance(type=TypeParamId(value=0), args=[], nullable=false)], nullable=false), TypeInstance(type=TypeParamId(value=1), args=[], nullable=false)], [TypeInstance(type=TypeId(value=2), args=[], nullable=false)]])
        Trait(FilePath(path=classes.pi), Comparable, 0): Dependent(dependencies=[], data=[[]])
        Function(Trait(FilePath(path=classes.pi), Comparable, 0), compare, 0): Dependent(dependencies=[], data=[])
        Trait(FilePath(path=classes.pi), Bar, 0): Dependent(dependencies=[], data=[])
        Function(FilePath(path=classes.pi), foo, 0): Dependent(dependencies=[HandleData(location=NodeLocation(pos=6..7, type=identifier), handles=NonEmptyList(nested=[TypeParamId(value=3)])), HandleData(location=NodeLocation(pos=11..21, type=identifier), handles=NonEmptyList(nested=[MemberId(value=1)])), HandleData(location=NodeLocation(pos=22..23, type=identifier), handles=NonEmptyList(nested=[TypeParamId(value=3)]))], data=[[TypeInstance(type=TypeId(value=1), args=[TypeInstance(type=TypeParamId(value=3), args=[], nullable=false)], nullable=false)]])
    """.trimIndent()

    @Test
    fun testTypeBounds() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)
        val interners = instance.interners
        instance.add(stlTree)

        instance.add(tree)
        val value = instance.access { queries ->
            interners
                .packIds[rootPackage]!!
                .memberHierarchyIterator(queries)
                .asSequence()
                .map { it to handler.typeParamBounds[it] }
                .joinToString(separator = "\n") { it.qualify(interners) }
        }

        assertEquals(expected, value)
    }
}