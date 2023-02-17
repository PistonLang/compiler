package pistonlang.compiler.common.parser

interface SyntaxType {
    val ignorable: Boolean
    val trailing: Boolean
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

typealias SyntaxChild<T> = Offset<Syntax<T>>

data class Offset<T>(val offset: Int, val value: T)

data class SyntaxNode<T : SyntaxType>(
    override val type: T,
    val children: List<SyntaxChild<T>>,
    override val length: Int
) : Syntax<T> {
    override val content: String
        get() = children.joinToString()
}

