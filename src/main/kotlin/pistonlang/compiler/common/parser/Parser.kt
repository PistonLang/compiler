package pistonlang.compiler.common.parser

import pistonlang.compiler.common.utl.BufferList

class Parser<T: SyntaxType>(lexer: Lexer<T>) {
    private val stream = TokenStream(lexer)
    private val buffer = BufferList<Syntax<T>>()
    private var trailingIndex = 0
    var sawNewline = false
        private set

    fun top() = buffer.last()

    private fun readToken(): Syntax<T> {
        val curr = stream.next()
        buffer.add(curr)
        return curr
    }

    fun bump(): Syntax<T> {
        sawNewline = false
        return bump(true)
    }

    private fun bump(findStart: Boolean): Syntax<T> = when {
        stream.hasNext() -> top()

        readToken().type.ignorable -> {
            val trailing = top().type.trailing
            if (findStart && trailing) trailingIndex = buffer.lastIndex
            if (top().type.isNewline) sawNewline = true
            bump(findStart && !trailing)
        }

        else -> {
            if (findStart) trailingIndex = buffer.lastIndex
            top()
        }
    }

    fun nest(type: T) {
        buffer[buffer.lastIndex] = SyntaxNode(type, listOf(SyntaxChild(0, buffer.last())), buffer.last().length)
    }

    fun mark() = ParseMarker(trailingIndex)

    fun end(type: T, data: ParseMarker) {
        val list = mutableListOf<SyntaxChild<T>>()
        var len = 0

        for (index in data.startIndex..buffer.lastIndex) {
            val curr = buffer[index]
            list += SyntaxChild(len, curr)
            len += curr.length
        }

        buffer.removeRange(data.startIndex + 1, trailingIndex)

        val res = SyntaxNode(type, list, len)
        buffer.add(res)
    }
}

@JvmInline
value class ParseMarker(val startIndex: Int)