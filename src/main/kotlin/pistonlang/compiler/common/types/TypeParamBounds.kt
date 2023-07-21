package pistonlang.compiler.common.types

import kotlinx.collections.immutable.PersistentSet
import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.MainInterners

data class TypeParamBounds(
    val canBeNullable: Boolean,
    val lowerTypeBounds: TypeDAG,
    val lowerVarBounds: PersistentSet<TypeParamId>,
    val upperVarBounds: PersistentSet<TypeParamId>
) : Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "TypeParamBounds: $canBeNullable ${lowerVarBounds.qualify(interners)} " +
                "${upperVarBounds.qualify(interners)} ${lowerTypeBounds.qualify(interners)}"
}