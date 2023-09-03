package pistonlang.compiler.common.items.handles

import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.items.TypeVarId
import pistonlang.compiler.common.main.MainInterners

enum class TypeType {
    Type,
    TypeParam,
    TypeVar,
    Error
}

enum class TypeError {
    UnknownType,
    MissingSTL,
    UnspecifiedType,
    ConflictingArgument
}

class TypeHandle internal constructor(
    @PublishedApi internal val id: Int,
    val type: TypeType
) : Qualifiable {
    inline fun <T> match(
        onType: (TypeId) -> T,
        onTypeParam: (TypeParamId) -> T,
        onTypeVar: (TypeVarId) -> T,
        onError: (TypeError) -> T
    ) = when (type) {
        TypeType.Type -> onType(TypeId(id))
        TypeType.TypeParam -> onTypeParam(TypeParamId(id))
        TypeType.TypeVar -> onTypeVar(TypeVarId(id))
        TypeType.Error -> onError(TypeError.entries[id])
    }

    val asTypeParam: TypeParamId?
        get() =
            if (type == TypeType.TypeParam) TypeParamId(id) else null

    val asType: TypeId?
        get() =
            if (type == TypeType.Type) TypeId(id) else null

    val asTypeVar: TypeVarId?
        get() =
            if (type == TypeType.TypeVar) TypeVarId(id) else null

    override fun equals(other: Any?): Boolean =
        other is TypeHandle && other.id == id && other.type == type

    override fun hashCode(): Int = type.hashCode() * 31 + id.hashCode()

    override fun toString(): String = when (type) {
        TypeType.Type -> TypeId(id).toString()
        TypeType.TypeParam -> TypeParamId(id).toString()
        TypeType.TypeVar -> TypeVarId(id).toString()
        TypeType.Error -> TypeError.entries[id].toString()
    }

    override fun qualify(interners: MainInterners): String = match(
        onTypeParam = { it.qualify(interners) },
        onType = { it.qualify(interners) },
        onTypeVar = { it.qualify(interners) },
        onError = { it.toString() }
    )
}

fun TypeId.asType() = TypeHandle(value, TypeType.Type)
fun TypeParamId.asType() = TypeHandle(value, TypeType.TypeParam)
fun TypeError.asType() = TypeHandle(ordinal, TypeType.Error)
fun TypeVarId.asType() = TypeHandle(value, TypeType.TypeVar)