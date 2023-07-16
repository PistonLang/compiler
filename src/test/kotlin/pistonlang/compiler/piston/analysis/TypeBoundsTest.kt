package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.main.hierarchyIterator
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.common.types.TypeInstance
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.nonEmptyListOf
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

    private val expected: List<Pair<MemberHandle, TypeBoundData>> =
        listOf(
            TraitHandle(
                parent = FileHandle(path = "classes.pi"),
                name = "Comparable",
                id = 0
            ) to Dependent(
                emptyList(),
                listOf(emptyList())
            ),
            FunctionHandle(
                parent = TraitHandle(parent = FileHandle(path = "classes.pi"), name = "Comparable", id = 0),
                name = "compare",
                id = 0
            ) to emptyTypeBoundData,
            TraitHandle(
                parent = FileHandle(path = "classes.pi"),
                name = "Bar",
                id = 0
            ) to emptyTypeBoundData,
            MultiInstanceClassHandle(
                parent = FileHandle(path = "classes.pi"),
                name = "Foo",
                id = 0
            ) to Dependent(
                dependencies = listOf(
                    HandleData(
                        location = NodeLocation(pos = 6..7, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            TypeParamHandle(
                                parent = MultiInstanceClassHandle(
                                    parent = FileHandle(path = "classes.pi"),
                                    name = "Foo",
                                    id = 0
                                ),
                                id = 0
                            )
                        )
                    ), HandleData(
                        location = NodeLocation(pos = 11..21, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            TraitHandle(
                                parent = FileHandle(path = "classes.pi"),
                                name = "Comparable",
                                id = 0
                            )
                        )
                    ), HandleData(
                        location = NodeLocation(pos = 22..23, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            TypeParamHandle(
                                parent = MultiInstanceClassHandle(
                                    parent = FileHandle(path = "classes.pi"),
                                    name = "Foo",
                                    id = 0
                                ),
                                id = 0
                            )
                        )
                    ), HandleData(
                        location = NodeLocation(pos = 26..27, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            TypeParamHandle(
                                parent = MultiInstanceClassHandle(
                                    parent = FileHandle(path = "classes.pi"),
                                    name = "Foo",
                                    id = 0
                                ),
                                id = 0
                            )
                        )
                    ), HandleData(
                        location = NodeLocation(pos = 31..32, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            TypeParamHandle(
                                parent = MultiInstanceClassHandle(
                                    parent = FileHandle(path = "classes.pi"),
                                    name = "Foo",
                                    id = 0
                                ),
                                id = 1
                            )
                        )
                    ), HandleData(
                        location = NodeLocation(pos = 34..35, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            TypeParamHandle(
                                parent = MultiInstanceClassHandle(
                                    parent = FileHandle(path = "classes.pi"),
                                    name = "Foo",
                                    id = 0
                                ), id = 1
                            )
                        )
                    ), HandleData(
                        location = NodeLocation(pos = 39..42, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            TraitHandle(
                                parent = FileHandle(path = "classes.pi"),
                                name = "Bar",
                                id = 0
                            )
                        )
                    )
                ),
                data = listOf(
                    listOf(
                        TypeInstance(
                            type = TraitHandle(parent = FileHandle(path = "classes.pi"), name = "Comparable", id = 0),
                            args = listOf(
                                TypeInstance(
                                    type = TypeParamHandle(
                                        parent = MultiInstanceClassHandle(
                                            parent = FileHandle(path = "classes.pi"),
                                            name = "Foo",
                                            id = 0
                                        ),
                                        id = 0
                                    ),
                                    args = emptyList(),
                                    nullable = false
                                )
                            ),
                            nullable = false
                        ),
                        TypeInstance(
                            type = TypeParamHandle(
                                parent = MultiInstanceClassHandle(
                                    parent = FileHandle(path = "classes.pi"),
                                    name = "Foo",
                                    id = 0
                                ),
                                id = 1
                            ),
                            args = emptyList(),
                            nullable = false
                        )
                    ),
                    listOf(
                        TypeInstance(
                            type = TraitHandle(parent = FileHandle(path = "classes.pi"), name = "Bar", id = 0),
                            args = emptyList(),
                            nullable = false
                        )
                    )
                )
            ),
            FunctionHandle(
                parent = FileHandle(path = "classes.pi"),
                name = "foo",
                id = 0
            ) to Dependent(
                dependencies = listOf(
                    HandleData(
                        location = NodeLocation(pos = 6..7, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            TypeParamHandle(
                                parent = FunctionHandle(
                                    parent = FileHandle(path = "classes.pi"),
                                    name = "foo",
                                    id = 0
                                ),
                                id = 0
                            )
                        )
                    ), HandleData(
                        location = NodeLocation(pos = 11..21, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            TraitHandle(
                                parent = FileHandle(path = "classes.pi"),
                                name = "Comparable",
                                id = 0
                            )
                        )
                    ), HandleData(
                        location = NodeLocation(pos = 22..23, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            TypeParamHandle(
                                parent = FunctionHandle(
                                    parent = FileHandle(path = "classes.pi"),
                                    name = "foo",
                                    id = 0
                                ),
                                id = 0
                            )
                        )
                    )
                ),
                data = listOf(
                    listOf(
                        TypeInstance(
                            type = TraitHandle(parent = FileHandle(path = "classes.pi"), name = "Comparable", id = 0),
                            args = listOf(
                                TypeInstance(
                                    type = TypeParamHandle(
                                        parent = FunctionHandle(
                                            parent = FileHandle(path = "classes.pi"),
                                            name = "foo",
                                            id = 0
                                        ),
                                        id = 0
                                    ),
                                    args = emptyList(),
                                    nullable = false
                                )
                            ),
                            nullable = false
                        )
                    )
                )
            )
        )

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
                .toList()
        }

        assertEquals(expected, value)
    }
}