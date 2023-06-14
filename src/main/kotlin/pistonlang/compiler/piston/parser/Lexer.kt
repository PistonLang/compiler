package pistonlang.compiler.piston.parser

import pistonlang.compiler.common.parser.Lexer
import pistonlang.compiler.common.parser.GreenLeaf

const val eof = '\u0000'

class PistonLexer(private val code: String) : Lexer<PistonType> {
    private fun getChar(pos: Int): Char = if (pos < code.length) code[pos] else eof

    private fun ended(pos: Int) = pos >= code.length

    override fun lexToken(pos: Int): PistonToken = when (val char = getChar(pos)) {
        eof -> if (ended(pos)) Tokens.eof else Tokens.nullChar
        '+' -> Tokens.plus
        '-' -> Tokens.minus
        '*' -> Tokens.star
        '/' -> lexSlash(pos + 1)
        '<' -> lexLess(pos + 1)
        '>' -> lexGreater(pos + 1)
        '=' -> lexEq(pos + 1)
        '!' -> lexEMark(pos + 1)
        '&' -> lexAnd(pos + 1)
        '|' -> lexPipe(pos + 1)
        '.' -> Tokens.dot
        ',' -> Tokens.comma
        ':' -> Tokens.colon
        '?' -> Tokens.qMark
        '(' -> Tokens.lParen
        ')' -> Tokens.rParen
        '[' -> Tokens.lBracket
        ']' -> Tokens.rBracket
        '{' -> Tokens.lBrace
        '}' -> Tokens.rBrace
        '\'' -> lexChar(pos + 1)
        '\"' -> lexString(pos, pos + 1)
        '0' -> lexZero(pos + 1)
        in '1'..'9' -> lexDecimal(pos, pos + 1)
        '\n' -> Tokens.newline
        ' ', '\t', '\u000B', '\u000C', '\u000D' -> lexWhitespace(pos, pos + 1)
        in 'a'..'z', in 'A'..'Z' -> lexIdentifier(pos, pos + 1)
        '_' -> lexIdentifier(pos, pos + 1)
        else ->
            if (char.isLetter()) lexIdentifier(pos, pos + 1)
            else GreenLeaf(PistonType.unknown, char.toString())
    }

    private fun lexZero(pos: Int) = when (getChar(pos)) {
        'b', 'B' -> lexBinary(pos - 1, pos + 1)
        'x', 'X' -> lexHex(pos - 1, pos + 1)
        else -> lexDecimal(pos - 1, pos)
    }

    private fun lexPipe(pos: Int) = when (getChar(pos)) {
        '|' -> Tokens.orOr
        else -> Tokens.or
    }

    private fun lexAnd(pos: Int) = when (getChar(pos)) {
        '&' -> Tokens.andAnd
        else -> Tokens.and
    }

    private fun lexEMark(pos: Int) = when (getChar(pos)) {
        '=' -> Tokens.eMarkEq
        else -> Tokens.eMark
    }

    private fun lexEq(pos: Int) = when (getChar(pos)) {
        '=' -> Tokens.eqEq
        else -> Tokens.eq
    }

    private fun lexGreater(pos: Int) = when (getChar(pos)) {
        '=' -> Tokens.greaterEq
        ':' -> Tokens.superType
        else -> Tokens.greater
    }

    private fun lexLess(pos: Int) = when (getChar(pos)) {
        '=' -> Tokens.lessEq
        ':' -> Tokens.subtype
        else -> Tokens.less
    }

    private fun lexSlash(pos: Int) = when (getChar(pos)) {
        '/' -> lexComment(pos - 1, pos + 1)
        '*' -> lexMultiComment(pos - 1, pos + 1, 0)
        else -> Tokens.slash
    }

    private fun tokenTill(start: Int, end: Int, type: PistonType): PistonToken =
        GreenLeaf(type, code.substring(start, end))

    private tailrec fun lexComment(start: Int, pos: Int): PistonToken = when (getChar(pos)) {
        '\n' -> tokenTill(start, pos, PistonType.comment)
        eof -> if (ended(pos)) tokenTill(start, pos, PistonType.comment) else lexComment(start, pos + 1)
        else -> lexComment(start, pos + 1)
    }

    private tailrec fun lexMultiComment(start: Int, pos: Int, layer: Int): PistonToken = when {
        ended(pos) -> tokenTill(start, pos, PistonType.comment)

        getChar(pos) == '*' && getChar(pos + 1) == '/' ->
            if (layer == 0) tokenTill(start, pos + 1, PistonType.comment)
            else lexMultiComment(start, pos + 1, layer - 1)

        getChar(pos) == '/' && getChar(pos + 1) == '*' ->
            lexMultiComment(start, pos + 1, layer + 1)

        else -> lexMultiComment(start, pos + 1, layer)
    }

    private tailrec fun lexBinary(start: Int, pos: Int): PistonToken = when (getChar(pos)) {
        '0', '1' -> lexBinary(start, pos + 1)
        else -> tokenTill(start, pos, PistonType.intLiteral)
    }

    private tailrec fun lexHex(start: Int, pos: Int): PistonToken = when (getChar(pos)) {
        in '0'..'9', in 'A'..'F', in 'a'..'f' -> lexHex(start, pos + 1)
        else -> tokenTill(start, pos, PistonType.intLiteral)
    }

    private tailrec fun lexFloating(start: Int, pos: Int): PistonToken = when (getChar(pos)) {
        in '0'..'9' -> lexFloating(start, pos + 1)

        'e', 'E' -> when (getChar(pos + 1)) {
            '+', '-' -> lexExponent(start, pos + 2)
            else -> lexExponent(start, pos + 1)
        }

        else -> tokenTill(start, pos, PistonType.floatLiteral)
    }

    private tailrec fun lexExponent(start: Int, pos: Int): PistonToken = when (getChar(pos)) {
        in '0'..'9' -> lexExponent(start, pos + 1)
        else -> tokenTill(start, pos, PistonType.floatLiteral)
    }

    private tailrec fun lexDecimal(start: Int, pos: Int): PistonToken = when (getChar(pos)) {
        in '0'..'9' -> lexDecimal(start, pos + 1)

        '.' ->
            if (getChar(pos + 1) in '0'..'9') lexFloating(start, pos + 1)
            else tokenTill(start, pos, PistonType.intLiteral)

        else -> tokenTill(start, pos, PistonType.intLiteral)
    }

    private tailrec fun lexWhitespace(start: Int, pos: Int): PistonToken = when (getChar(pos)) {
        ' ', '\t', '\u000B', '\u000C', '\u000D' -> lexWhitespace(start, pos + 1)
        else -> tokenTill(start, pos, PistonType.whitespace)
    }

    private tailrec fun lexIdentifier(start: Int, pos: Int): PistonToken = when (getChar(pos)) {
        '_' ->
            if (getChar(pos + 1) == '=') handleIdentifier(code.substring(start, pos + 2))
            else lexIdentifier(start, pos + 1)
        in AlphaGroup -> lexIdentifier(start, pos + 1)
        else -> handleIdentifier(code.substring(start, pos))
    }

    private fun lexChar(pos: Int) = when {
        getChar(pos + 1) == '\'' -> GreenLeaf(PistonType.charLiteral, code.substring(pos - 1, pos + 2))
        else -> when (val curr = getChar(pos)) {
            '\n', eof -> Tokens.singleQuote
            else ->
                if (curr.isWhitespace()) GreenLeaf(PistonType.charLiteral, code.substring(pos - 1, pos + 1))
                else lexLongChar(pos - 1, pos)
        }
    }

    private fun lexLongChar(start: Int, pos: Int): PistonToken = when (val curr = getChar(pos)) {
        '\\' -> lexLongChar(start, pos + if (getChar(pos + 1) == '\'') 2 else 1)
        '\'' -> GreenLeaf(PistonType.charLiteral, code.substring(start, pos + 1))
        else ->
            if (!curr.isWhitespace()) GreenLeaf(PistonType.charLiteral, code.substring(start, pos))
            else lexLongChar(start, pos + 1)
    }

    private tailrec fun lexString(start: Int, pos: Int): PistonToken = when (getChar(pos)) {
        '\"' -> GreenLeaf(PistonType.stringLiteral, code.substring(start, pos + 1))
        '\\' -> when (getChar(pos + 1)) {
            '\\', '\"' -> lexString(start, pos + 2)
            else -> lexString(start, pos + 1)
        }

        eof ->
            if (ended(pos)) GreenLeaf(PistonType.stringLiteral, code.substring(start, pos))
            else lexString(start, pos + 1)

        else -> lexString(start, pos + 1)
    }

    private fun handleIdentifier(ident: String): PistonToken = when (ident) {
        "this" -> Tokens.thisKw
        "val" -> Tokens.valKw
        "var" -> Tokens.varKw
        "super" -> Tokens.superKw
        "import" -> Tokens.importKw
        "class" -> Tokens.classKw
        "trait" -> Tokens.traitKw
        "def" -> Tokens.defKw
        "where" -> Tokens.whereKw
        "null" -> Tokens.nullKw
        "true" -> Tokens.trueKw
        "false" -> Tokens.falseKw
        else -> GreenLeaf(PistonType.identifier, ident)
    }
}

internal object AlphaGroup {
    operator fun contains(char: Char) = when (char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', '\'' -> true
        else -> char.isLetterOrDigit()
    }
}