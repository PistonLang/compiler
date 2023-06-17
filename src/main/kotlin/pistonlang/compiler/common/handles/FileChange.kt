package pistonlang.compiler.common.handles

import pistonlang.compiler.common.queries.QueryVersion

enum class ChangeType {
    Addition,
    Update,
    Removal,
}

data class FileChange(val file: FileHandle, val type: ChangeType, val version: QueryVersion)