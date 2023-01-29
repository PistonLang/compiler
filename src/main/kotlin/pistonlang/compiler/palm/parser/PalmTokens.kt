package pistonlang.compiler.palm.parser

import pistonlang.compiler.common.parser.SyntaxToken

typealias PalmToken = SyntaxToken<PalmType>

object Tokens {
    val thisKw = SyntaxToken(PalmType.thisKw, "this")
    val superKw = SyntaxToken(PalmType.superKw, "super")
    val nullKw = SyntaxToken(PalmType.nullKw, "null")
    val whereKw = SyntaxToken(PalmType.whereKw, "where")
    val importKw = SyntaxToken(PalmType.importKw, "import")
    val getKw = SyntaxToken(PalmType.getKw, "get")
    val setKw = SyntaxToken(PalmType.setKw, "set")
    val valKw = SyntaxToken(PalmType.valKw, "val")
    val varKw = SyntaxToken(PalmType.varKw, "var")
    val defKw = SyntaxToken(PalmType.defKw, "def")
    val classKw = SyntaxToken(PalmType.classKw, "class")
    val traitKw = SyntaxToken(PalmType.traitKw, "trait")
    val trueKw = SyntaxToken(PalmType.trueKw, "true")
    val falseKw = SyntaxToken(PalmType.falseKw, "false")

    val newline = SyntaxToken(PalmType.newline, "\n")
    val eof = SyntaxToken(PalmType.eof, "")

    val eq = SyntaxToken(PalmType.eq, "=")
    val eqEq = SyntaxToken(PalmType.eqEq, "==")
    val eMark = SyntaxToken(PalmType.unknown, "!")
    val eMarkEq = SyntaxToken(PalmType.eMarkEq, "!=")
    val less = SyntaxToken(PalmType.less, "<")
    val greater = SyntaxToken(PalmType.greater, ">")
    val lessEq = SyntaxToken(PalmType.lessEq, "<=")
    val greaterEq = SyntaxToken(PalmType.greaterEq, ">=")
    val andAnd = SyntaxToken(PalmType.andAnd, "&&")
    val orOr = SyntaxToken(PalmType.orOr, "||")
    val plus = SyntaxToken(PalmType.plus, "+")
    val minus = SyntaxToken(PalmType.minus, "-")
    val star = SyntaxToken(PalmType.star, "*")
    val slash = SyntaxToken(PalmType.slash, "/")

    val dot = SyntaxToken(PalmType.dot, ".")
    val comma = SyntaxToken(PalmType.comma, ",")
    val qMark = SyntaxToken(PalmType.qMark, "?")
    val colon = SyntaxToken(PalmType.colon, ":")

    val lParen = SyntaxToken(PalmType.lParen, "(")
    val rParen = SyntaxToken(PalmType.rParen, ")")
    val lBracket = SyntaxToken(PalmType.lBracket, "[")
    val rBracket = SyntaxToken(PalmType.rBracket, "]")
    val lBrace = SyntaxToken(PalmType.lBrace, "{")
    val rBrace = SyntaxToken(PalmType.rBrace, "}")

    val arrow = SyntaxToken(PalmType.arrow, "->")
    val or = SyntaxToken(PalmType.unknown, "|")
    val and = SyntaxToken(PalmType.and, "&")
    val subtype = SyntaxToken(PalmType.subtype, "<:")
    val superType = SyntaxToken(PalmType.superType, ">:")

    val zero = SyntaxToken(PalmType.intLiteral, "0")

    val nullChar = SyntaxToken(PalmType.unknown, "\u0000")
    val singleQuote = SyntaxToken(PalmType.charLiteral, "\'")
}