package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pistonlang.compiler.common.files.VirtualPackageTree
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import kotlin.test.assertEquals

class ImportsTest {
    private val tree: VirtualPackageTree<Pair<String, String>> = virtualTree {
        child("foo") {
            data("a.pi") {
                """
                    def a(num: Int32): Int32 = 2 * num
                """.trimIndent() to "Dependent(dependencies=[], data={})"
            }
        }
        child("bar") {
            data("items.pi") {
                """
                    val a: Int32 = 5
                    var b: Int32 = 10
                """.trimIndent() to "Dependent(dependencies=[], data={})"
            }
            child("c") {
                data("empty.pi") {
                    "" to "Dependent(dependencies=[], data={})"
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
            """.trimIndent() to "Dependent(dependencies=[HandleData(location=NodeLocation(pos=6..9, type=identifier), handles=NonEmptyList(nested=[PackageHandle(path=foo)])), HandleData(location=NodeLocation(pos=10..11, type=identifier), handles=NonEmptyList(nested=[FunctionHandle(parent=FileHandle(path=foo.a.pi), name=a, id=0)])), HandleData(location=NodeLocation(pos=42..45, type=identifier), handles=NonEmptyList(nested=[PackageHandle(path=bar)])), HandleData(location=NodeLocation(pos=49..50, type=identifier), handles=NonEmptyList(nested=[ValHandle(parent=FileHandle(path=bar.items.pi), name=a, id=0)])), HandleData(location=NodeLocation(pos=52..53, type=identifier), handles=NonEmptyList(nested=[VarHandle(parent=FileHandle(path=bar.items.pi), name=b, id=0)])), HandleData(location=NodeLocation(pos=55..56, type=identifier), handles=NonEmptyList(nested=[PackageHandle(path=bar.c)]))], data={a=[1, 3], b=[4], c=[5]})"
        }
    }

    @Test
    fun testImports() {
        val instance = defaultInstance()
        val handler = instance.addHandler(defaultHandler)

        instance.add(tree.mapValues { it.first })
        instance.access {
            assertAll(tree.map { (file, data) ->
                {
                    assertEquals(data.second, handler.fileImportData[file].toString())
                }
            })
        }
    }
}