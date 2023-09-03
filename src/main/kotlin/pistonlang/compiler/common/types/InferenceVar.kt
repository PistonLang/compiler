package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.main.MainInterners

data class InferenceVar(val type: TypeId, val index: Int) : Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "InferenceVar(${type.qualify(interners)}, $index)"
}