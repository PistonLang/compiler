package pistonlang.compiler.piston.analysis

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import pistonlang.compiler.common.types.TypeDAG
import pistonlang.compiler.common.types.TypeInstance
import pistonlang.compiler.common.types.emptyTypeDAG

sealed interface TypeConstraint<out T>

data class BoundsConstraint<out T>(
    val canBeNullable: Boolean,
    val lowerTypeBounds: TypeDAG,
    val lowerVarBounds: PersistentSet<T>,
    val upperTypeBounds: PersistentSet<TypeInstance>,
    val upperVarBounds: PersistentSet<T>
) : TypeConstraint<T>

val emptyBoundsConstraint =
    BoundsConstraint<Nothing>(true, emptyTypeDAG, persistentHashSetOf(), persistentHashSetOf(), persistentHashSetOf())

data class TypeEqualityConstraint(val instance: TypeInstance) : TypeConstraint<Nothing>

data class VarEqualityConstraint<out T>(val variable: T) : TypeConstraint<T>

data object ErrorConstraint : TypeConstraint<Nothing>