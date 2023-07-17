package pistonlang.compiler.common.files

data class FileData(val valid: Boolean, val code: String)

@JvmInline
value class FilePath(val path: String)

val invalidFileData = FileData(false, "")