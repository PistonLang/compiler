package pistonlang.compiler.common.parser.nodes

import pistonlang.compiler.common.parser.SyntaxSet
import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.util.EmptyIterator

sealed interface GreenNode<Type : SyntaxType> {
    val type: Type
    val length: Int
    val content: String
    val childCount: Int
    val childIterator: Iterator<GreenChild<Type>>
    operator fun get(index: Int): GreenChild<Type>
}

data class GreenLeaf<Type : SyntaxType>(override val type: Type, override val content: String) : GreenNode<Type> {
    override val length: Int
        get() = content.length

    override val childCount: Int
        get() = 0

    override fun get(index: Int): GreenChild<Type> = error("Tokens have no children")

    override val childIterator: Iterator<GreenChild<Type>>
        get() = EmptyIterator
}

data class GreenBranch<Type : SyntaxType>(
    override val type: Type,
    private val children: List<GreenChild<Type>>,
    override val length: Int
) : GreenNode<Type> {
    override val content: String
        get() = children.joinToString()

    override val childCount: Int
        get() = children.size

    override fun get(index: Int): GreenChild<Type> = children[index]

    override val childIterator: Iterator<GreenChild<Type>>
        get() = object : Iterator<GreenChild<Type>> {
            var index = 0

            override fun hasNext() = index < children.size

            override fun next() = children[index++]
        }
}

fun <Type: SyntaxType> GreenNode<Type>.firstDirectChild(type: Type) =
    childIterator.asSequence().firstOrNull { it.type == type }

fun <Type> GreenNode<Type>.firstDirectChild(set: SyntaxSet<Type>) where Type: SyntaxType, Type: Enum<Type> =
    childIterator.asSequence().firstOrNull { it.type in set }

fun <Type: SyntaxType> GreenNode<Type>.lastDirectChild(type: Type) =
    childIterator.asSequence().lastOrNull { it.type == type }

fun <Type> GreenNode<Type>.lastDirectChild(set: SyntaxSet<Type>) where Type: SyntaxType, Type: Enum<Type> =
    childIterator.asSequence().lastOrNull { it.type in set }

typealias GreenChild<Type> = Offset<GreenNode<Type>>

data class Offset<out Type>(val offset: Int, val value: Type)

val <Type : SyntaxType> GreenChild<Type>.type get() = this.value.type

val <Type: SyntaxType> GreenChild<Type>.content get() = value.content

val <Type: SyntaxType> GreenChild<Type>.length get() = value.length

val <Type: SyntaxType> GreenChild<Type>.endPos get() = offset + length

val <Type: SyntaxType> GreenChild<Type>.parentRelativeLocation: RelativeNodeLoc<Type>
    get() = RelativeNodeLoc(offset..endPos, type)

val List<GreenChild<*>>.textLength: Int
    get() = if (isEmpty()) 0 else last().let { it.offset + it.value.length }