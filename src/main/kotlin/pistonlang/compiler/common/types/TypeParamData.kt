package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.items.TypeVarId

data class TypeParamData(
    val ids: List<TypeParamId>,
    val nameMap: Map<String, List<TypeParamId>>,
    val varMap: Map<String, List<TypeVarId>>
)

val emptyTypeParamData = TypeParamData(emptyList(), emptyMap(), emptyMap())
