package pistonlang.compiler.common.items.handles

import pistonlang.compiler.common.items.MemberId
import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.main.MainInterners

data class TypeParamHandle(val parent: MemberId, val index: Int) : Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "TypeParam(${parent.qualify(interners)}, ${index})"
}
