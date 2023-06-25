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
            file("a.pi") {
                """
                    def a(num: Int32): Int32 = 2 * num
                """.trimIndent() to "ImportData(tree=ReferenceTree(dataList=[], nodes=[]), nameMap={})"
            }
        }
        child("bar") {
            file("items.pi") {
                """
                    val a: Int32 = 5
                    var b: Int32 = 10
                """.trimIndent() to "ImportData(tree=ReferenceTree(dataList=[], nodes=[]), nameMap={})"
            }
            child("c") {
                file("empty.pi") { "" to "ImportData(tree=ReferenceTree(dataList=[], nodes=[]), nameMap={})" }
            }
        }
        file("test.pi") {
            """
                import {
                    foo.a               // function
                    bar: { a, b, c }    // val, var, package
                }
                
                def useAll(): Int32 = a(a + b) - c.d
            """.trimIndent() to "ImportData(tree=ReferenceTree(dataList=[ReferenceData(location=NodeLocation(pos=10..11, type=identifier), references=NonEmptyList(list=[ItemReference(parent=FileReference(path=foo/a.pi), name=a, type=Function, id=0)])), ReferenceData(location=NodeLocation(pos=49..50, type=identifier), references=NonEmptyList(list=[ItemReference(parent=FileReference(path=bar/items.pi), name=a, type=Val, id=0)])), ReferenceData(location=NodeLocation(pos=52..53, type=identifier), references=NonEmptyList(list=[ItemReference(parent=FileReference(path=bar/items.pi), name=b, type=Var, id=0)])), ReferenceData(location=NodeLocation(pos=55..56, type=identifier), references=NonEmptyList(list=[PackageReference(path=[bar, c])]))], nodes=[ReferenceTreeNode(fullRange=NodeLocation(pos=6..11, type=importPathAccess), index=0, children=[ReferenceTreeNode(fullRange=NodeLocation(pos=6..9, type=identifier), index=0, children=[])]), ReferenceTreeNode(fullRange=NodeLocation(pos=42..58, type=importSegment), index=-1, children=[ReferenceTreeNode(fullRange=NodeLocation(pos=42..45, type=identifier), index=1, children=[]), ReferenceTreeNode(fullRange=NodeLocation(pos=49..50, type=identifier), index=1, children=[]), ReferenceTreeNode(fullRange=NodeLocation(pos=52..53, type=identifier), index=2, children=[]), ReferenceTreeNode(fullRange=NodeLocation(pos=55..56, type=identifier), index=3, children=[])])]), nameMap={a=[0, 1], b=[2], c=[3]})"
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