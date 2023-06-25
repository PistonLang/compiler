package pistonlang.compiler.common.parser

import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.nodes.*

class Parser<Type : SyntaxType>(lexer: Lexer<Type>, resultType: Type, startPos: Int = 0) {
    private val stream = TokenStream(lexer, startPos)

    @PublishedApi
    internal val nodeStack = mutableListOf(MutableSyntaxNode(resultType))
    private val buffer = ArrayDeque<GreenNode<Type>>()
    private var sawNewline = false

    init {
        fillBuffer()
    }

    fun nextToken() = stream.current

    fun at(type: Type): Boolean = currType == type

    val currType get() = buffer.last().type

    private tailrec fun fillBuffer() {
        val curr = stream.current
        stream.move()
        buffer.addLast(curr)
        if (curr.type.isNewline) sawNewline = true
        if (curr.type.ignorable) fillBuffer()
    }

    @PublishedApi
    internal fun pushWhitespace() {
        val node = nodeStack.last()
        while (buffer.first().type.ignorable) {
            node.push(buffer.removeFirst())
        }
        sawNewline = false
    }

    fun pushToNode(type: Type) {
        pushWhitespace()
        val child = buffer.removeLast()
        nodeStack.last().push(GreenBranch(type, listOf(GreenChild(0, child)), child.length))
        fillBuffer()
    }

    inline fun createNode(type: Type, crossinline fn: () -> Unit): Boolean {
        pushWhitespace()
        val node = MutableSyntaxNode(type)
        nodeStack.add(node)
        fn()
        nodeStack.removeLast()
        return node.valid().also { valid -> if (valid) nodeStack.last().push(node.toImmutable()) }
    }

    inline fun nestLast(type: Type, crossinline fn: () -> Unit) {
        val node = MutableSyntaxNode(type)
        val last = nodeStack.last().pop()
        nodeStack.add(node)
        node.push(last)
        fn()
        nodeStack.removeLast()
        nodeStack.last().push(node.toImmutable())
    }

    fun push() {
        val node = nodeStack.last()
        while (buffer.isNotEmpty()) {
            node.push(buffer.removeFirst())
        }
        fillBuffer()
    }

    fun finish(): GreenNode<Type> = when (nodeStack.size) {
        1 -> nodeStack.first().toImmutable()
        0 -> error("Node stack is empty")
        else -> error("Node stack has more than 1 element")
    }

    val startWithNewline get(): Boolean = sawNewline
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