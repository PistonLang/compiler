package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.MainInterners

data class SupertypeData(val dag: TypeDAG, val excluding: Set<TypeId>): Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "SupertypeData ${excluding.qualify(interners)} ${dag.qualify(interners)}"
}

val emptySupertypeData = SupertypeData(emptyTypeDAG, emptySet())