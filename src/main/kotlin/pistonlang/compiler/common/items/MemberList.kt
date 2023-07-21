package pistonlang.compiler.common.items

import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.RelativeNodeLoc

enum class MemberType(val newType: Boolean = false) {
    MultiInstanceClass(newType = true),
    SingletonClass(newType = true),
    Trait(newType = true),
    Val,
    Var,
    Function,
    Getter,
    Setter,
}

@JvmInline
value class MemberList<out Type : SyntaxType> internal constructor(private val list: List<Map<String, List<RelativeNodeLoc<Type>>>>) {

    fun iteratorFor(type: MemberType) = list[type.ordinal].iterator()

    operator fun get(type: MemberType, name: String, index: Int): RelativeNodeLoc<Type>? =
        list[type.ordinal][name]?.get(index)
}

val emptyMemberList = MemberList(List(MemberType.entries.size) { emptyMap<String, List<RelativeNodeLoc<Nothing>>>() })

@JvmInline
value class MutableMemberList<Type : SyntaxType> private constructor(private val list: MutableList<MutableMap<String, MutableList<RelativeNodeLoc<Type>>>>) {
    constructor() : this(MutableList(MemberType.entries.size) { mutableMapOf() })

    fun add(type: MemberType, name: String, reference: RelativeNodeLoc<Type>) {
        list[type.ordinal].getOrPut(name) { mutableListOf() }.add(reference)
    }

    fun toImmutable() = MemberList(list.toList())
}