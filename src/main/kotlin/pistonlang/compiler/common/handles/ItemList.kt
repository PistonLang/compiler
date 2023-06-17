package pistonlang.compiler.common.handles

import pistonlang.compiler.common.language.SyntaxType

@JvmInline
value class ItemList<Type: SyntaxType> internal constructor(private val list: List<List<ItemReference<Type>>>) {

    fun iteratorFor(type: ItemType) = list[type.ordinal].iterator()

    operator fun get(type: ItemType, index: Int) = list[type.ordinal][index]
}

@JvmInline
value class MutableItemList<Type: SyntaxType> private constructor(private val list: MutableList<MutableList<ItemReference<Type>>>) {
    constructor() : this(MutableList(ItemType.values().size) { arrayListOf() })

    fun add(reference: ItemReference<Type>) {
        list[reference.type.ordinal].add(reference)
    }

    fun toImmutable() = ItemList(list.toList())
}

fun <Type: SyntaxType> itemListOf(vararg items: ItemReference<Type>): ItemList<Type> {
    val mut = MutableItemList<Type>()
    items.forEach { mut.add(it) }
    return mut.toImmutable()
}