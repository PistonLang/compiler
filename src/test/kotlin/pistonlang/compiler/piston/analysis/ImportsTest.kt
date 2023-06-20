package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.handles.*
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.piston.TestFileTree
import pistonlang.compiler.piston.add
import pistonlang.compiler.piston.fileTree
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.nonEmptyListOf
import kotlin.test.assertEquals

class ImportsTest {
    private val testFileTree: TestFileTree = fileTree {
        child("foo") {
            file("a.pi") {
                """
                    def a(num: Int) = 2 * num
                """.trimIndent()
            }
        }
        child("bar") {
            file("items.pi") {
                """
                    val a = 5
                    var b = 10
                """.trimIndent()
            }
            child("c") {
                file("empty.pi") { "" }
            }
        }
        file("test.pi") {
            """
                import {
                    foo.a               // function
                    bar: { a, b, c }    // val, var, package
                }
                
                def useAll() = a(a + b) - c.d
            """.trimIndent()
        }
    }

    private val expected: ImportData =
        ImportData(
            tree = HandleTree(
                dataList = listOf(
                    HandleData(
                        location = NodeLocation(pos = 10..11, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            ItemHandle(
                                file = FileHandle(path = "foo/a.pi"),
                                name = "a",
                                type = ItemType.Function,
                                id = 0
                            )
                        )
                    ), HandleData(
                        location = NodeLocation(pos = 49..50, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            ItemHandle(
                                file = FileHandle(path = "bar/items.pi"),
                                name = "a",
                                type = ItemType.Val,
                                id = 0
                            )
                        )
                    ), HandleData(
                        location = NodeLocation(pos = 52..53, type = PistonType.identifier),
                        handles = nonEmptyListOf(
                            ItemHandle(
                                file = FileHandle(path = "bar/items.pi"),
                                name = "b",
                                type = ItemType.Var,
                                id = 0
                            )
                        )
                    ), HandleData(
                        location = NodeLocation(pos = 55..56, type = PistonType.identifier),
                        handles = nonEmptyListOf(PackageHandle(path = listOf("bar", "c")))
                    )
                ),
                nodes = listOf(
                    HandleTreeNode(
                        fullRange = NodeLocation(pos = 6..37, type = PistonType.importPathAccess),
                        index = 0,
                        children = listOf(
                            HandleTreeNode(
                                fullRange = NodeLocation(pos = 6..9, type = PistonType.identifier),
                                index = 0,
                                children = emptyList()
                            )
                        )
                    ),
                    HandleTreeNode(
                        fullRange = NodeLocation(pos = 37..82, type = PistonType.importSegment),
                        index = -1,
                        children = listOf(
                            HandleTreeNode(
                                fullRange = NodeLocation(pos = 42..45, type = PistonType.identifier),
                                index = 1,
                                children = emptyList()
                            ), HandleTreeNode(
                                fullRange = NodeLocation(pos = 49..50, type = PistonType.identifier),
                                index = 1,
                                children = emptyList()
                            ), HandleTreeNode(
                                fullRange = NodeLocation(pos = 52..53, type = PistonType.identifier),
                                index = 2,
                                children = emptyList()
                            ), HandleTreeNode(
                                fullRange = NodeLocation(pos = 55..56, type = PistonType.identifier),
                                index = 3,
                                children = emptyList()
                            )
                        )
                    )
                )
            ), nameMap = mapOf("a" to listOf(0, 1), "b" to listOf(2), "c" to listOf(3))
        )

    @Test
    fun testImports() {
        val instance = CompilerInstance(QueryVersionData())
        val handler = PistonLanguageHandler(::PistonLexer, PistonParsing::parseFile, instance)
        instance.addHandler(handler)

        instance.add(testFileTree)
        assertEquals(expected, handler.fileImportData[FileHandle("test.pi")].value)
    }
}