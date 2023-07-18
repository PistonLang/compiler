package pistonlang.compiler.common.items.handles

import pistonlang.compiler.common.items.MemberType
import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.main.MainInterners

data class MemberHandle(
    val parent: ParentHandle,
    val type: MemberType,
    val name: String,
    val id: Int
) : Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "${type}(${parent.qualify(interners)}, ${name}, ${id})"
}

