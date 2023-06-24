package pistonlang.compiler.common.items

sealed interface ParentReference {
    val isFile: Boolean get() = false
}

data class FileReference(val path: String) : ParentReference {
    override val isFile: Boolean
        get() = true
}

sealed interface UsableReference {
    val isNamespace: Boolean
    val isPackage: Boolean get() = false
    val isError: Boolean get() = false
}

@JvmInline
value class PackageReference(val path: List<String>) : UsableReference {
    fun subpackage(name: String) = PackageReference(path + name)

    override val isNamespace: Boolean
        get() = true

    override val isPackage: Boolean
        get() = true
}

data class ItemReference(
    val parent: ParentReference,
    val name: String,
    val type: ItemType,
    val id: Int,
) : ParentReference, UsableReference {
    override val isNamespace: Boolean
        get() = type.type
}

/**
 * This reference is used for representing an error in places when a reference would be expected
 */
object NullReference : UsableReference {
    override val isNamespace: Boolean
        get() = false

    override val isError: Boolean
        get() = true
}