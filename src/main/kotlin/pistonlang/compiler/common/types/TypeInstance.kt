package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.ErrorHandle
import pistonlang.compiler.common.items.TypeHandle

data class TypeInstance(val type: TypeHandle, val args: List<TypeInstance>, val nullable: Boolean)

val errorInstance = TypeInstance(ErrorHandle, emptyList(), false)