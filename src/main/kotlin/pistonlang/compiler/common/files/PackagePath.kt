package pistonlang.compiler.common.files


@JvmInline
value class PackagePath(val path: String)

val rootPackage = PackagePath("")