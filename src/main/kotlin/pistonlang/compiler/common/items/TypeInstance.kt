package pistonlang.compiler.common.items

data class TypeInstance(val type: TypeHandle, val args: List<TypeInstance>, val nullable: Boolean)

val errorInstance = TypeInstance(ErrorHandle, emptyList(), false)