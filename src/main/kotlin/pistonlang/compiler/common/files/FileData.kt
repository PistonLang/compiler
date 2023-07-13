package pistonlang.compiler.common.files

data class FileData(val valid: Boolean, val code: String)

val invalidFileData = FileData(false, "")