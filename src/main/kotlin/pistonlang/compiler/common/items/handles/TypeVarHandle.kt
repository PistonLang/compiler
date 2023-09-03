package pistonlang.compiler.common.items.handles

import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.main.MainInterners
import pistonlang.compiler.common.types.InferenceVar

class TypeVarHandle internal constructor(
    @PublishedApi internal val typeOrParam: Int,
    @PublishedApi internal val index: Int
) : Qualifiable {
    inline fun <T> match(onInferVar: (InferenceVar) -> T, onTypeParam: (TypeParamId) -> T) =
        if (index == -1) onTypeParam(TypeParamId(typeOrParam))
        else onInferVar(InferenceVar(TypeId(typeOrParam), index))

    val asTypeParam: TypeParamId?
        get() = if (index == -1) TypeParamId(typeOrParam) else null

    val asInferVar: InferenceVar?
        get() = if (index == -1) null else InferenceVar(TypeId(typeOrParam), index)

    override fun qualify(interners: MainInterners): String = match(
        onInferVar = { it.qualify(interners) },
        onTypeParam = { it.qualify(interners) }
    )

    override fun toString(): String = match(
        onInferVar = { it.toString() },
        onTypeParam = { it.toString() }
    )

    override fun hashCode(): Int = typeOrParam * 31 + index

    override fun equals(other: Any?): Boolean =
        other is TypeVarHandle && typeOrParam == other.typeOrParam && index == other.index
}

fun TypeParamId.asTypeVar() = TypeVarHandle(value, -1)

fun InferenceVar.asTypeVar() =
    if (index == -1) error("-1 cannot be a variable index")
    else TypeVarHandle(type.value, index)