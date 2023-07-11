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

class ParamsTest {
    private val tree = virtualTree {
        data("foo.pi") {
            """
                trait Foo[T] {
                    def foo(a: T, b: T): Int32
                }
                
                class Bar <: Foo[Int32] {
                    def foo(a: Int32, b: Int32): Int32 = a + b
                    
                    def a = 10
                    def a_=(new: Int32) = println(new)
                }
            """.trimIndent()
        }
    }

    private val expected = listOf(
        FunctionHandle(
            parent = TraitHandle(parent = FileHandle(path = "foo.pi"), name = "Foo", id = 0),
            name = "foo",
            id = 0
        ) to Dependent(
            dependencies = listOf(
                HandleData(
                    location = NodeLocation(pos = 4..5, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        TypeParamHandle(
                            parent = TraitHandle(
                                parent = FileHandle(path = "foo.pi"),
                                name = "Foo",
                                id = 0
                            ),
                            id = 0
                        )
                    )
                ),
                HandleData(
                    location = NodeLocation(pos = 10..11, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        TypeParamHandle(
                            parent = TraitHandle(
                                parent = FileHandle(path = "foo.pi"),
                                name = "Foo",
                                id = 0
                            ),
                            id = 0
                        )
                    )
                )
            ),
            data = listOf(
                TypeInstance(
                    type = TypeParamHandle(
                        parent = TraitHandle(
                            parent = FileHandle(path = "foo.pi"),
                            name = "Foo",
                            id = 0
                        ),
                        id = 0
                    ),
                    args = emptyList(),
                    nullable = false
                ),
                TypeInstance(
                    type = TypeParamHandle(
                        parent = TraitHandle(
                            parent = FileHandle(path = "foo.pi"),
                            name = "Foo",
                            id = 0
                        ),
                        id = 0
                    ),
                    args = emptyList(),
                    nullable = false
                )
            )
        ),
        FunctionHandle(
            parent = SingletonClassHandle(parent = FileHandle(path = "foo.pi"), name = "Bar", id = 0),
            name = "foo",
            id = 0
        ) to Dependent(
            dependencies = listOf(
                HandleData(
                    location = NodeLocation(pos = 4..9, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        MultiInstanceClassHandle(
                            parent = FileHandle(path = "piston/numbers.pi"),
                            name = "Int32",
                            id = 0
                        )
                    )
                ),
                HandleData(
                    location = NodeLocation(pos = 14..19, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        MultiInstanceClassHandle(
                            parent = FileHandle(path = "piston/numbers.pi"),
                            name = "Int32",
                            id = 0
                        )
                    )
                )
            ),
            data = listOf(
                TypeInstance(
                    type = MultiInstanceClassHandle(
                        parent = FileHandle(path = "piston/numbers.pi"),
                        name = "Int32",
                        id = 0
                    ),
                    args = emptyList(),
                    nullable = false
                ),
                TypeInstance(
                    type = MultiInstanceClassHandle(
                        parent = FileHandle(path = "piston/numbers.pi"),
                        name = "Int32",
                        id = 0
                    ),
                    args = emptyList(),
                    nullable = false
                )
            )
        ),
        SetterHandle(
            parent = SingletonClassHandle(parent = FileHandle(path = "foo.pi"), name = "Bar", id = 0),
            name = "a",
            id = 0
        ) to Dependent(
            dependencies = listOf(
                HandleData(
                    location = NodeLocation(pos = 6..11, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        MultiInstanceClassHandle(
                            parent = FileHandle(path = "piston/numbers.pi"),
                            name = "Int32",
                            id = 0
                        )
                    )
                )
            ),
            data = listOf(
                TypeInstance(
                    type = MultiInstanceClassHandle(
                        parent = FileHandle(path = "piston/numbers.pi"),
                        name = "Int32",
                        id = 0
                    ),
                    args = emptyList(),
                    nullable = false
                )
            )
        )
    )

    @Test
    fun testParams() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)

        instance.add(tree)
        val value = instance.access { queries ->
            rootPackage
                .hierarchyIterator(queries)
                .asSequence()
                .filterIsInstance<ParameterizedHandle>()
                .map { it to handler.params[it] }
                .toList()
        }

        assertEquals(expected, value)
    }
}