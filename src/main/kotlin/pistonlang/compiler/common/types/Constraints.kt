package pistonlang.compiler.common.types

import kotlinx.collections.immutable.PersistentSet

sealed interface TypeConstraint<out T>

data class BoundsConstraint<out T>(
    val canBeNullable: Boolean,
    val lowerTypeBounds: TypeDAG,
    val lowerVarBounds: PersistentSet<T>,
    val upperTypeBounds: List<TypeInstance>,
    val upperVarBounds: PersistentSet<T>
): TypeConstraint<T>

data class TypeEqualityConstraint(val instance: TypeInstance): TypeConstraint<Nothing>

data class VarEqualityConstraint<out T>(val variable: T): TypeConstraint<T>

data object ErrorConstraint : TypeConstraint<Nothing>