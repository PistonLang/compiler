package pistonlang.compiler.common.parser

interface Lexer<T: SyntaxType> {
    fun lexToken(pos: Int): GreenLeaf<T>
}

class TokenStream<T: SyntaxType>(startPos: Int, private val lexer: Lexer<T>): Iterable<GreenNode<T>> {
    var current = lexer.lexToken(startPos)
        private set

    private var pos = current.length

    fun move() {
        current = lexer.lexToken(pos)
        pos += current.length
    }

    override fun iterator() = object : Iterator<GreenNode<T>> {
        override fun hasNext() = !current.type.isEOF
        override fun next(): GreenNode<T> = current.also { move() }
    }
}