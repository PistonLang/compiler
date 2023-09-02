package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.main.memberHierarchyIterator
import pistonlang.compiler.common.main.stl.stlTree
import kotlin.test.Test
import kotlin.test.assertEquals

class SupertypeDAGTest {
    private val tree = virtualTree {
        data("foo.pi") {
            """
                trait A[T]
                
                trait B <: A[Int32]
                
                trait C[T, S] <: A[T]
                
                trait D <: B & C[Int32, Bool]
                          
                trait E <: C[Int32, Int32]                
            """.trimIndent()
        }
        data("cycle.pi") {
            """
                trait Foo <: Bar
                
                trait Bar <: Foo
            """.trimIndent()
        }
    }

    private val expected = """
        TypeDAG([Trait(FilePath(path=cycle.pi), Foo, 0)]) {
            Trait(FilePath(path=cycle.pi), Foo, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
            Trait(FilePath(path=piston.special.pi), Any, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        }
        TypeDAG([Trait(FilePath(path=cycle.pi), Bar, 0)]) {
            Trait(FilePath(path=cycle.pi), Bar, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
            Trait(FilePath(path=piston.special.pi), Any, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        }
        TypeDAG([Trait(FilePath(path=foo.pi), A, 0)]) {
            Trait(FilePath(path=foo.pi), A, 0): Node([TypeParam(Trait(FilePath(path=foo.pi), A, 0), 0)], [Trait(FilePath(path=piston.special.pi), Any, 0)])
            Trait(FilePath(path=piston.special.pi), Any, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        }
        TypeDAG([Trait(FilePath(path=foo.pi), B, 0)]) {
            Trait(FilePath(path=foo.pi), A, 0): Node([MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)], [Trait(FilePath(path=piston.special.pi), Any, 0)])
            Trait(FilePath(path=foo.pi), B, 0): Node([], [Trait(FilePath(path=foo.pi), A, 0)])
            Trait(FilePath(path=piston.special.pi), Any, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        }
        TypeDAG([Trait(FilePath(path=foo.pi), C, 0)]) {
            Trait(FilePath(path=foo.pi), A, 0): Node([TypeParam(Trait(FilePath(path=foo.pi), C, 0), 0)], [Trait(FilePath(path=piston.special.pi), Any, 0)])
            Trait(FilePath(path=foo.pi), C, 0): Node([TypeParam(Trait(FilePath(path=foo.pi), C, 0), 0), TypeParam(Trait(FilePath(path=foo.pi), C, 0), 1)], [Trait(FilePath(path=foo.pi), A, 0)])
            Trait(FilePath(path=piston.special.pi), Any, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        }
        TypeDAG([Trait(FilePath(path=foo.pi), D, 0)]) {
            Trait(FilePath(path=foo.pi), A, 0): Node([MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)], [Trait(FilePath(path=piston.special.pi), Any, 0)])
            Trait(FilePath(path=foo.pi), B, 0): Node([], [Trait(FilePath(path=foo.pi), A, 0)])
            Trait(FilePath(path=foo.pi), C, 0): Node([MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0), MultiInstanceClass(FilePath(path=piston.bools.pi), Bool, 0)], [Trait(FilePath(path=foo.pi), A, 0)])
            Trait(FilePath(path=foo.pi), D, 0): Node([], [Trait(FilePath(path=foo.pi), B, 0), Trait(FilePath(path=foo.pi), C, 0)])
            Trait(FilePath(path=piston.special.pi), Any, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        }
        TypeDAG([Trait(FilePath(path=foo.pi), E, 0)]) {
            Trait(FilePath(path=foo.pi), A, 0): Node([MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)], [Trait(FilePath(path=piston.special.pi), Any, 0)])
            Trait(FilePath(path=foo.pi), C, 0): Node([MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0), MultiInstanceClass(FilePath(path=piston.numbers.pi), Int32, 0)], [Trait(FilePath(path=foo.pi), A, 0)])
            Trait(FilePath(path=foo.pi), E, 0): Node([], [Trait(FilePath(path=foo.pi), C, 0)])
            Trait(FilePath(path=piston.special.pi), Any, 0): Node([], [Trait(FilePath(path=piston.special.pi), Any, 0)])
        }
    """.trimIndent()

    @Test
    fun testSupertypeDAG() {
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
                .mapNotNull { interners.typeIds[it] }
                .map { queries.supertypeDAG[it].dag }
                .joinToString(separator = "\n") { it.qualify(interners) }
        }

        assertEquals(expected, value)
    }
}