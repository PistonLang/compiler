package pistonlang.compiler.common.handles

sealed interface ParentHandle

sealed interface PathDefinedHandle {
    val isNamespace: Boolean
    val isPackage: Boolean
}

data class FileHandle(val path: String) : ParentHandle

@JvmInline
value class PackageHandle(val path: List<String>) : PathDefinedHandle {
    override val isNamespace: Boolean
        get() = true

    override val isPackage: Boolean
        get() = true
}

data class ItemHandle(val file: ParentHandle, val name: String, val type: ItemType, val id: Int) : ParentHandle, PathDefinedHandle {
    override val isNamespace: Boolean
        get() = type.type

    override val isPackage: Boolean
        get() = false
}