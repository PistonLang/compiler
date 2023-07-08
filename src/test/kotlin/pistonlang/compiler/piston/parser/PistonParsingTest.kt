package pistonlang.compiler.piston.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pistonlang.compiler.common.parser.Parser
import pistonlang.compiler.common.parser.nodes.RedNode
import kotlin.test.assertEquals

class PistonParsingTest {
    private val codeExpectedMap: List<Pair<String, String>> = listOf(
        "def test[T](a: Int, b: Int, c: T) = println((a + b).toString() + c)" to """
File@0
└─FunctionDef@0
  ├─DefKw@0
  ├─Whitespace( )@3
  ├─Identifier(test)@4
  ├─TypeParams@8
  │ ├─LBracket@8
  │ ├─Identifier(T)@9
  │ └─RBracket@10
  ├─FunctionParams@11
  │ ├─LParen@11
  │ ├─FunctionParam@12
  │ │ ├─Identifier(a)@12
  │ │ └─TypeAnnotation@13
  │ │   ├─Colon@13
  │ │   ├─Whitespace( )@14
  │ │   └─PathType@15
  │ │     └─PathSegment@15
  │ │       └─Identifier(Int)@15
  │ ├─Comma@18
  │ ├─Whitespace( )@19
  │ ├─FunctionParam@20
  │ │ ├─Identifier(b)@20
  │ │ └─TypeAnnotation@21
  │ │   ├─Colon@21
  │ │   ├─Whitespace( )@22
  │ │   └─PathType@23
  │ │     └─PathSegment@23
  │ │       └─Identifier(Int)@23
  │ ├─Comma@26
  │ ├─Whitespace( )@27
  │ ├─FunctionParam@28
  │ │ ├─Identifier(c)@28
  │ │ └─TypeAnnotation@29
  │ │   ├─Colon@29
  │ │   ├─Whitespace( )@30
  │ │   └─PathType@31
  │ │     └─PathSegment@31
  │ │       └─Identifier(T)@31
  │ └─RParen@32
  ├─Whitespace( )@33
  └─ExpressionBody@34
    ├─Eq@34
    ├─Whitespace( )@35
    └─CallExpression@36
      ├─IdentifierExpression@36
      │ └─PathSegment@36
      │   └─Identifier(println)@36
      ├─LParen@43
      ├─PlusExpression@44
      │ ├─CallExpression@44
      │ │ ├─AccessExpression@44
      │ │ │ ├─NestedExpression@44
      │ │ │ │ ├─LParen@44
      │ │ │ │ ├─PlusExpression@45
      │ │ │ │ │ ├─IdentifierExpression@45
      │ │ │ │ │ │ └─PathSegment@45
      │ │ │ │ │ │   └─Identifier(a)@45
      │ │ │ │ │ ├─Whitespace( )@46
      │ │ │ │ │ ├─Plus@47
      │ │ │ │ │ ├─Whitespace( )@48
      │ │ │ │ │ └─IdentifierExpression@49
      │ │ │ │ │   └─PathSegment@49
      │ │ │ │ │     └─Identifier(b)@49
      │ │ │ │ └─RParen@50
      │ │ │ ├─Dot@51
      │ │ │ └─PathSegment@52
      │ │ │   └─Identifier(toString)@52
      │ │ ├─LParen@60
      │ │ └─RParen@61
      │ ├─Whitespace( )@62
      │ ├─Plus@63
      │ ├─Whitespace( )@64
      │ └─IdentifierExpression@65
      │   └─PathSegment@65
      │     └─Identifier(c)@65
      └─RParen@66
        """.trimIndent(),
        """
    def foo[T](list: List[T]) = Unit
    
    def bar = 10
    
    def bar_=(num: Int) = num.println()
    
    val a = 10
    
    var b = 15
    
    trait A {
        def print() = "A".println()
    }
    
    class B(num: Int) <: A {
        val num = num
    
        def print() = num.println()
    }
    
    class C <: A {
        def print() = 'C'.println()
    }
    """.trimIndent() to """
File@0
├─FunctionDef@0
│ ├─DefKw@0
│ ├─Whitespace( )@3
│ ├─Identifier(foo)@4
│ ├─TypeParams@7
│ │ ├─LBracket@7
│ │ ├─Identifier(T)@8
│ │ └─RBracket@9
│ ├─FunctionParams@10
│ │ ├─LParen@10
│ │ ├─FunctionParam@11
│ │ │ ├─Identifier(list)@11
│ │ │ └─TypeAnnotation@15
│ │ │   ├─Colon@15
│ │ │   ├─Whitespace( )@16
│ │ │   └─PathType@17
│ │ │     └─PathSegment@17
│ │ │       ├─Identifier(List)@17
│ │ │       └─TypeArgs@21
│ │ │         ├─LBracket@21
│ │ │         ├─TypeArg@22
│ │ │         │ └─PathType@22
│ │ │         │   └─PathSegment@22
│ │ │         │     └─Identifier(T)@22
│ │ │         └─RBracket@23
│ │ └─RParen@24
│ ├─Whitespace( )@25
│ └─ExpressionBody@26
│   ├─Eq@26
│   ├─Whitespace( )@27
│   └─IdentifierExpression@28
│     └─PathSegment@28
│       └─Identifier(Unit)@28
├─Newline@32
├─Newline@33
├─FunctionDef@34
│ ├─DefKw@34
│ ├─Whitespace( )@37
│ ├─Identifier(bar)@38
│ ├─Whitespace( )@41
│ └─ExpressionBody@42
│   ├─Eq@42
│   ├─Whitespace( )@43
│   └─LiteralExpression@44
│     └─IntLiteral(10)@44
├─Newline@46
├─Newline@47
├─FunctionDef@48
│ ├─DefKw@48
│ ├─Whitespace( )@51
│ ├─Identifier(bar_=)@52
│ ├─FunctionParams@57
│ │ ├─LParen@57
│ │ ├─FunctionParam@58
│ │ │ ├─Identifier(num)@58
│ │ │ └─TypeAnnotation@61
│ │ │   ├─Colon@61
│ │ │   ├─Whitespace( )@62
│ │ │   └─PathType@63
│ │ │     └─PathSegment@63
│ │ │       └─Identifier(Int)@63
│ │ └─RParen@66
│ ├─Whitespace( )@67
│ └─ExpressionBody@68
│   ├─Eq@68
│   ├─Whitespace( )@69
│   └─CallExpression@70
│     ├─AccessExpression@70
│     │ ├─IdentifierExpression@70
│     │ │ └─PathSegment@70
│     │ │   └─Identifier(num)@70
│     │ ├─Dot@73
│     │ └─PathSegment@74
│     │   └─Identifier(println)@74
│     ├─LParen@81
│     └─RParen@82
├─Newline@83
├─Newline@84
├─PropertyDef@85
│ ├─ValKw@85
│ ├─Whitespace( )@88
│ ├─Identifier(a)@89
│ ├─Whitespace( )@90
│ └─ExpressionBody@91
│   ├─Eq@91
│   ├─Whitespace( )@92
│   └─LiteralExpression@93
│     └─IntLiteral(10)@93
├─Newline@95
├─Newline@96
├─PropertyDef@97
│ ├─VarKw@97
│ ├─Whitespace( )@100
│ ├─Identifier(b)@101
│ ├─Whitespace( )@102
│ └─ExpressionBody@103
│   ├─Eq@103
│   ├─Whitespace( )@104
│   └─LiteralExpression@105
│     └─IntLiteral(15)@105
├─Newline@107
├─Newline@108
├─TraitDef@109
│ ├─TraitKw@109
│ ├─Whitespace( )@114
│ ├─Identifier(A)@115
│ ├─Whitespace( )@116
│ └─StatementBlock@117
│   ├─LBrace@117
│   ├─Newline@118
│   ├─Whitespace(    )@119
│   ├─FunctionDef@123
│   │ ├─DefKw@123
│   │ ├─Whitespace( )@126
│   │ ├─Identifier(print)@127
│   │ ├─FunctionParams@132
│   │ │ ├─LParen@132
│   │ │ └─RParen@133
│   │ ├─Whitespace( )@134
│   │ └─ExpressionBody@135
│   │   ├─Eq@135
│   │   ├─Whitespace( )@136
│   │   └─CallExpression@137
│   │     ├─AccessExpression@137
│   │     │ ├─LiteralExpression@137
│   │     │ │ └─StringLiteral("A")@137
│   │     │ ├─Dot@140
│   │     │ └─PathSegment@141
│   │     │   └─Identifier(println)@141
│   │     ├─LParen@148
│   │     └─RParen@149
│   ├─Newline@150
│   └─RBrace@151
├─Newline@152
├─Newline@153
├─ClassDef@154
│ ├─ClassKw@154
│ ├─Whitespace( )@159
│ ├─Identifier(B)@160
│ ├─FunctionParams@161
│ │ ├─LParen@161
│ │ ├─FunctionParam@162
│ │ │ ├─Identifier(num)@162
│ │ │ └─TypeAnnotation@165
│ │ │   ├─Colon@165
│ │ │   ├─Whitespace( )@166
│ │ │   └─PathType@167
│ │ │     └─PathSegment@167
│ │ │       └─Identifier(Int)@167
│ │ └─RParen@170
│ ├─Whitespace( )@171
│ ├─Supertypes@172
│ │ ├─Subtype@172
│ │ ├─Whitespace( )@174
│ │ └─IntersectionType@175
│ │   └─PathType@175
│ │     └─PathSegment@175
│ │       └─Identifier(A)@175
│ ├─Whitespace( )@176
│ └─StatementBlock@177
│   ├─LBrace@177
│   ├─Newline@178
│   ├─Whitespace(    )@179
│   ├─PropertyDef@183
│   │ ├─ValKw@183
│   │ ├─Whitespace( )@186
│   │ ├─Identifier(num)@187
│   │ ├─Whitespace( )@190
│   │ └─ExpressionBody@191
│   │   ├─Eq@191
│   │   ├─Whitespace( )@192
│   │   └─IdentifierExpression@193
│   │     └─PathSegment@193
│   │       └─Identifier(num)@193
│   ├─Newline@196
│   ├─Newline@197
│   ├─Whitespace(    )@198
│   ├─FunctionDef@202
│   │ ├─DefKw@202
│   │ ├─Whitespace( )@205
│   │ ├─Identifier(print)@206
│   │ ├─FunctionParams@211
│   │ │ ├─LParen@211
│   │ │ └─RParen@212
│   │ ├─Whitespace( )@213
│   │ └─ExpressionBody@214
│   │   ├─Eq@214
│   │   ├─Whitespace( )@215
│   │   └─CallExpression@216
│   │     ├─AccessExpression@216
│   │     │ ├─IdentifierExpression@216
│   │     │ │ └─PathSegment@216
│   │     │ │   └─Identifier(num)@216
│   │     │ ├─Dot@219
│   │     │ └─PathSegment@220
│   │     │   └─Identifier(println)@220
│   │     ├─LParen@227
│   │     └─RParen@228
│   ├─Newline@229
│   └─RBrace@230
├─Newline@231
├─Newline@232
└─ClassDef@233
  ├─ClassKw@233
  ├─Whitespace( )@238
  ├─Identifier(C)@239
  ├─Whitespace( )@240
  ├─Supertypes@241
  │ ├─Subtype@241
  │ ├─Whitespace( )@243
  │ └─IntersectionType@244
  │   └─PathType@244
  │     └─PathSegment@244
  │       └─Identifier(A)@244
  ├─Whitespace( )@245
  └─StatementBlock@246
    ├─LBrace@246
    ├─Newline@247
    ├─Whitespace(    )@248
    ├─FunctionDef@252
    │ ├─DefKw@252
    │ ├─Whitespace( )@255
    │ ├─Identifier(print)@256
    │ ├─FunctionParams@261
    │ │ ├─LParen@261
    │ │ └─RParen@262
    │ ├─Whitespace( )@263
    │ └─ExpressionBody@264
    │   ├─Eq@264
    │   ├─Whitespace( )@265
    │   └─CallExpression@266
    │     ├─AccessExpression@266
    │     │ ├─LiteralExpression@266
    │     │ │ └─CharLiteral('C')@266
    │     │ ├─Dot@269
    │     │ └─PathSegment@270
    │     │   └─Identifier(println)@270
    │     ├─LParen@277
    │     └─RParen@278
    ├─Newline@279
    └─RBrace@280
    """.trimIndent(),
        """
            import {
                foo.a               // function
                bar: { a, b, c }    // val, var, package
            }
            
            def useAll() = a(a + b) - c.d
        """.trimIndent() to """
File@0
├─Import@0
│ ├─ImportKw@0
│ ├─Whitespace( )@6
│ └─ImportGroup@7
│   ├─LBrace@7
│   ├─Newline@8
│   ├─Whitespace(    )@9
│   ├─ImportSegment@13
│   │ └─ImportPathAccess@13
│   │   ├─Identifier(foo)@13
│   │   ├─Dot@16
│   │   └─Identifier(a)@17
│   ├─Whitespace(               )@18
│   ├─Comment(// function)@33
│   ├─Newline@44
│   ├─Whitespace(    )@45
│   ├─ImportSegment@49
│   │ ├─Identifier(bar)@49
│   │ ├─Colon@52
│   │ ├─Whitespace( )@53
│   │ └─ImportGroup@54
│   │   ├─LBrace@54
│   │   ├─Whitespace( )@55
│   │   ├─ImportSegment@56
│   │   │ └─Identifier(a)@56
│   │   ├─Comma@57
│   │   ├─Whitespace( )@58
│   │   ├─ImportSegment@59
│   │   │ └─Identifier(b)@59
│   │   ├─Comma@60
│   │   ├─Whitespace( )@61
│   │   ├─ImportSegment@62
│   │   │ └─Identifier(c)@62
│   │   ├─Whitespace( )@63
│   │   └─RBrace@64
│   ├─Whitespace(    )@65
│   ├─Comment(// val, var, package)@69
│   ├─Newline@89
│   └─RBrace@90
├─Newline@91
├─Newline@92
└─FunctionDef@93
  ├─DefKw@93
  ├─Whitespace( )@96
  ├─Identifier(useAll)@97
  ├─FunctionParams@103
  │ ├─LParen@103
  │ └─RParen@104
  ├─Whitespace( )@105
  └─ExpressionBody@106
    ├─Eq@106
    ├─Whitespace( )@107
    └─PlusExpression@108
      ├─CallExpression@108
      │ ├─IdentifierExpression@108
      │ │ └─PathSegment@108
      │ │   └─Identifier(a)@108
      │ ├─LParen@109
      │ ├─PlusExpression@110
      │ │ ├─IdentifierExpression@110
      │ │ │ └─PathSegment@110
      │ │ │   └─Identifier(a)@110
      │ │ ├─Whitespace( )@111
      │ │ ├─Plus@112
      │ │ ├─Whitespace( )@113
      │ │ └─IdentifierExpression@114
      │ │   └─PathSegment@114
      │ │     └─Identifier(b)@114
      │ └─RParen@115
      ├─Whitespace( )@116
      ├─Minus@117
      ├─Whitespace( )@118
      └─AccessExpression@119
        ├─IdentifierExpression@119
        │ └─PathSegment@119
        │   └─Identifier(c)@119
        ├─Dot@120
        └─PathSegment@121
          └─Identifier(d)@121
        """.trimIndent(),
        "def func[A where A <: Int32](a: A, b: Int32): Int32 = a + b" to """
File@0
└─FunctionDef@0
  ├─DefKw@0
  ├─Whitespace( )@3
  ├─Identifier(func)@4
  ├─TypeParams@8
  │ ├─LBracket@8
  │ ├─Identifier(A)@9
  │ ├─Whitespace( )@10
  │ ├─TypeGuard@11
  │ │ ├─WhereKw@11
  │ │ ├─Whitespace( )@16
  │ │ └─TypeBound@17
  │ │   ├─Identifier(A)@17
  │ │   ├─Whitespace( )@18
  │ │   ├─Subtype@19
  │ │   ├─Whitespace( )@21
  │ │   └─PathType@22
  │ │     └─PathSegment@22
  │ │       └─Identifier(Int32)@22
  │ └─RBracket@27
  ├─FunctionParams@28
  │ ├─LParen@28
  │ ├─FunctionParam@29
  │ │ ├─Identifier(a)@29
  │ │ └─TypeAnnotation@30
  │ │   ├─Colon@30
  │ │   ├─Whitespace( )@31
  │ │   └─PathType@32
  │ │     └─PathSegment@32
  │ │       └─Identifier(A)@32
  │ ├─Comma@33
  │ ├─Whitespace( )@34
  │ ├─FunctionParam@35
  │ │ ├─Identifier(b)@35
  │ │ └─TypeAnnotation@36
  │ │   ├─Colon@36
  │ │   ├─Whitespace( )@37
  │ │   └─PathType@38
  │ │     └─PathSegment@38
  │ │       └─Identifier(Int32)@38
  │ └─RParen@43
  ├─TypeAnnotation@44
  │ ├─Colon@44
  │ ├─Whitespace( )@45
  │ └─PathType@46
  │   └─PathSegment@46
  │     └─Identifier(Int32)@46
  ├─Whitespace( )@51
  └─ExpressionBody@52
    ├─Eq@52
    ├─Whitespace( )@53
    └─PlusExpression@54
      ├─IdentifierExpression@54
      │ └─PathSegment@54
      │   └─Identifier(a)@54
      ├─Whitespace( )@55
      ├─Plus@56
      ├─Whitespace( )@57
      └─IdentifierExpression@58
        └─PathSegment@58
          └─Identifier(b)@58
        """.trimIndent()
    )

    @Test
    fun testParser() {
        assertAll(codeExpectedMap.map { (code, expected) ->
            {
                val parser = Parser(PistonLexer(code), PistonType.file)

                val green = PistonParsing.parseFile(parser)
                val red = RedNode(null, green, 0)
                val builder = StringBuilder()
                red.format(builder, "")

                val res = builder.toString()

                assertEquals(expected, res)
            }
        })
    }
}