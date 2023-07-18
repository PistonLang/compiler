package pistonlang.compiler.common.items

import pistonlang.compiler.common.main.MainInterners

interface Id : Qualifiable {
    val value: Int
}

data object UnitId : Id {
    override val value: Int
        get() = 0

    override fun qualify(interners: MainInterners): String =
        "Unit"
}

@JvmInline
value class PackageId @PublishedApi internal constructor(override val value: Int) : Id {
    override fun qualify(interners: MainInterners): String =
        interners.packIds.getKey(this).toString()
}

@JvmInline
value class FileId @PublishedApi internal constructor(override val value: Int) : Id {
    override fun qualify(interners: MainInterners): String =
        interners.fileIds.getKey(this).toString()
}

@JvmInline
value class MemberId @PublishedApi internal constructor(override val value: Int) : Id {
    override fun qualify(interners: MainInterners): String =
        interners.memberIds.getKey(this).qualify(interners)
}

@JvmInline
value class TypeId @PublishedApi internal constructor(override val value: Int) : Id {
    override fun qualify(interners: MainInterners): String =
        interners.typeIds.getKey(this).qualify(interners)
}

@JvmInline
value class TypeParamId @PublishedApi internal constructor(override val value: Int) : Id {
    override fun qualify(interners: MainInterners): String =
        interners.typeParamIds.getKey(this).qualify(interners)
}