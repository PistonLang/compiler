package pistonlang.compiler.common.parser

import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.nodes.*

class Parser<T : SyntaxType>(lexer: Lexer<T>, resultType: T, startPos: Int = 0) {
    private val stream = TokenStream(lexer, startPos)

    @PublishedApi
    internal val nodeStack = mutableListOf(MutableSyntaxNode(resultType))
    private var buffer = buildSegment()

    fun nextToken() = stream.current

    fun at(type: T): Boolean = currType == type

    val currType get() = buffer.last().type

    private inline fun useBuffer(crossinline fn: (buf: MutableList<GreenChild<T>>) -> Unit) {
        addTrailing(buffer.textLength)
        fn(buffer)
        buffer = buildSegment()
    }

    private tailrec fun buildSegment(buf: MutableList<GreenChild<T>>, offset: Int) {
        val curr = stream.current
        buf.add(GreenChild(offset, curr))
        stream.move()
        if (curr.type.ignorable) buildSegment(buf, offset + curr.length)
    }

    private fun buildSegment(): MutableList<GreenChild<T>> {
        val res = mutableListOf<GreenChild<T>>()
        buildSegment(res, 0)
        return res
    }

    private tailrec fun addTrailing(offset: Int) {
        val curr = stream.current
        if (curr.type.run { ignorable && !isNewline }) {
            buffer.add(GreenChild(offset, curr))
            stream.move()
            addTrailing(offset + curr.length)
        }
    }

    fun pushToNode(type: T) = useBuffer { buf ->
        nodeStack.last().push(GreenBranch(type, buf, buf.textLength))
    }

    inline fun createNode(type: T, crossinline fn: () -> Unit): Boolean {
        val node = MutableSyntaxNode(type)
        nodeStack.add(node)
        fn()
        nodeStack.removeLast()
        return node.valid().also { valid -> if (valid) nodeStack.last().push(node.toImmutable()) }
    }

    inline fun nestLast(type: T, crossinline fn: () -> Unit) {
        val node = MutableSyntaxNode(type)
        val last = nodeStack.last().pop()
        nodeStack.add(node)
        node.push(last)
        fn()
        nodeStack.removeLast()
        nodeStack.last().push(node.toImmutable())
    }

    fun push() = useBuffer { buf ->
        val node = nodeStack.last()
        buf.forEach { node.push(it.value) }
    }

    fun finish(): GreenNode<T> = when (nodeStack.size) {
        1 -> nodeStack.first().toImmutable()
        0 -> error("Node stack is empty")
        else -> error("Node stack has more than 1 element")
    }

    val startWithNewline get(): Boolean = buffer.isNotEmpty() && buffer.first().value.type.isNewline
}

@PublishedApi
internal class MutableSyntaxNode<T : SyntaxType>(private val type: T) {
    private val children = mutableListOf<GreenChild<T>>()

    fun push(node: GreenNode<T>) {
        children.add(GreenChild(children.textLength, node))
    }

    fun pop(): GreenNode<T> {
        val res = children.last().value
        children.removeLast()
        return res
    }

    fun valid(): Boolean = children.isNotEmpty()

    fun toImmutable() = GreenBranch(type, children, children.textLength)
}