package pistonlang.compiler.common.parser

import pistonlang.compiler.common.utl.BufferList

class Parser<T: SyntaxType>(lexer: Lexer<T>) {
    private val stream = TokenStream(lexer)
    private val buffer = BufferList<Syntax<T>>()
    private var currentIndex = -1
    private var trailingIndex = -1
    var sawNewline = false
        private set

    fun top() = buffer[currentIndex]

    private fun handleForward(trailing: Boolean): Boolean {
        currentIndex++
        val top = top().type
        if (top.isNewline) sawNewline = true
        if (!trailing && top.trailing) {
            trailingIndex = currentIndex
            return true
        }
        return trailing
    }

    private tailrec fun forwardAdd(trailing: Boolean): Syntax<T> {
        val newTrailing = handleForward(trailing)
        val top = top()
        return if (top.type.ignorable) forwardAdd(newTrailing) else top
    }

    private tailrec fun forwardMove(trailing: Boolean): Syntax<T> = if (currentIndex == buffer.size) forwardAdd(trailing) else {
        val newTrailing = handleForward(trailing)
        val top = top()
        if (top.type.ignorable) forwardMove(newTrailing) else top
    }

    fun forward() {
        sawNewline = false
        forwardMove(false)
    }

    fun next(): Syntax<T> = if (currentIndex < buffer.lastIndex) {
        buffer[currentIndex + 1]
    } else {
        stream.next().also { buffer.add(it) }
    }

    fun mark() = ParseMarker(trailingIndex)

    private fun finishNode(type: T, start: Int, end: Int) {
        val list = mutableListOf<SyntaxChild<T>>()
        var len = 0

        for (index in start until end) {
            val curr = buffer[index]
            list += SyntaxChild(len, curr)
            len += curr.length
        }

        val res = SyntaxNode(type, list, len)
        buffer[start] = res
        currentIndex -= trailingIndex - start + 1
        buffer.removeRange(start + 1, trailingIndex)
    }

    fun between(type: T, start: ParseMarker, end: ParseMarker) = finishNode(type, start.index, end.index)

    fun end(type: T, start: ParseMarker) = finishNode(type, start.index, trailingIndex)
}

data class ParseMarker(internal val index: Int)