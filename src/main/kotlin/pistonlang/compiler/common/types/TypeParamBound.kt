package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.main.MainInterners

data class TypeParamBound(val param: TypeParamId, val bound: TypeInstance): Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "${param.qualify(interners)} <: ${bound.qualify(interners)}"
}