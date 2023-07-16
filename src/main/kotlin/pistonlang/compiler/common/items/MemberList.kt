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
value class MemberList<Type : SyntaxType> internal constructor(private val list: List<Map<String, List<RelativeNodeLoc<Type>>>>) {

    fun iteratorFor(type: MemberType) = list[type.ordinal].iterator()

    operator fun get(type: MemberType, name: String, index: Int): RelativeNodeLoc<Type>? =
        list[type.ordinal][name]?.get(index)
}

@JvmInline
value class MutableMemberList<Type : SyntaxType> private constructor(private val list: MutableList<MutableMap<String, MutableList<RelativeNodeLoc<Type>>>>) {
    constructor() : this(MutableList(MemberType.entries.size) { mutableMapOf() })

    fun add(type: MemberType, name: String, reference: RelativeNodeLoc<Type>) {
        list[type.ordinal].getOrPut(name) { mutableListOf() }.add(reference)
    }

    fun toImmutable() = MemberList(list.toList())
}