package pistonlang.compiler.palm.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pistonlang.compiler.common.parser.TokenStream

internal class PalmLexerTest {
    @Test
    fun testLexer() {
        val code = """
            def test[T](a: Int, b: Int, c: T) = println((a + b).toString() + c)
        """.trimIndent()

        val expected = "[defKw, whitespace, identifier, lBracket, identifier, rBracket, lParen, identifier, colon, whitespace, identifier, comma, whitespace, identifier, colon, whitespace, identifier, comma, whitespace, identifier, colon, whitespace, identifier, rParen, whitespace, eq, whitespace, identifier, lParen, lParen, identifier, whitespace, plus, whitespace, identifier, rParen, dot, identifier, lParen, rParen, whitespace, plus, whitespace, identifier, rParen]"

        val lexer = TokenStream(PalmLexer(code))

        val list = lexer.asSequence().map { it.type }.toList()

        assertEquals(expected, list.toString())
    }
}