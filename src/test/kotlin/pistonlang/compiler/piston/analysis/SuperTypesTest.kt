package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing
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

    val expected = listOf(
        emptySuperTypeData,
        SupertypeData(
            tree = listOf(
                HandleData(
                    location = NodeLocation(pos = 0..5, type = PistonType.identifier),
                    handles = nonEmptyListOf(
                        TraitHandle(parent = FileHandle(path = "foo.pi"), name = "Empty", id = 0)
                    )
                )
            ),
            types = nonEmptyListOf(
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
        SupertypeData(
            tree = listOf(
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
            types = nonEmptyListOf(
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
        val instance = CompilerInstance(QueryVersionData())
        val handler =
            PistonLanguageHandler(::PistonLexer, PistonParsing::parseFile, instance)
        instance.addHandler(handler)

        instance.add(tree)
        val value = instance
            .packageItems[PackageHandle(emptyList())].value
            .asSequence()
            .flatMap { (_, values) ->
                values
                    .filter { it.itemType.type }
                    .map { key -> handler.supertypes[key as NewTypeHandle].value }
            }
            .toList()

        assertEquals(expected, value)
    }
}