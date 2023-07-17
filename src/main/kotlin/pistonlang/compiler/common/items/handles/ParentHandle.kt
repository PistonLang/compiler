package pistonlang.compiler.common.items.handles

import pistonlang.compiler.common.items.FileId
import pistonlang.compiler.common.items.MemberId

enum class ParentType {
    File,
    Member
}

class ParentHandle private constructor(@PublishedApi internal val id: Int, val type: ParentType) {
    constructor(fileId: FileId) : this(fileId.value, ParentType.File)
    constructor(memberId: MemberId) : this(memberId.value, ParentType.Member)

    inline fun <T> match(onFile: (FileId) -> T, onMember: (MemberId) -> T) = when (type) {
        ParentType.File -> onFile(FileId(id))
        ParentType.Member -> onMember(MemberId(id))
    }

    override fun equals(other: Any?): Boolean =
        other is ParentHandle && other.id == id && other.type == type

    override fun hashCode(): Int = type.hashCode() * 31 + id.hashCode()

    override fun toString(): String = when (type) {
        ParentType.File -> FileId(id).toString()
        ParentType.Member -> MemberId(id).toString()
    }
}