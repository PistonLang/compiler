package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.MainInterners

data class SupertypeDAG internal constructor(
    val dag: TypeDAG,
    internal val excluding: Set<TypeId>,
): Qualifiable {
    override fun qualify(interners: MainInterners): String {
        val excludingQ = if (excluding.isEmpty()) "" else "excluding: ${excluding.qualify(interners)}"
        return "SupertypeData $excludingQ ${dag.qualify(interners)}"
    }
}