package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pistonlang.compiler.common.files.VirtualPackageTree
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.stl.stlTree
import kotlin.test.assertEquals

class ImportsTest {
    private val tree: VirtualPackageTree<Pair<String, String>> = virtualTree {
        child("foo") {
            data("a.pi") {
                """
                    def a(num: Int32): Int32 = 2 * num
                """.trimIndent() to "Dependent([], {})"
            }
        }
        child("bar") {
            data("items.pi") {
                """
                    val a: Int32 = 5
                    var b: Int32 = 10
                """.trimIndent() to "Dependent([], {})"
            }
            child("c") {
                data("empty.pi") {
                    "" to "Dependent([], {})"
                }
            }
        }
        data("test.pi") {
            """
                import {
                    foo.a               // function
                    bar: { a, b, c }    // val, var, package
                }
                
                def useAll(): Int32 = a(a + b) - c.d
            """.trimIndent() to "Dependent([NodeLocation(pos=6..9, type=identifier): [PackagePath(path=foo)], NodeLocation(pos=10..11, type=identifier): [Function(FilePath(path=foo.a.pi), a, 0)], NodeLocation(pos=42..45, type=identifier): [PackagePath(path=bar)], NodeLocation(pos=49..50, type=identifier): [Val(FilePath(path=bar.items.pi), a, 0)], NodeLocation(pos=52..53, type=identifier): [Var(FilePath(path=bar.items.pi), b, 0)], NodeLocation(pos=55..56, type=identifier): [PackagePath(path=bar.c)]], {a=[1, 3], b=[4], c=[5]})"
        }
    }

    @Test
    fun testImports() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)
        val interners = instance.interners
        instance.add(stlTree)

        instance.add(tree.mapValues { it.first })
        instance.access {
            assertAll(tree.map { (file, data) ->
                {
                    val id = interners.fileIds[file]!!
                    assertEquals(data.second, handler.fileImportData[id].qualify(interners))
                }
            })
        }
    }
}