package pistonlang.compiler.common.items

sealed interface ParentReference {
    val isFile: Boolean get() = false

    fun findFile(): FileReference
}

data class FileReference(val path: String) : ParentReference {
    override val isFile: Boolean
        get() = true

    override fun findFile(): FileReference = this
}

sealed interface UsableReference {
    val isNamespace: Boolean
    val isType: Boolean
    val isPackage: Boolean get() = false
    val isError: Boolean get() = false
}

@JvmInline
value class PackageReference(val path: List<String>) : UsableReference {
    fun subpackage(name: String) = PackageReference(path + name)

    override val isNamespace: Boolean
        get() = true

    override val isType: Boolean
        get() = false

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

    override val isType: Boolean
        get() = type.type

    override fun findFile(): FileReference = parent.findFile()
}

data class TypeParamReference(
    val parent: ParentReference,
    val id: Int
) : UsableReference {
    override val isNamespace: Boolean
        get() = false

    override val isType: Boolean
        get() = true
}

/**
 * This reference is used for representing an error in places when a reference would be expected
 */
object NullReference : UsableReference {
    override val isNamespace: Boolean
        get() = false

    override val isError: Boolean
        get() = true

    override val isType: Boolean
        get() = true
}