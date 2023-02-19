package pistonlang.compiler.common.parser

class Parser<T : SyntaxType>(lexer: Lexer<T>, resultType: T) {
    private val stream = TokenStream(0, lexer)
    @PublishedApi
    internal val nodeStack = mutableListOf(MutableSyntaxNode(resultType))
    private var buffer = buildSegment()

    fun nextToken() = stream.current

    fun at(type: T): Boolean = currType == type

    val currType get() = buffer.last().type

    private inline fun useBuffer(crossinline fn: (buf: MutableList<SyntaxChild<T>>) -> Unit) {
        addTrailing(buffer.textLength)
        fn(buffer)
        buffer = buildSegment()
    }

    private tailrec fun buildSegment(buf: MutableList<SyntaxChild<T>>, offset: Int) {
        val curr = stream.current
        buf.add(SyntaxChild(offset, curr))
        stream.move()
        if (curr.type.ignorable) buildSegment(buf, offset + curr.length)
    }

    private fun buildSegment(): MutableList<SyntaxChild<T>> {
        val res = mutableListOf<SyntaxChild<T>>()
        buildSegment(res, 0)
        return res
    }

    private tailrec fun addTrailing(offset: Int) {
        val curr = stream.current
        if (curr.type.run { ignorable && !isNewline }) {
            buffer.add(SyntaxChild(offset, curr))
            stream.move()
            addTrailing(offset + curr.length)
        }
    }

    fun nest(type: T) {
        val top = nodeStack.last()
        val newNode = MutableSyntaxNode(type)
        newNode.add(top.toImmutable())
        nodeStack[nodeStack.size] = newNode
    }

    fun pushToNode(type: T) = useBuffer { buf ->
        nodeStack.last().add(SyntaxNode(type, buf, buf.textLength))
    }

    inline fun createNode(type: T, crossinline fn: () -> Unit) {
        val node = MutableSyntaxNode(type)
        nodeStack.add(node)
        fn()
        nodeStack.removeLast()
        nodeStack.last().add(node.toImmutable())
    }

    inline fun tryCreateNode(type: T, crossinline succeeded: () -> Boolean): Boolean {
        val node = MutableSyntaxNode(type)
        nodeStack.add(node)
        val success = succeeded()
        nodeStack.removeLast()
        if (success) nodeStack.last().add(node.toImmutable())
        return success
    }

    fun push() = useBuffer { buf ->
        val node = nodeStack.last()
        buf.forEach { node.add(it.value) }
    }

    fun finish(): Syntax<T> = when (nodeStack.size) {
        1 -> nodeStack.first().toImmutable()
        0 -> error("Node stack is empty")
        else -> error("Node stack has more than 1 element")
    }

    val startWithNewline get(): Boolean = buffer.isNotEmpty() && buffer.first().value.type.isNewline
}

@PublishedApi
internal class MutableSyntaxNode<T : SyntaxType>(private val type: T) {
    private val children = mutableListOf<SyntaxChild<T>>()

    fun add(node: Syntax<T>) {
        children.add(SyntaxChild(children.textLength, node))
    }
    fun toImmutable() = SyntaxNode(type, children, children.textLength)
}