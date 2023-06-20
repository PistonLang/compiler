package pistonlang.compiler.common.handles

sealed interface ParentHandle

sealed interface ReferencableHandle {
    val isNamespace: Boolean
    val isPackage: Boolean get() = false
    val isError: Boolean get() = false
}

data class FileHandle(val path: String) : ParentHandle

@JvmInline
value class PackageHandle(val path: List<String>) : ReferencableHandle {
    fun subpackage(name: String) = PackageHandle(path + name)

    override val isNamespace: Boolean
        get() = true

    override val isPackage: Boolean
        get() = true
}

data class ItemHandle(val file: ParentHandle, val name: String, val type: ItemType, val id: Int) : ParentHandle, ReferencableHandle {
    override val isNamespace: Boolean
        get() = type.type
}

/**
 * This Handle is used for representing an error in places when a handle would be expected
 */
object NullHandle : ReferencableHandle {
    override val isNamespace: Boolean
        get() = false

    override val isError: Boolean
        get() = true
}