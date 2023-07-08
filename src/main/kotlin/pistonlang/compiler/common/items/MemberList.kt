package pistonlang.compiler.common.items

import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.RelativeNodeLoc

enum class MemberType(val item: ItemType, val buildHandle: (ParentHandle, String, Int) -> MemberHandle) {
    MultiInstanceClass(ItemType.MultiInstanceClass, ::MultiInstanceClassHandle),
    SingletonClass(ItemType.SingletonClass, ::SingletonClassHandle),
    Trait(ItemType.Trait, ::TraitHandle),
    Val(ItemType.Val, ::ValHandle),
    Var(ItemType.Var, ::VarHandle),
    Function(ItemType.Function, ::FunctionHandle),
    Getter(ItemType.Getter, ::GetterHandle),
    Setter(ItemType.Setter, ::SetterHandle),
}

@JvmInline
value class MemberList<Type : SyntaxType> internal constructor(private val list: List<List<RelativeNodeLoc<Type>>>) {

    fun iteratorFor(type: MemberType) = list[type.ordinal].iterator()

    operator fun get(type: MemberType, index: Int) = list[type.ordinal][index]
}

@JvmInline
value class MutableMemberList<Type : SyntaxType> private constructor(private val list: MutableList<MutableList<RelativeNodeLoc<Type>>>) {
    constructor() : this(MutableList(MemberType.entries.size) { arrayListOf() })

    fun add(type: MemberType, reference: RelativeNodeLoc<Type>) {
        list[type.ordinal].add(reference)
    }

    fun toImmutable() = MemberList(list.toList())
}

fun <Type : SyntaxType> memberListOf(vararg items: Pair<MemberType, RelativeNodeLoc<Type>>): MemberList<Type> {
    val mut = MutableMemberList<Type>()
    items.forEach { mut.add(it.first, it.second) }
    return mut.toImmutable()
}