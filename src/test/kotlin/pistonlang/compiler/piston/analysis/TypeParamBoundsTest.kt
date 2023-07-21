package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.memberHierarchyIterator
import pistonlang.compiler.common.main.stl.stlTree
import kotlin.test.assertEquals

class TypeParamBoundsTest {
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
        MultiInstanceClass(FilePath(path=classes.pi), Foo, 0): [TypeParamBounds: false [TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 1)] [] TypeDAG([Trait(FilePath(path=classes.pi), Comparable, 0)]) {
        	Trait(FilePath(path=piston.special.pi), Any, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        	Trait(FilePath(path=classes.pi), Comparable, 0): Node([TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 0)], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        }, TypeParamBounds: true [] [TypeParam(MultiInstanceClass(FilePath(path=classes.pi), Foo, 0), 0)] TypeDAG([Trait(FilePath(path=classes.pi), Bar, 0)]) {
        	Trait(FilePath(path=piston.special.pi), Any, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        	Trait(FilePath(path=classes.pi), Bar, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        }]
        Trait(FilePath(path=classes.pi), Comparable, 0): [TypeParamBounds: true [] [] TypeDAG([Trait(FilePath(path=piston.special.pi), Any, 0)]) {
        	Trait(FilePath(path=piston.special.pi), Any, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        }]
        Function(Trait(FilePath(path=classes.pi), Comparable, 0), compare, 0): []
        Trait(FilePath(path=classes.pi), Bar, 0): []
        Function(FilePath(path=classes.pi), foo, 0): [TypeParamBounds: true [] [] TypeDAG([Trait(FilePath(path=classes.pi), Comparable, 0)]) {
        	Trait(FilePath(path=piston.special.pi), Any, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        	Trait(FilePath(path=classes.pi), Comparable, 0): Node([TypeParam(Function(FilePath(path=classes.pi), foo, 0), 0)], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        }]
    """.trimIndent()

    @Test
    fun testTypeBounds() {
        val instance = defaultInstance()
        instance.addHandler(defaultHandler)
        val interners = instance.interners
        instance.add(stlTree)

        instance.add(tree)
        val value = instance.access { queries ->
            interners
                .packIds[rootPackage]!!
                .memberHierarchyIterator(queries)
                .asSequence()
                .map { it to queries.typeParamBounds[it] }
                .joinToString(separator = "\n") { it.qualify(interners) }
        }

        assertEquals(expected, value)
    }
}