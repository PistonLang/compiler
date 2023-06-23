package pistonlang.compiler.common.files

import pistonlang.compiler.common.items.FileReference
import pistonlang.compiler.common.queries.QueryVersion

enum class ChangeType {
    Addition,
    Update,
    Removal,
}

data class FileChange(val file: FileReference, val type: ChangeType, val version: QueryVersion)