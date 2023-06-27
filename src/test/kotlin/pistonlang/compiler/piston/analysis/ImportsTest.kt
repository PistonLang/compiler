package pistonlang.compiler.piston.analysis

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pistonlang.compiler.common.files.VirtualPackageTree
import pistonlang.compiler.common.files.add
import pistonlang.compiler.common.files.virtualTree
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing
import kotlin.test.assertEquals

class ImportsTest {
    private val tree: VirtualPackageTree<Pair<String, String>> = virtualTree {
        child("foo") {
            data("a.pi") {
                """
                    def a(num: Int32): Int32 = 2 * num
                """.trimIndent() to "ImportData(tree=HandleTree(dataList=[], nodes=[]), nameMap={})"
            }
        }
        child("bar") {
            data("items.pi") {
                """
                    val a: Int32 = 5
                    var b: Int32 = 10
                """.trimIndent() to "ImportData(tree=HandleTree(dataList=[], nodes=[]), nameMap={})"
            }
            child("c") {
                data("empty.pi") { "" to "ImportData(tree=HandleTree(dataList=[], nodes=[]), nameMap={})" }
            }
        }
        data("test.pi") {
            """
                import {
                    foo.a               // function
                    bar: { a, b, c }    // val, var, package
                }
                
                def useAll(): Int32 = a(a + b) - c.d
            """.trimIndent() to "ImportData(tree=HandleTree(dataList=[HandleData(location=NodeLocation(pos=10..11, type=identifier), handles=NonEmptyList(list=[FunctionHandle(parent=FileHandle(path=foo/a.pi), name=a, id=0)])), HandleData(location=NodeLocation(pos=49..50, type=identifier), handles=NonEmptyList(list=[ValHandle(parent=FileHandle(path=bar/items.pi), name=a, id=0)])), HandleData(location=NodeLocation(pos=52..53, type=identifier), handles=NonEmptyList(list=[VarHandle(parent=FileHandle(path=bar/items.pi), name=b, id=0)])), HandleData(location=NodeLocation(pos=55..56, type=identifier), handles=NonEmptyList(list=[PackageHandle(path=[bar, c])]))], nodes=[HandleTreeNode(fullRange=NodeLocation(pos=6..11, type=importPathAccess), index=0, children=[HandleTreeNode(fullRange=NodeLocation(pos=6..9, type=identifier), index=0, children=[])]), HandleTreeNode(fullRange=NodeLocation(pos=42..58, type=importSegment), index=-1, children=[HandleTreeNode(fullRange=NodeLocation(pos=42..45, type=identifier), index=1, children=[]), HandleTreeNode(fullRange=NodeLocation(pos=49..50, type=identifier), index=1, children=[]), HandleTreeNode(fullRange=NodeLocation(pos=52..53, type=identifier), index=2, children=[]), HandleTreeNode(fullRange=NodeLocation(pos=55..56, type=identifier), index=3, children=[])])]), nameMap={a=[0, 1], b=[2], c=[3]})"
        }
    }

    @Test
    fun testImports() {
        val instance = CompilerInstance(QueryVersionData())
        val handler = PistonLanguageHandler(::PistonLexer, PistonParsing::parseFile, instance)
        instance.addHandler(handler)

        instance.add(tree.mapValues { it.first })
        assertAll(tree.map { (file, data) ->
            {
                assertEquals(data.second, handler.fileImportData[file].value.toString())
            }
        })
    }
}