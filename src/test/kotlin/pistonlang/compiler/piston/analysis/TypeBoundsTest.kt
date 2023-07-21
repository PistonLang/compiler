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
        MultiInstanceClass(FilePath(path=classes.pi), Foo, 0): Dependent([NodeLocation(pos=6..7, type=identifier): [TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 0)], NodeLocation(pos=11..21, type=identifier): [Trait(FilePath(path=classes.pi), Comparable, 0)], NodeLocation(pos=22..23, type=identifier): [TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 0)], NodeLocation(pos=26..27, type=identifier): [TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 0)], NodeLocation(pos=31..32, type=identifier): [TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 1)], NodeLocation(pos=34..35, type=identifier): [TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 1)], NodeLocation(pos=39..42, type=identifier): [Trait(FilePath(path=classes.pi), Bar, 0)]], [TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 0) <: Trait(FilePath(path=classes.pi), Comparable, 0)[TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 0)], TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 0) <: TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 1), TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 1) <: Trait(FilePath(path=classes.pi), Bar, 0)])
        Trait(FilePath(path=classes.pi), Comparable, 0): Dependent([], [])
        Function(Trait(FilePath(path=classes.pi), Comparable, 0), compare, 0): Dependent([], [])
        Trait(FilePath(path=classes.pi), Bar, 0): Dependent([], [])
        Function(FilePath(path=classes.pi), foo, 0): Dependent([NodeLocation(pos=6..7, type=identifier): [TypeParam(Function(FilePath(path=classes.pi), foo, 0), 0)], NodeLocation(pos=11..21, type=identifier): [Trait(FilePath(path=classes.pi), Comparable, 0)], NodeLocation(pos=22..23, type=identifier): [TypeParam(Function(FilePath(path=classes.pi), foo, 0), 0)]], [TypeParam(Function(FilePath(path=classes.pi), foo, 0), 0) <: Trait(FilePath(path=classes.pi), Comparable, 0)[TypeParam(Function(FilePath(path=classes.pi), foo, 0), 0)]])
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