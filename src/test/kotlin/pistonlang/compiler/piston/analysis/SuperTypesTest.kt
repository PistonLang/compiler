package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.nonEmptyListOf
import kotlin.test.assertEquals

class SuperTypesTest {
    private val tree = virtualTree {
        data("foo.pi") {
            """
                trait Empty
                
                trait Foo[T] <: Empty
                
                class Test[A, B](a: A, b: B) <: Foo[A]
            """.trimIndent()
        }
    }

    private val expected = listOf(
        emptySuperTypeData,
        Dependent(
            dependencies = listOf(
                HandleData(
                    location = NodeLocation(pos = 0..5, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        TraitHandle(parent = FileHandle(path = "foo.pi"), name = "Empty", id = 0)
                    )
                )
            ),
            data = nonEmptyListOf(
                TypeInstance(
                    type = TraitHandle(
                        parent = FileHandle(path = "foo.pi"),
                        name = "Empty",
                        id = 0
                    ),
                    args = emptyList(),
                    nullable = false
                )
            )
        ),
        Dependent(
            dependencies = listOf(
                HandleData(
                    location = NodeLocation(pos = 0..3, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        TraitHandle(parent = FileHandle(path = "foo.pi"), name = "Foo", id = 0)
                    )
                ), HandleData(
                    location = NodeLocation(pos = 4..5, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        TypeParamHandle(
                            parent = MultiInstanceClassHandle(
                                parent = FileHandle(path = "foo.pi"),
                                name = "Test",
                                id = 0
                            ), id = 0
                        )
                    )
                )
            ),
            data = nonEmptyListOf(
                TypeInstance(
                    type = TraitHandle(
                        parent = FileHandle(path = "foo.pi"),
                        name = "Foo",
                        id = 0
                    ),
                    args = listOf(
                        TypeInstance(
                            type = TypeParamHandle(
                                parent = MultiInstanceClassHandle(
                                    parent = FileHandle(path = "foo.pi"),
                                    name = "Test",
                                    id = 0
                                ), id = 0
                            ), args = emptyList(), nullable = false
                        )
                    ),
                    nullable = false
                )
            )
        )
    )

    @Test
    fun testChildItems() {
        val instance = defaultInstance()
        val handler = defaultHandler(instance)
        instance.addHandler(handler)

        instance.add(tree)
        val value = instance
            .packageItems[PackageHandle(emptyList())]
            .asSequence()
            .flatMap { (_, values) ->
                values
                    .filter { it.itemType.type }
                    .map { key -> handler.supertypes[key as NewTypeHandle] }
            }
            .toList()

        assertEquals(expected, value)
    }
}