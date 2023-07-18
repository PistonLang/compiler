package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.handles.TypeError
import pistonlang.compiler.common.items.handles.TypeHandle
import pistonlang.compiler.common.items.handles.asType
import pistonlang.compiler.common.items.qualify
import pistonlang.compiler.common.main.MainInterners

data class TypeInstance(val type: TypeHandle, val args: List<TypeInstance>, val nullable: Boolean) : Qualifiable {
    override fun qualify(interners: MainInterners): String =
        "${type.qualify(interners)}${if (args.isEmpty()) "" else args.qualify(interners)}${if (nullable) "?" else ""}"
}

val unknownInstance = TypeInstance(TypeError.UnknownType.asType(), emptyList(), false)