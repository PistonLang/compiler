package pistonlang.compiler.common.items.handles

import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.main.MainInterners
import pistonlang.compiler.common.types.TypeVar

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
        onTypeVar: (TypeVar) -> T,
        onError: (TypeError) -> T
    ) = when (type) {
        TypeType.Type -> onType(TypeId(id))
        TypeType.TypeParam -> onTypeParam(TypeParamId(id))
        TypeType.TypeVar -> onTypeVar(TypeVar(id))
        TypeType.Error -> onError(TypeError.entries[id])
    }

    val asTypeParam: TypeParamId?
        get() =
            if (type == TypeType.TypeParam) TypeParamId(id) else null

    val asType: TypeId?
        get() =
            if (type == TypeType.Type) TypeId(id) else null

    val asTypeVar: TypeVar?
        get() =
            if (type == TypeType.TypeVar) TypeVar(id) else null

    override fun equals(other: Any?): Boolean =
        other is TypeHandle && other.id == id && other.type == type

    override fun hashCode(): Int = type.hashCode() * 31 + id.hashCode()

    override fun toString(): String = when (type) {
        TypeType.Type -> TypeId(id).toString()
        TypeType.TypeParam -> TypeParamId(id).toString()
        TypeType.TypeVar -> TypeVar(id).toString()
        TypeType.Error -> TypeError.entries[id].toString()
    }

    override fun qualify(interners: MainInterners): String = match(
        onTypeParam = { it.qualify(interners) },
        onType = { it.qualify(interners) },
        onTypeVar = { it.toString() },
        onError = { it.toString() }
    )
}

fun TypeId.asType() = TypeHandle(value, TypeType.Type)
fun TypeParamId.asType() = TypeHandle(value, TypeType.TypeParam)
fun TypeError.asType() = TypeHandle(ordinal, TypeType.Error)
fun TypeVar.asType() = TypeHandle(id, TypeType.TypeVar)