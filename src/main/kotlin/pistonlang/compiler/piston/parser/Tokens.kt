package pistonlang.compiler.piston.parser

import pistonlang.compiler.common.parser.GreenLeaf

typealias PistonToken = GreenLeaf<PistonType>

object Tokens {
    val thisKw = GreenLeaf(PistonType.thisKw, "this")
    val superKw = GreenLeaf(PistonType.superKw, "super")
    val nullKw = GreenLeaf(PistonType.nullKw, "null")
    val whereKw = GreenLeaf(PistonType.whereKw, "where")
    val importKw = GreenLeaf(PistonType.importKw, "import")
    val valKw = GreenLeaf(PistonType.valKw, "val")
    val varKw = GreenLeaf(PistonType.varKw, "var")
    val defKw = GreenLeaf(PistonType.defKw, "def")
    val classKw = GreenLeaf(PistonType.classKw, "class")
    val traitKw = GreenLeaf(PistonType.traitKw, "trait")
    val trueKw = GreenLeaf(PistonType.trueKw, "true")
    val falseKw = GreenLeaf(PistonType.falseKw, "false")

    val newline = GreenLeaf(PistonType.newline, "\n")
    val eof = GreenLeaf(PistonType.eof, "")

    val eq = GreenLeaf(PistonType.eq, "=")
    val eqEq = GreenLeaf(PistonType.eqEq, "==")
    val eMark = GreenLeaf(PistonType.unknown, "!")
    val eMarkEq = GreenLeaf(PistonType.eMarkEq, "!=")
    val less = GreenLeaf(PistonType.less, "<")
    val greater = GreenLeaf(PistonType.greater, ">")
    val lessEq = GreenLeaf(PistonType.lessEq, "<=")
    val greaterEq = GreenLeaf(PistonType.greaterEq, ">=")
    val andAnd = GreenLeaf(PistonType.andAnd, "&&")
    val orOr = GreenLeaf(PistonType.orOr, "||")
    val plus = GreenLeaf(PistonType.plus, "+")
    val minus = GreenLeaf(PistonType.minus, "-")
    val star = GreenLeaf(PistonType.star, "*")
    val slash = GreenLeaf(PistonType.slash, "/")

    val dot = GreenLeaf(PistonType.dot, ".")
    val comma = GreenLeaf(PistonType.comma, ",")
    val qMark = GreenLeaf(PistonType.qMark, "?")
    val colon = GreenLeaf(PistonType.colon, ":")

    val lParen = GreenLeaf(PistonType.lParen, "(")
    val rParen = GreenLeaf(PistonType.rParen, ")")
    val lBracket = GreenLeaf(PistonType.lBracket, "[")
    val rBracket = GreenLeaf(PistonType.rBracket, "]")
    val lBrace = GreenLeaf(PistonType.lBrace, "{")
    val rBrace = GreenLeaf(PistonType.rBrace, "}")

    val or = GreenLeaf(PistonType.unknown, "|")
    val and = GreenLeaf(PistonType.and, "&")
    val subtype = GreenLeaf(PistonType.subtype, "<:")
    val superType = GreenLeaf(PistonType.supertype, ">:")
    
    val nullChar = GreenLeaf(PistonType.unknown, "\u0000")
    val singleQuote = GreenLeaf(PistonType.charLiteral, "\'")
}