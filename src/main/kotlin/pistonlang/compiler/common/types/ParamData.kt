package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.items.TypeVarId

data class ParamData<ParamType>(
    val ids: List<ParamType>,
    val nameMap: Map<String, List<ParamType>>,
)

inline fun <T, S> ParamData<T>.map(fn: (T) -> S): ParamData<S> =
    ParamData(ids.map(fn), nameMap.mapValues { it.value.map(fn) })

typealias TypeParamData = ParamData<TypeParamId>
typealias TypeVarData = ParamData<TypeVarId>