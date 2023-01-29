package pistonlang.compiler.common.parser

interface Lexer<T: SyntaxType> {
    fun lexToken(pos: Int): SyntaxToken<T>

    fun ended(pos: Int): Boolean
}

class TokenStream<T: SyntaxType>(private val lexer: Lexer<T>): Iterator<SyntaxToken<T>> {
    private var pos = 0

    override fun hasNext(): Boolean = !lexer.ended(pos)

    override fun next(): SyntaxToken<T> = lexer.lexToken(pos).also { pos += it.length }
}