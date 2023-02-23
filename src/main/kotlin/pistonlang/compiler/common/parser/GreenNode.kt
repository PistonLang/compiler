package pistonlang.compiler.common.parser

import pistonlang.compiler.util.EmptyIterator

sealed interface GreenNode<T : SyntaxType> {
    val type: T
    val length: Int
    val content: String
    val childCount: Int
    val childIterator: Iterator<GreenChild<T>>
    operator fun get(index: Int): GreenChild<T>
}

data class GreenLeaf<T : SyntaxType>(override val type: T, override val content: String) : GreenNode<T> {
    override val length: Int
        get() = content.length

    override val childCount: Int
        get() = 0

    override fun get(index: Int): GreenChild<T> = error("Tokens have no children")

    override val childIterator: Iterator<GreenChild<T>>
        get() = EmptyIterator
}

data class GreenBranch<T : SyntaxType>(
    override val type: T,
    private val children: List<GreenChild<T>>,
    override val length: Int
) : GreenNode<T> {
    override val content: String
        get() = children.joinToString()

    override val childCount: Int
        get() = children.size

    override fun get(index: Int): GreenChild<T> = children[index]

    override val childIterator: Iterator<GreenChild<T>>
        get() = object : Iterator<GreenChild<T>> {
            var index = 0

            override fun hasNext() = index < children.size

            override fun next() = children[index++]
        }
}

fun <T: SyntaxType> GreenNode<T>.firstDirectChild(type: T) =
    childIterator.asSequence().firstOrNull { it.type == type }

fun <T> GreenNode<T>.firstDirectChild(set: SyntaxSet<T>) where T: SyntaxType, T: Enum<T> =
    childIterator.asSequence().firstOrNull { it.type in set }

fun <T: SyntaxType> GreenNode<T>.lastDirectChild(type: T) =
    childIterator.asSequence().lastOrNull { it.type == type }

fun <T> GreenNode<T>.lastDirectChild(set: SyntaxSet<T>) where T: SyntaxType, T: Enum<T> =
    childIterator.asSequence().lastOrNull { it.type in set }

typealias GreenChild<T> = Offset<GreenNode<T>>

data class Offset<out T>(val offset: Int, val value: T)

val <T : SyntaxType> GreenChild<T>.type get() = this.value.type

val List<GreenChild<*>>.textLength: Int
    get() = if (isEmpty()) 0 else last().let { it.offset + it.value.length }