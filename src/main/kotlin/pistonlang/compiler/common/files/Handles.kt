package pistonlang.compiler.common.files

sealed interface ParentHandle

sealed interface PathDefinedHandle

data class FileHandle(val path: String) : ParentHandle

@JvmInline
value class PackageHandle(val path: List<String>) : PathDefinedHandle

data class ItemHandle(val file: ParentHandle, val name: String, val id: Int) : ParentHandle, PathDefinedHandle