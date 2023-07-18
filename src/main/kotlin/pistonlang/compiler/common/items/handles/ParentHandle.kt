package pistonlang.compiler.common.items.handles

import pistonlang.compiler.common.items.FileId
import pistonlang.compiler.common.items.MemberId
import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.main.MainInterners

enum class ParentType {
    File,
    Member
}

class ParentHandle internal constructor(@PublishedApi internal val value: Int, val type: ParentType) : Qualifiable {
    inline fun <T> match(onFile: (FileId) -> T, onMember: (MemberId) -> T) = when (type) {
        ParentType.File -> onFile(FileId(value))
        ParentType.Member -> onMember(MemberId(value))
    }

    val asFile: FileId?
        get() = if (type == ParentType.File) FileId(value) else null

    val asMember: MemberId?
        get() = if (type == ParentType.Member) MemberId(value) else null

    override fun equals(other: Any?): Boolean =
        other is ParentHandle && other.value == value && other.type == type

    override fun hashCode(): Int = type.hashCode() * 31 + value.hashCode()

    override fun toString(): String = when (type) {
        ParentType.File -> FileId(value).toString()
        ParentType.Member -> MemberId(value).toString()
    }

    override fun qualify(interners: MainInterners) = match(
        onFile = { it.qualify(interners) },
        onMember = { it.qualify(interners) }
    )
}

fun FileId.asParent() = ParentHandle(value, ParentType.File)
fun MemberId.asParent() = ParentHandle(value, ParentType.Member)