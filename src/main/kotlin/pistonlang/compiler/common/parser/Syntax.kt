package pistonlang.compiler.common.parser

interface SyntaxType {
    val ignorable: Boolean
    val isNewline: Boolean
}

sealed interface Syntax<T : SyntaxType> {
    val type: T
    val length: Int
    val content: String
}

data class SyntaxToken<T : SyntaxType>(override val type: T, override val content: String) : Syntax<T> {
    override val length: Int
        get() = content.length
}

data class SyntaxNode<T : SyntaxType>(
    override val type: T,
    val children: List<SyntaxChild<T>>,
    override val length: Int
) : Syntax<T> {
    override val content: String
        get() = children.joinToString()
}

typealias SyntaxChild<T> = Offset<Syntax<T>>

data class Offset<out T>(val offset: Int, val value: T)

val <T: SyntaxType> SyntaxChild<T>.type get() = this.value.type

val List<SyntaxChild<*>>.textLength: Int
    get() = if (isEmpty()) 0 else last().let { it.offset + it.value.length }