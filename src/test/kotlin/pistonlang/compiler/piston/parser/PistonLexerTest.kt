package pistonlang.compiler.piston.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pistonlang.compiler.common.parser.TokenIterator

class PistonLexerTest {
    val code = """
            def test[T](a: Int, b: Int, c: T) = println((a + b).toString() + c)
        """.trimIndent()

    val expected =
        "[defKw, whitespace, identifier, lBracket, identifier, rBracket, lParen, identifier, colon, whitespace, identifier, comma, whitespace, identifier, colon, whitespace, identifier, comma, whitespace, identifier, colon, whitespace, identifier, rParen, whitespace, eq, whitespace, identifier, lParen, lParen, identifier, whitespace, plus, whitespace, identifier, rParen, dot, identifier, lParen, rParen, whitespace, plus, whitespace, identifier, rParen]"

    @Test
    fun testLexer() {
        val lexer = TokenIterator(PistonLexer(code))

        val list = lexer.asSequence().map { it.type }.toList()

        assertEquals(expected, list.toString())
    }
}