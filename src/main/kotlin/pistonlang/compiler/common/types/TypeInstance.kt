package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.handles.TypeError
import pistonlang.compiler.common.items.handles.TypeHandle

data class TypeInstance(val type: TypeHandle, val args: List<TypeInstance>, val nullable: Boolean)

val unknownInstance = TypeInstance(TypeHandle(TypeError.UnknownType), emptyList(), false)