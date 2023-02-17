package pistonlang.compiler.piston.parser

import pistonlang.compiler.common.parser.SyntaxToken

typealias PistonToken = SyntaxToken<PistonType>

object Tokens {
    val thisKw = SyntaxToken(PistonType.thisKw, "this")
    val superKw = SyntaxToken(PistonType.superKw, "super")
    val nullKw = SyntaxToken(PistonType.nullKw, "null")
    val whereKw = SyntaxToken(PistonType.whereKw, "where")
    val importKw = SyntaxToken(PistonType.importKw, "import")
    val valKw = SyntaxToken(PistonType.valKw, "val")
    val varKw = SyntaxToken(PistonType.varKw, "var")
    val defKw = SyntaxToken(PistonType.defKw, "def")
    val classKw = SyntaxToken(PistonType.classKw, "class")
    val traitKw = SyntaxToken(PistonType.traitKw, "trait")
    val trueKw = SyntaxToken(PistonType.trueKw, "true")
    val falseKw = SyntaxToken(PistonType.falseKw, "false")

    val newline = SyntaxToken(PistonType.newline, "\n")
    val eof = SyntaxToken(PistonType.eof, "")

    val eq = SyntaxToken(PistonType.eq, "=")
    val eqEq = SyntaxToken(PistonType.eqEq, "==")
    val eMark = SyntaxToken(PistonType.unknown, "!")
    val eMarkEq = SyntaxToken(PistonType.eMarkEq, "!=")
    val less = SyntaxToken(PistonType.less, "<")
    val greater = SyntaxToken(PistonType.greater, ">")
    val lessEq = SyntaxToken(PistonType.lessEq, "<=")
    val greaterEq = SyntaxToken(PistonType.greaterEq, ">=")
    val andAnd = SyntaxToken(PistonType.andAnd, "&&")
    val orOr = SyntaxToken(PistonType.orOr, "||")
    val plus = SyntaxToken(PistonType.plus, "+")
    val minus = SyntaxToken(PistonType.minus, "-")
    val star = SyntaxToken(PistonType.star, "*")
    val slash = SyntaxToken(PistonType.slash, "/")

    val dot = SyntaxToken(PistonType.dot, ".")
    val comma = SyntaxToken(PistonType.comma, ",")
    val qMark = SyntaxToken(PistonType.qMark, "?")
    val colon = SyntaxToken(PistonType.colon, ":")

    val lParen = SyntaxToken(PistonType.lParen, "(")
    val rParen = SyntaxToken(PistonType.rParen, ")")
    val lBracket = SyntaxToken(PistonType.lBracket, "[")
    val rBracket = SyntaxToken(PistonType.rBracket, "]")
    val lBrace = SyntaxToken(PistonType.lBrace, "{")
    val rBrace = SyntaxToken(PistonType.rBrace, "}")

    val or = SyntaxToken(PistonType.unknown, "|")
    val and = SyntaxToken(PistonType.and, "&")
    val subtype = SyntaxToken(PistonType.subtype, "<:")
    val superType = SyntaxToken(PistonType.superType, ">:")
    
    val nullChar = SyntaxToken(PistonType.unknown, "\u0000")
    val singleQuote = SyntaxToken(PistonType.charLiteral, "\'")
}