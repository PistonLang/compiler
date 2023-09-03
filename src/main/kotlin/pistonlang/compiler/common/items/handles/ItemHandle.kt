package pistonlang.compiler.common.items.handles

import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.main.MainInterners

enum class ItemType {
    Package,
    TypeParam,
    TypeVar,
    Member,
    Error
}

enum class HandleError {
    InvalidPathToken
}

class ItemHandle internal constructor(
    @PublishedApi internal val id: Int,
    val type: ItemType
) : Qualifiable {
    inline fun <T> match(
        onPackage: (PackageId) -> T,
        onMember: (MemberId) -> T,
        onTypeParam: (TypeParamId) -> T,
        onTypeVar: (TypeVarId) -> T,
        onError: (HandleError) -> T,
    ) = when (type) {
        ItemType.Package -> onPackage(PackageId(id))
        ItemType.Member -> onMember(MemberId(id))
        ItemType.TypeParam -> onTypeParam(TypeParamId(id))
        ItemType.TypeVar -> onTypeVar(TypeVarId(id))
        ItemType.Error -> onError(HandleError.entries[id])
    }

    val asPackage: PackageId?
        get() = if (type == ItemType.Package) PackageId(id) else null

    val asMember: MemberId?
        get() = if (type == ItemType.Member) MemberId(id) else null

    val asTypeParam: TypeParamId?
        get() = if (type == ItemType.TypeParam) TypeParamId(id) else null

    val asTypeVar: TypeVarId?
        get() = if (type == ItemType.TypeVar) TypeVarId(id) else null

    val asError: HandleError?
        get() = if (type == ItemType.Error) HandleError.entries[id] else null


    override fun equals(other: Any?): Boolean =
        other is ItemHandle && other.id == id && other.type == type

    override fun hashCode(): Int = type.hashCode() * 31 + id.hashCode()

    override fun toString(): String = when (type) {
        ItemType.Package -> PackageId(id).toString()
        ItemType.Member -> MemberId(id).toString()
        ItemType.TypeParam -> TypeParamId(id).toString()
        ItemType.TypeVar -> TypeVarId(id).toString()
        ItemType.Error -> HandleError.entries[id].toString()
    }

    fun toTypeHandle(interners: MainInterners): TypeHandle? = match(
        onPackage = { null },
        onMember = { interners.typeIds[it]?.asType() },
        onTypeParam = { it.asType() },
        onTypeVar = { it.asType() },
        onError = { null }
    )

    override fun qualify(interners: MainInterners): String = match(
        onMember = { it.qualify(interners) },
        onTypeParam = { it.qualify(interners) },
        onPackage = { it.qualify(interners) },
        onTypeVar = { it.qualify(interners) },
        onError = { it.toString() }
    )
}

fun PackageId.asItem() = ItemHandle(value, ItemType.Package)
fun MemberId.asItem() = ItemHandle(value, ItemType.Member)
fun TypeParamId.asItem() = ItemHandle(value, ItemType.TypeParam)
fun TypeVarId.asItem() = ItemHandle(value, ItemType.TypeVar)
fun HandleError.asItem() = ItemHandle(ordinal, ItemType.Error)
