package pistonlang.compiler.common.items.handles

import pistonlang.compiler.common.items.MemberType

data class MemberHandle(
    val parent: ParentHandle,
    val type: MemberType,
    val name: String,
    val id: Int
)

