package pistonlang.compiler.common.items

interface Id {
    val value: Int
}

data object UnitId : Id {
    override val value: Int
        get() = 0
}

@JvmInline
value class PackageId @PublishedApi internal constructor(override val value: Int) : Id

@JvmInline
value class FileId @PublishedApi internal constructor(override val value: Int) : Id

@JvmInline
value class MemberId @PublishedApi internal constructor(override val value: Int) : Id

@JvmInline
value class TypeId @PublishedApi internal constructor(override val value: Int) : Id

@JvmInline
value class TypeParamId @PublishedApi internal constructor(override val value: Int) : Id