package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.main.hierarchyIterator
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.nonEmptyListOf
import kotlin.test.assertEquals

class ReturnTypeTest {
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

    private val expected = listOf(
        FunctionHandle(parent = FileHandle(path = "func.pi"), name = "func", id = 0) to Dependent(
            dependencies = listOf(
                HandleData(
                    location = NodeLocation(pos = 0..5, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        MultiInstanceClassHandle(
                            parent = FileHandle(path = "piston.numbers.pi"),
                            name = "Int32",
                            id = 0
                        )
                    )
                )
            ),
            data = TypeInstance(
                type = MultiInstanceClassHandle(
                    parent = FileHandle(path = "piston.numbers.pi"),
                    name = "Int32",
                    id = 0
                ),
                args = emptyList(),
                nullable = false
            )
        ),
        ValHandle(
            parent = MultiInstanceClassHandle(parent = FileHandle(path = "class.pi"), name = "Foo", id = 0),
            name = "t",
            id = 0
        ) to Dependent(
            dependencies = listOf(
                HandleData(
                    location = NodeLocation(pos = 0..1, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        TypeParamHandle(
                            parent = MultiInstanceClassHandle(
                                parent = FileHandle(path = "class.pi"),
                                name = "Foo",
                                id = 0
                            ),
                            id = 0
                        )
                    )
                )
            ),
            data = TypeInstance(
                type = TypeParamHandle(
                    parent = MultiInstanceClassHandle(
                        parent = FileHandle(path = "class.pi"),
                        name = "Foo",
                        id = 0
                    ),
                    id = 0
                ),
                args = emptyList(),
                nullable = false
            )
        ),
        FunctionHandle(
            parent = MultiInstanceClassHandle(parent = FileHandle(path = "class.pi"), name = "Foo", id = 0),
            name = "foo",
            id = 0
        ) to Dependent(
            dependencies = emptyList(),
            data = unitInstance
        ),
        FunctionHandle(
            parent = TraitHandle(parent = FileHandle(path = "trait.pi"), name = "Bar", id = 0),
            name = "foo",
            id = 0
        ) to Dependent(
            dependencies = emptyList(),
            data = unitInstance
        ),
        GetterHandle(
            parent = TraitHandle(parent = FileHandle(path = "trait.pi"), name = "Bar", id = 0),
            name = "bar",
            id = 0
        ) to Dependent(
            dependencies = listOf(
                HandleData(
                    location = NodeLocation(pos = 0..5, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        MultiInstanceClassHandle(
                            parent = FileHandle(path = "piston.numbers.pi"),
                            name = "Int32",
                            id = 0
                        )
                    )
                )
            ),
            data = TypeInstance(
                type = MultiInstanceClassHandle(
                    parent = FileHandle(path = "piston.numbers.pi"),
                    name = "Int32",
                    id = 0
                ),
                args = emptyList(), nullable = false
            )
        )
    )

    @Test
    fun testChildItems() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)

        instance.add(tree)
        val value = instance.access { queries ->
            rootPackage
                .hierarchyIterator(queries)
                .asSequence()
                .filterIsInstance<TypedHandle>()
                .map { it to handler.returnType[it] }
                .toList()
        }

        assertEquals(expected, value)
    }
}