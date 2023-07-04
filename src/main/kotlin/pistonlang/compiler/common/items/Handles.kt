package pistonlang.compiler.common.items

sealed interface ParentHandle {
    val isFile: Boolean get() = false

    fun findFile(): FileHandle
}

data class FileHandle(val path: String) : ParentHandle {
    override val isFile: Boolean
        get() = true

    override fun findFile(): FileHandle = this
}

sealed interface ItemHandle {
    val itemType: ItemType
    val isError: Boolean get() = false
}

@JvmInline
value class PackageHandle(val path: List<String>) : ItemHandle {
    fun subpackage(name: String) = PackageHandle(path + name)

    override val itemType: ItemType
        get() = ItemType.Package
}

/**
 * A handle for a file member
 */
sealed interface MemberHandle : ParentHandle, ItemHandle {
    val parent: ParentHandle
    val name: String
    val memberType: MemberType
    val id: Int

    override fun findFile(): FileHandle = parent.findFile()
}

sealed interface ParameterizedHandle : ItemHandle


data class FunctionHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : ParameterizedHandle, MemberHandle {
    override val itemType: ItemType
        get() = ItemType.Function

    override val memberType: MemberType
        get() = MemberType.Function
}

sealed interface TypeHandle : ItemHandle

sealed interface NewTypeHandle : MemberHandle, TypeHandle

data class SingletonClassHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : NewTypeHandle {
    override val itemType: ItemType
        get() = ItemType.SingletonClass

    override val memberType: MemberType
        get() = MemberType.SingletonClass
}

data class MultiInstanceClassHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : NewTypeHandle {
    override val itemType: ItemType
        get() = ItemType.MultiInstanceClass

    override val memberType: MemberType
        get() = MemberType.MultiInstanceClass
}

data class TraitHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : NewTypeHandle {
    override val itemType: ItemType
        get() = ItemType.Trait

    override val memberType: MemberType
        get() = MemberType.Trait
}

data class ValHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : MemberHandle {
    override val itemType: ItemType
        get() = ItemType.Val

    override val memberType: MemberType
        get() = MemberType.Val
}

data class VarHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : MemberHandle {
    override val itemType: ItemType
        get() = ItemType.Var

    override val memberType: MemberType
        get() = MemberType.Var
}

data class GetterHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : MemberHandle {
    override val itemType: ItemType
        get() = ItemType.Getter

    override val memberType: MemberType
        get() = MemberType.Getter
}

data class SetterHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : MemberHandle, ParameterizedHandle {
    override val itemType: ItemType
        get() = ItemType.Setter

    override val memberType: MemberType
        get() = MemberType.Setter
}

data class ConstructorHandle(
    val parent: MultiInstanceClassHandle,
    val id: Int
): ItemHandle {
    override val itemType: ItemType
        get() = ItemType.Constructor
}

data class TypeParamHandle(
    val parent: ParentHandle,
    val id: Int
) : ItemHandle, TypeHandle {
    override val itemType: ItemType
        get() = ItemType.TypeParam
}

/**
 * This handle is used for representing an error in places when a reference would be expected
 */
object ErrorHandle : ItemHandle {
    override val itemType: ItemType
        get() = ItemType.Null
}