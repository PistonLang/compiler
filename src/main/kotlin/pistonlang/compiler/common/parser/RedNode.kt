package pistonlang.compiler.common.parser

import pistonlang.compiler.util.contains
import pistonlang.compiler.util.isBefore

class RedNode<T : SyntaxType>(
    val parent: RedNode<T>?,
    val green: GreenNode<T>,
    val pos: Int
) {
    fun wrap(child: GreenChild<T>) = RedNode(this, child.value, pos + child.offset)

    operator fun get(pos: Int) = green[pos].let(this::wrap)

    val type get() = green.type

    val length get() = green.length

    val span get() = pos..(pos + length)

    val childCount get() = green.childCount

    val childIterator: Iterator<RedNode<T>>
        get() = object : Iterator<RedNode<T>> {
            val green = this@RedNode.green.childIterator

            override fun hasNext() = green.hasNext()

            override fun next() = green.next().let(this@RedNode::wrap)
        }

    fun findAt(span: IntRange, type: T) = findAt(this, span, type)

    fun firstDirectChild(type: T) = green.firstDirectChild(type)?.let(this::wrap)

    fun lastDirectChild(type: T) = green.lastDirectChild(type)?.let(this::wrap)

    fun findParent(type: T) = findParent(this, type)

    fun format(builder: StringBuilder, prefix: String) {
        builder
            .append(type.name.replaceFirstChar(Char::titlecase))
            .append('@')
            .append(pos)

        val iter = childIterator
        var hasNext = iter.hasNext()

        while (hasNext) {
            val child = iter.next()
            hasNext = iter.hasNext()
            builder.append('\n').append(prefix)

            if (hasNext) {
                builder.append("├─")
                child.format(builder, "$prefix│ ")
            } else {
                builder.append("└─")
                child.format(builder, "$prefix  ")
            }
        }
    }
}

fun <T> RedNode<T>.firstDirectChild(set: SyntaxSet<T>) where T: SyntaxType, T: Enum<T> =
    green.firstDirectChild(set)?.let(this::wrap)

fun <T> RedNode<T>.lastDirectChild(set: SyntaxSet<T>) where T: SyntaxType, T: Enum<T> =
    green.lastDirectChild(set)?.let(this::wrap)

private tailrec fun <T: SyntaxType> findAt(node: RedNode<T>, span: IntRange, type: T): RedNode<T>? {
    if (node.type == type && node.span == span) return node
    if (node.childCount == 0) return null

    var lowerBound = 0
    var upperBound = node.childCount - 1
    while (lowerBound <= upperBound) {
        val mid = (lowerBound + upperBound) / 2
        val curr = node[mid]
        when {
            curr.span in span -> return findAt(curr, span, type)
            curr.span isBefore span -> lowerBound = mid + 1
            else -> upperBound = mid - 1
        }
    }

    return null
}

private tailrec fun <T: SyntaxType> findParent(node: RedNode<T>, type: T): RedNode<T>? {
    val par = node.parent ?: return null
    return if (par.type == type) par else findParent(par, type)
}

private tailrec fun <T> findParent(node: RedNode<T>, set: SyntaxSet<T>): RedNode<T>? where T: SyntaxType, T: Enum<T> {
    val par = node.parent ?: return null
    return if (par.type in set) par else findParent(par, set)
}

fun <T> RedNode<T>.findParent(set: SyntaxSet<T>): RedNode<T>? where T: SyntaxType, T: Enum<T> =
    findParent(this, set)