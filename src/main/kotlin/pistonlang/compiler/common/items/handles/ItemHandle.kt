package pistonlang.compiler.common.items.handles

import pistonlang.compiler.common.items.MemberId
import pistonlang.compiler.common.items.PackageId
import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.main.MainInterners

enum class ItemType {
    Package,
    TypeParam,
    Member,
    Error
}

enum class HandleError {
    InvalidPathToken
}

class ItemHandle private constructor(
    @PublishedApi internal val id: Int,
    val type: ItemType
) {
    constructor(packId: PackageId) : this(packId.value, ItemType.Package)
    constructor(memberId: MemberId) : this(memberId.value, ItemType.Member)
    constructor(error: HandleError) : this(error.ordinal, ItemType.Error)
    constructor(typeParamId: TypeParamId) : this(typeParamId.value, ItemType.TypeParam)


    inline fun <T> match(
        onPackage: (PackageId) -> T,
        onMember: (MemberId) -> T,
        onTypeParam: (TypeParamId) -> T,
        onError: (HandleError) -> T,
    ) = when (type) {
        ItemType.Package -> onPackage(PackageId(id))
        ItemType.Member -> onMember(MemberId(id))
        ItemType.TypeParam -> onTypeParam(TypeParamId(id))
        ItemType.Error -> onError(HandleError.entries[id])
    }

    val asPackage: PackageId?
        get() = if (type == ItemType.Package) PackageId(id) else null

    val asMember: MemberId?
        get() = if (type == ItemType.Member) MemberId(id) else null

    val asTypeParam: TypeParamId?
        get() = if (type == ItemType.TypeParam) TypeParamId(id) else null

    val asError: HandleError?
        get() = if (type == ItemType.Error) HandleError.entries[id] else null

    override fun equals(other: Any?): Boolean =
        other is ItemHandle && other.id == id && other.type == type

    override fun hashCode(): Int = type.hashCode() * 31 + id.hashCode()

    override fun toString(): String = when (type) {
        ItemType.Package -> PackageId(id).toString()
        ItemType.Member -> MemberId(id).toString()
        ItemType.Error -> HandleError.entries[id].toString()
        ItemType.TypeParam -> TypeParamId(id).toString()
    }

    fun toTypeHandle(interners: MainInterners): TypeHandle? = match(
        onPackage = { null },
        onMember = { if (interners.memberIds[it].type.newType) TypeHandle(interners.typeIds[it]) else null },
        onTypeParam = { TypeHandle(it) },
        onError = { null }
    )
}

