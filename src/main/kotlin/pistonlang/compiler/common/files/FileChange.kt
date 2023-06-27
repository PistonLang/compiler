package pistonlang.compiler.common.files

import pistonlang.compiler.common.items.FileHandle
import pistonlang.compiler.common.queries.QueryVersion

enum class ChangeType {
    Addition,
    Update,
    Removal,
}

data class FileChange(val file: FileHandle, val type: ChangeType, val version: QueryVersion)