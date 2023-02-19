package pistonlang.compiler.common.parser

interface Lexer<T: SyntaxType> {
    fun lexToken(pos: Int): SyntaxToken<T>

    fun ended(pos: Int): Boolean
}

class TokenStream<T: SyntaxType>(startPos: Int, private val lexer: Lexer<T>): Iterable<Syntax<T>> {
    var current = lexer.lexToken(startPos)
        private set

    private var pos = current.length

    fun move() {
        current = lexer.lexToken(0)
        pos += current.length
    }

    override fun iterator() = object : Iterator<Syntax<T>> {
        override fun hasNext() = lexer.ended(pos)
        override fun next(): Syntax<T> = current.also { move() }
    }
}