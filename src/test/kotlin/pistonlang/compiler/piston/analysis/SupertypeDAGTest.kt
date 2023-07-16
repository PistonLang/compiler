package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.NewTypeHandle
import pistonlang.compiler.common.items.rootPackage
import pistonlang.compiler.common.main.hierarchyIterator
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
            """.trimIndent()
        }
    }

    private val expected = """
        TypeDAG(lowest=[TraitHandle(parent=FileHandle(path=foo.pi), name=A, id=0)], nodes={TraitHandle(parent=FileHandle(path=piston.special.pi), name=Any, id=0)=TypeDAGNode(args=[], parents=[]), TraitHandle(parent=FileHandle(path=foo.pi), name=A, id=0)=TypeDAGNode(args=[TypeInstance(type=TypeParamHandle(parent=TraitHandle(parent=FileHandle(path=foo.pi), name=A, id=0), id=0), args=[], nullable=false)], parents=[TraitHandle(parent=FileHandle(path=piston.special.pi), name=Any, id=0)])})
        TypeDAG(lowest=[TraitHandle(parent=FileHandle(path=foo.pi), name=B, id=0)], nodes={TraitHandle(parent=FileHandle(path=piston.special.pi), name=Any, id=0)=TypeDAGNode(args=[], parents=[]), TraitHandle(parent=FileHandle(path=foo.pi), name=A, id=0)=TypeDAGNode(args=[TypeInstance(type=MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0), args=[], nullable=false)], parents=[TraitHandle(parent=FileHandle(path=piston.special.pi), name=Any, id=0)]), TraitHandle(parent=FileHandle(path=foo.pi), name=B, id=0)=TypeDAGNode(args=[], parents=[TraitHandle(parent=FileHandle(path=foo.pi), name=A, id=0)])})
        TypeDAG(lowest=[TraitHandle(parent=FileHandle(path=foo.pi), name=C, id=0)], nodes={TraitHandle(parent=FileHandle(path=piston.special.pi), name=Any, id=0)=TypeDAGNode(args=[], parents=[]), TraitHandle(parent=FileHandle(path=foo.pi), name=A, id=0)=TypeDAGNode(args=[TypeInstance(type=TypeParamHandle(parent=TraitHandle(parent=FileHandle(path=foo.pi), name=C, id=0), id=0), args=[], nullable=false)], parents=[TraitHandle(parent=FileHandle(path=piston.special.pi), name=Any, id=0)]), TraitHandle(parent=FileHandle(path=foo.pi), name=C, id=0)=TypeDAGNode(args=[TypeInstance(type=TypeParamHandle(parent=TraitHandle(parent=FileHandle(path=foo.pi), name=C, id=0), id=0), args=[], nullable=false), TypeInstance(type=TypeParamHandle(parent=TraitHandle(parent=FileHandle(path=foo.pi), name=C, id=0), id=0), args=[], nullable=false)], parents=[TraitHandle(parent=FileHandle(path=foo.pi), name=A, id=0)])})
        TypeDAG(lowest=[TraitHandle(parent=FileHandle(path=foo.pi), name=D, id=0)], nodes={TraitHandle(parent=FileHandle(path=piston.special.pi), name=Any, id=0)=TypeDAGNode(args=[], parents=[]), TraitHandle(parent=FileHandle(path=foo.pi), name=A, id=0)=TypeDAGNode(args=[TypeInstance(type=MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0), args=[], nullable=false)], parents=[TraitHandle(parent=FileHandle(path=piston.special.pi), name=Any, id=0)]), TraitHandle(parent=FileHandle(path=foo.pi), name=B, id=0)=TypeDAGNode(args=[], parents=[TraitHandle(parent=FileHandle(path=foo.pi), name=A, id=0)]), TraitHandle(parent=FileHandle(path=foo.pi), name=C, id=0)=TypeDAGNode(args=[TypeInstance(type=MultiInstanceClassHandle(parent=FileHandle(path=piston.numbers.pi), name=Int32, id=0), args=[], nullable=false), TypeInstance(type=MultiInstanceClassHandle(parent=FileHandle(path=piston.bools.pi), name=Bool, id=0), args=[], nullable=false)], parents=[TraitHandle(parent=FileHandle(path=foo.pi), name=A, id=0)]), TraitHandle(parent=FileHandle(path=foo.pi), name=D, id=0)=TypeDAGNode(args=[], parents=[TraitHandle(parent=FileHandle(path=foo.pi), name=B, id=0), TraitHandle(parent=FileHandle(path=foo.pi), name=C, id=0)])})
    """.trimIndent()

    @Test
    fun testSupertypeDAG() {
        val instance = defaultInstance()
        instance.addHandler(defaultHandler)

        instance.add(tree)
        val value = instance.access { queries ->
            rootPackage
                .hierarchyIterator(queries)
                .asSequence()
                .filterIsInstance<NewTypeHandle>()
                .map { queries.supertypeDAG[it] }
                .joinToString(separator = "\n")
        }

        assertEquals(expected, value)
    }
}