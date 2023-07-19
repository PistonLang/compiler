package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.memberHierarchyIterator
import pistonlang.compiler.common.main.stl.stlTree
import kotlin.test.assertEquals

class ChildItemsTest {
    private val tree = virtualTree {
        data("func.pi") {
            """
                def func[A where A <: Int32](a: A, b: Int32): Int32 = a + b
            """.trimIndent()
        }

        data("class.pi") {
            """
                class Foo[T](t: T) <: Bar {
                    val t: T = t
                    
                    def foo() = println(t.toString())
                }
            """.trimIndent()
        }
        data("trait.pi") {
            """
                trait Bar {
                    def foo()
                    
                    def bar: Int32 = 10
                }
            """.trimIndent()
        }
    }

    private val expected = """
        Function(FilePath(path=func.pi), func, 0): MemberList(list=[{}, {}, {}, {}, {}, {}, {}, {}])
        MultiInstanceClass(FilePath(path=class.pi), Foo, 0): MemberList(list=[{}, {}, {}, {t=[NodeLocation(pos=32..44, type=propertyDef)]}, {}, {foo=[NodeLocation(pos=54..87, type=functionDef)]}, {}, {}])
        Val(MultiInstanceClass(FilePath(path=class.pi), Foo, 0), t, 0): MemberList(list=[{}, {}, {}, {}, {}, {}, {}, {}])
        Function(MultiInstanceClass(FilePath(path=class.pi), Foo, 0), foo, 0): MemberList(list=[{}, {}, {}, {}, {}, {}, {}, {}])
        Trait(FilePath(path=trait.pi), Bar, 0): MemberList(list=[{}, {}, {}, {}, {}, {foo=[NodeLocation(pos=16..35, type=functionDef)]}, {bar=[NodeLocation(pos=35..54, type=functionDef)]}, {}])
        Function(Trait(FilePath(path=trait.pi), Bar, 0), foo, 0): MemberList(list=[{}, {}, {}, {}, {}, {}, {}, {}])
        Getter(Trait(FilePath(path=trait.pi), Bar, 0), bar, 0): MemberList(list=[{}, {}, {}, {}, {}, {}, {}, {}])
    """.trimIndent()

    @Test
    fun testChildItems() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)
        val interners = instance.interners
        instance.add(stlTree)

        instance.add(tree)
        val got = instance.access { queries ->
            interners
                .packIds[rootPackage]!!
                .memberHierarchyIterator(queries)
                .asSequence()
                .map { it to handler.childItems[it] }
                .joinToString(separator = "\n") { it.qualify(interners) }
        }

        assertEquals(expected, got)
    }
}