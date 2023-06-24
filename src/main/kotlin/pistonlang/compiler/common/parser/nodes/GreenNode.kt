package pistonlang.compiler.common.parser.nodes

import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.common.parser.SyntaxSet
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

val <Type: SyntaxType> GreenNode<Type>.childSequence get() = childIterator.asSequence()

fun <Type : SyntaxType> GreenNode<Type>.firstDirectChild(type: Type) =
    childSequence.firstOrNull { it.type == type }

inline fun <Type : SyntaxType> GreenNode<Type>.firstDirectChildOr(type: Type, fn: () -> GreenChild<Type>) =
    firstDirectChild(type) ?: fn()

fun <Type> GreenNode<Type>.firstDirectChild(set: SyntaxSet<Type>) where Type : SyntaxType, Type : Enum<Type> =
    childSequence.firstOrNull { it.type in set }

inline fun <Type> GreenNode<Type>.firstDirectChildOr(
    set: SyntaxSet<Type>,
    fn: () -> GreenChild<Type>
) where Type : SyntaxType, Type : Enum<Type> = firstDirectChild(set) ?: fn()

fun <Type : SyntaxType> GreenNode<Type>.lastDirectChild(type: Type) =
    childSequence.lastOrNull { it.type == type }

inline fun <Type : SyntaxType> GreenNode<Type>.lastDirectChildOr(type: Type, fn: () -> GreenChild<Type>) =
    lastDirectChild(type) ?: fn()

fun <Type> GreenNode<Type>.lastDirectChild(set: SyntaxSet<Type>) where Type : SyntaxType, Type : Enum<Type> =
    childSequence.lastOrNull { it.type in set }

inline fun <Type> GreenNode<Type>.lastDirectChildOr(
    set: SyntaxSet<Type>,
    fn: () -> GreenChild<Type>,
) where Type : SyntaxType, Type : Enum<Type> = firstDirectChild(set) ?: fn()

fun <Type : SyntaxType> GreenNode<Type>.firstDirectRawChild(type: Type) =
    firstDirectChild(type)?.value

inline fun <Type : SyntaxType> GreenNode<Type>.firstDirectRawChildOr(type: Type, fn: () -> GreenNode<Type>) =
    firstDirectRawChild(type) ?: fn()

fun <Type> GreenNode<Type>.firstDirectRawChild(set: SyntaxSet<Type>) where Type : SyntaxType, Type : Enum<Type> =
    firstDirectChild(set)?.value

inline fun <Type> GreenNode<Type>.firstDirectRawChildOr(
    set: SyntaxSet<Type>,
    fn: () -> GreenNode<Type>
) where Type : SyntaxType, Type : Enum<Type> = firstDirectRawChild(set) ?: fn()

fun <Type : SyntaxType> GreenNode<Type>.lastDirectRawChild(type: Type) =
    lastDirectChild(type)?.value

inline fun <Type : SyntaxType> GreenNode<Type>.lastDirectRawChildOr(type: Type, fn: () -> GreenChild<Type>) =
    lastDirectRawChild(type) ?: fn()

fun <Type> GreenNode<Type>.lastDirectRawChild(set: SyntaxSet<Type>) where Type : SyntaxType, Type : Enum<Type> =
    lastDirectChild(set)?.value

inline fun <Type> GreenNode<Type>.lastDirectRawChildOr(
    set: SyntaxSet<Type>,
    fn: () -> GreenChild<Type>,
) where Type : SyntaxType, Type : Enum<Type> = lastDirectChild(set) ?: fn()

typealias GreenChild<Type> = Offset<GreenNode<Type>>

data class Offset<out Type>(val offset: Int, val value: Type)

val <Type : SyntaxType> GreenChild<Type>.type get() = this.value.type

val <Type : SyntaxType> GreenChild<Type>.content get() = value.content

val <Type : SyntaxType> GreenChild<Type>.length get() = value.length

val <Type : SyntaxType> GreenChild<Type>.endPos get() = offset + length

val <Type : SyntaxType> GreenChild<Type>.parentRelativeLocation: RelativeNodeLoc<Type>
    get() = RelativeNodeLoc(offset..endPos, type)

val List<GreenChild<*>>.textLength: Int
    get() = if (isEmpty()) 0 else last().let { it.offset + it.value.length }

fun <Type: SyntaxType> GreenNode<Type>.locFrom(startPos: Int) =
    NodeLocation(startPos..(startPos + this.length), this.type)