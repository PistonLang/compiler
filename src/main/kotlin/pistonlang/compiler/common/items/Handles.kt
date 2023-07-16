package pistonlang.compiler.common.items

import pistonlang.compiler.common.files.packPathDelimiter

sealed interface Handle

/**
 * A handle to something that can be a parent of another handle
 */
sealed interface ParentHandle : Handle {
    val isFile: Boolean get() = false
}

/**
 * A handle to something that has a parent
 */
sealed interface ChildHandle : Handle {
    val parent: ParentHandle
}

/**
 * A handle to a file, represented by a path
 */
@JvmInline
value class FileHandle(val path: String) : ParentHandle {
    override val isFile: Boolean
        get() = true
}

/**
 * A handle to an item (anything that can be referenced during type-checking)
 */
sealed interface ItemHandle : Handle {
    val itemType: ItemType
}

/**
 * A handle to a package, represented by a path
 */
@JvmInline
value class PackageHandle(val path: String) : ItemHandle {
    val suffix: String
        get() = path.substring(path.lastIndexOf('.') + 1)

    fun subPackage(name: String) =
        if (path.isEmpty()) PackageHandle(name)
        else PackageHandle("$path$packPathDelimiter$name")

    override val itemType: ItemType
        get() = ItemType.Package
}

val rootPackage = PackageHandle("")

/**
 * A handle for a file member
 */
sealed interface MemberHandle : ParentHandle, ItemHandle, ChildHandle {
    override val parent: ParentHandle
    val name: String
    val memberType: MemberType
    val id: Int
}

/**
 * A handle for an item which has parameters
 */
sealed interface ParameterizedHandle : MemberHandle

/**
 * A handle for a member which has a type or return type
 */
sealed interface TypedHandle : MemberHandle

data class FunctionHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : ParameterizedHandle, TypedHandle {
    override val itemType: ItemType
        get() = ItemType.Function

    override val memberType: MemberType
        get() = MemberType.Function
}

/**
 * A handle to a type, which can be used in a type instance
 */
sealed interface TypeHandle : ItemHandle

/**
 * A handle to a type declaration
 */
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
) : TypedHandle {
    override val itemType: ItemType
        get() = ItemType.Val

    override val memberType: MemberType
        get() = MemberType.Val
}

data class VarHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : TypedHandle {
    override val itemType: ItemType
        get() = ItemType.Var

    override val memberType: MemberType
        get() = MemberType.Var
}

data class GetterHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : TypedHandle {
    override val itemType: ItemType
        get() = ItemType.Getter

    override val memberType: MemberType
        get() = MemberType.Getter
}

data class SetterHandle(
    override val parent: ParentHandle,
    override val name: String,
    override val id: Int
) : TypedHandle, ParameterizedHandle {
    override val itemType: ItemType
        get() = ItemType.Setter

    override val memberType: MemberType
        get() = MemberType.Setter
}

data class ConstructorHandle(
    override val parent: MultiInstanceClassHandle,
    val id: Int
) : ItemHandle, ChildHandle {
    override val itemType: ItemType
        get() = ItemType.Constructor
}

data class TypeParamHandle(
    override val parent: ParentHandle,
    val id: Int
) : ItemHandle, TypeHandle, ChildHandle {
    override val itemType: ItemType
        get() = ItemType.TypeParam
}

/**
 * This handle is used for representing an error in places when a reference would be expected
 */
data object ErrorHandle : ItemHandle, TypeHandle {
    override val itemType: ItemType
        get() = ItemType.Null
}