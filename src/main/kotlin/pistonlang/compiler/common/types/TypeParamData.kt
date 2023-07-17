package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.TypeParamId

data class TypeParamData(val ids: List<TypeParamId>, val nameMap: Map<String, List<TypeParamId>>)

val emptyTypeParamData = TypeParamData(emptyList(), emptyMap())
