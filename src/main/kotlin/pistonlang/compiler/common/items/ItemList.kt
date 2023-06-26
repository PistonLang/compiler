package pistonlang.compiler.common.items

import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.RelativeNodeLoc

enum class ItemType(
    val type: Boolean = false,
    val callable: Boolean = false,
    val value: Boolean = false,
    val mutable: Boolean = false
) {
    MultiInstanceClass(type = true, callable = true),
    SingletonClass(type = true, value = true),
    Trait(type = true),
    Val(value = true),
    Var(value = true, mutable = true),
    Function(callable = true),
    Getter(value = true),
    Setter(mutable = true),
}

@JvmInline
value class ItemList<Type: SyntaxType> internal constructor(private val list: List<List<RelativeNodeLoc<Type>>>) {

    fun iteratorFor(type: ItemType) = list[type.ordinal].iterator()

    operator fun get(type: ItemType, index: Int) = list[type.ordinal][index]
}

@JvmInline
value class MutableItemList<Type: SyntaxType> private constructor(private val list: MutableList<MutableList<RelativeNodeLoc<Type>>>) {
    constructor() : this(MutableList(ItemType.values().size) { arrayListOf() })

    fun add(type: ItemType, reference: RelativeNodeLoc<Type>) {
        list[type.ordinal].add(reference)
    }

    fun toImmutable() = ItemList(list.toList())
}

fun <Type: SyntaxType> itemListOf(vararg items: Pair<ItemType, RelativeNodeLoc<Type>>): ItemList<Type> {
    val mut = MutableItemList<Type>()
    items.forEach { mut.add(it.first, it.second) }
    return mut.toImmutable()
}