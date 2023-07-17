package pistonlang.compiler.common.items.handles

import pistonlang.compiler.common.items.TypeId
import pistonlang.compiler.common.items.TypeParamId

enum class TypeType {
    Type,
    TypeParam,
    Error
}

enum class TypeError {
    UnknownType
}

class TypeHandle private constructor(
    @PublishedApi internal val id: Int,
    val type: TypeType
) {
    constructor(typeId: TypeId) : this(typeId.value, TypeType.Type)
    constructor(typeParam: TypeParamId) : this(typeParam.value, TypeType.TypeParam)
    constructor(error: TypeError) : this(error.ordinal, TypeType.Error)

    inline fun <T> match(
        onType: (TypeId) -> T,
        onTypeParam: (TypeParamId) -> T,
        onError: (TypeError) -> T
    ) = when (type) {
        TypeType.Type -> onType(TypeId(id))
        TypeType.TypeParam -> onTypeParam(TypeParamId(id))
        TypeType.Error -> onError(TypeError.entries[id])
    }

    val asTypeParam: TypeParamId?
        get() =
            if (type == TypeType.TypeParam) TypeParamId(id) else null

    val asType: TypeId?
        get() =
            if (type == TypeType.Type) TypeId(id) else null

    override fun equals(other: Any?): Boolean =
        other is TypeHandle && other.id == id && other.type == type

    override fun hashCode(): Int = type.hashCode() * 31 + id.hashCode()

    override fun toString(): String = when (type) {
        TypeType.Type -> TypeId(id).toString()
        TypeType.TypeParam -> TypeParamId(id).toString()
        TypeType.Error -> TypeError.entries[id].toString()
    }
}
