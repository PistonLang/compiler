package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.MainInterners

data class SupertypeDAG(val dag: TypeDAG, val excluding: Set<TypeId>): Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "SupertypeData ${excluding.qualify(interners)} ${dag.qualify(interners)}"
}

val emptySupertypeDAG = SupertypeDAG(emptyTypeDAG, emptySet())