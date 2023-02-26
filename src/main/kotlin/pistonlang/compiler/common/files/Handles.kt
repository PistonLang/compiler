package pistonlang.compiler.common.files

@JvmInline
value class PackageHandle(val path: List<String>)

data class FileHandle(val path: String)

data class ItemHandle(val file: FileHandle, val name: String, val id: Int)