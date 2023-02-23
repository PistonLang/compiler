package pistonlang.compiler.piston.parser

import org.junit.jupiter.api.Test
import pistonlang.compiler.common.parser.Parser
import pistonlang.compiler.common.parser.RedNode
import kotlin.test.assertEquals

class PistonParsingTest {
    @Test
    fun testParser() {
        val code = """
            def test[T](a: Int, b: Int, c: T) = println((a + b).toString() + c)
        """.trimIndent()

        val expected = """
File@0
└─FunctionDef@0
  ├─DefKw@0
  ├─Whitespace@3
  ├─Identifier@4
  ├─TypeParams@8
  │ ├─LBracket@8
  │ ├─Identifier@9
  │ └─RBracket@10
  ├─FunctionParams@11
  │ ├─LParen@11
  │ ├─FunctionParam@12
  │ │ ├─Identifier@12
  │ │ └─TypeAnnotation@13
  │ │   ├─Colon@13
  │ │   ├─Whitespace@14
  │ │   └─TypePath@15
  │ │     └─PathSegment@15
  │ │       └─Identifier@15
  │ ├─Comma@18
  │ ├─Whitespace@19
  │ ├─FunctionParam@20
  │ │ ├─Identifier@20
  │ │ └─TypeAnnotation@21
  │ │   ├─Colon@21
  │ │   ├─Whitespace@22
  │ │   └─TypePath@23
  │ │     └─PathSegment@23
  │ │       └─Identifier@23
  │ ├─Comma@26
  │ ├─Whitespace@27
  │ ├─FunctionParam@28
  │ │ ├─Identifier@28
  │ │ └─TypeAnnotation@29
  │ │   ├─Colon@29
  │ │   ├─Whitespace@30
  │ │   └─TypePath@31
  │ │     └─PathSegment@31
  │ │       └─Identifier@31
  │ ├─RParen@32
  │ └─Whitespace@33
  └─ExpressionBody@34
    ├─Eq@34
    ├─Whitespace@35
    └─CallExpression@36
      ├─IdentifierExpression@36
      │ └─PathSegment@36
      │   └─Identifier@36
      ├─LParen@43
      ├─PlusExpression@44
      │ ├─CallExpression@44
      │ │ ├─AccessExpression@44
      │ │ │ ├─NestedExpression@44
      │ │ │ │ ├─LParen@44
      │ │ │ │ ├─PlusExpression@45
      │ │ │ │ │ ├─IdentifierExpression@45
      │ │ │ │ │ │ └─PathSegment@45
      │ │ │ │ │ │   ├─Identifier@45
      │ │ │ │ │ │   └─Whitespace@46
      │ │ │ │ │ ├─Plus@47
      │ │ │ │ │ ├─Whitespace@48
      │ │ │ │ │ └─IdentifierExpression@49
      │ │ │ │ │   └─PathSegment@49
      │ │ │ │ │     └─Identifier@49
      │ │ │ │ └─RParen@50
      │ │ │ ├─Dot@51
      │ │ │ └─PathSegment@52
      │ │ │   └─Identifier@52
      │ │ ├─LParen@60
      │ │ ├─RParen@61
      │ │ └─Whitespace@62
      │ ├─Plus@63
      │ ├─Whitespace@64
      │ └─IdentifierExpression@65
      │   └─PathSegment@65
      │     └─Identifier@65
      └─RParen@66
        """.trimIndent()

        val parser = Parser(PistonLexer(code), PistonType.file)

        val green = PistonParsing.parseFile(parser)
        val red = RedNode(null, green, 0)
        val builder = StringBuilder()
        red.format(builder, "")

        val res = builder.toString()

        assertEquals(expected, res)
    }
}