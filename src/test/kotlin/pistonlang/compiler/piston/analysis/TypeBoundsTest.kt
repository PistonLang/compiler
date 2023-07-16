package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.rootPackage
import pistonlang.compiler.common.main.hierarchyIterator
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
        (MultiInstanceClassHandle(parent=FileHandle(path=classes.pi), name=Foo, id=0), Dependent(dependencies=[HandleData(location=NodeLocation(pos=6..7, type=identifier), handles=NonEmptyList(nested=[TypeParamHandle(parent=MultiInstanceClassHandle(parent=FileHandle(path=classes.pi), name=Foo, id=0), id=0)])), HandleData(location=NodeLocation(pos=11..21, type=identifier), handles=NonEmptyList(nested=[TraitHandle(parent=FileHandle(path=classes.pi), name=Comparable, id=0)])), HandleData(location=NodeLocation(pos=22..23, type=identifier), handles=NonEmptyList(nested=[TypeParamHandle(parent=MultiInstanceClassHandle(parent=FileHandle(path=classes.pi), name=Foo, id=0), id=0)])), HandleData(location=NodeLocation(pos=26..27, type=identifier), handles=NonEmptyList(nested=[TypeParamHandle(parent=MultiInstanceClassHandle(parent=FileHandle(path=classes.pi), name=Foo, id=0), id=0)])), HandleData(location=NodeLocation(pos=31..32, type=identifier), handles=NonEmptyList(nested=[TypeParamHandle(parent=MultiInstanceClassHandle(parent=FileHandle(path=classes.pi), name=Foo, id=0), id=1)])), HandleData(location=NodeLocation(pos=34..35, type=identifier), handles=NonEmptyList(nested=[TypeParamHandle(parent=MultiInstanceClassHandle(parent=FileHandle(path=classes.pi), name=Foo, id=0), id=1)])), HandleData(location=NodeLocation(pos=39..42, type=identifier), handles=NonEmptyList(nested=[TraitHandle(parent=FileHandle(path=classes.pi), name=Bar, id=0)]))], data=[[TypeInstance(type=TraitHandle(parent=FileHandle(path=classes.pi), name=Comparable, id=0), args=[TypeInstance(type=TypeParamHandle(parent=MultiInstanceClassHandle(parent=FileHandle(path=classes.pi), name=Foo, id=0), id=0), args=[], nullable=false)], nullable=false), TypeInstance(type=TypeParamHandle(parent=MultiInstanceClassHandle(parent=FileHandle(path=classes.pi), name=Foo, id=0), id=1), args=[], nullable=false)], [TypeInstance(type=TraitHandle(parent=FileHandle(path=classes.pi), name=Bar, id=0), args=[], nullable=false)]]))
        (TraitHandle(parent=FileHandle(path=classes.pi), name=Comparable, id=0), Dependent(dependencies=[], data=[[]]))
        (FunctionHandle(parent=TraitHandle(parent=FileHandle(path=classes.pi), name=Comparable, id=0), name=compare, id=0), Dependent(dependencies=[], data=[]))
        (TraitHandle(parent=FileHandle(path=classes.pi), name=Bar, id=0), Dependent(dependencies=[], data=[]))
        (FunctionHandle(parent=FileHandle(path=classes.pi), name=foo, id=0), Dependent(dependencies=[HandleData(location=NodeLocation(pos=6..7, type=identifier), handles=NonEmptyList(nested=[TypeParamHandle(parent=FunctionHandle(parent=FileHandle(path=classes.pi), name=foo, id=0), id=0)])), HandleData(location=NodeLocation(pos=11..21, type=identifier), handles=NonEmptyList(nested=[TraitHandle(parent=FileHandle(path=classes.pi), name=Comparable, id=0)])), HandleData(location=NodeLocation(pos=22..23, type=identifier), handles=NonEmptyList(nested=[TypeParamHandle(parent=FunctionHandle(parent=FileHandle(path=classes.pi), name=foo, id=0), id=0)]))], data=[[TypeInstance(type=TraitHandle(parent=FileHandle(path=classes.pi), name=Comparable, id=0), args=[TypeInstance(type=TypeParamHandle(parent=FunctionHandle(parent=FileHandle(path=classes.pi), name=foo, id=0), id=0), args=[], nullable=false)], nullable=false)]]))
    """.trimIndent()

    @Test
    fun testTypeParams() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)

        instance.add(tree)
        val value = instance.access { queries ->
            rootPackage
                .hierarchyIterator(queries)
                .asSequence()
                .map { it to handler.typeParamBounds[it] }
                .joinToString(separator = "\n")
        }

        assertEquals(expected, value)
    }
}