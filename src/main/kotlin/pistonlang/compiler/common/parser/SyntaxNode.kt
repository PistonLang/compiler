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

data class SyntaxChild<T : SyntaxType>(val offset: Int, val syntax: Syntax<T>)

data class SyntaxNode<T : SyntaxType>(
    override val type: T,
    val children: List<SyntaxChild<T>>,
    override val length: Int
) : Syntax<T> {
    override val content: String
        get() = children.joinToString()
}

