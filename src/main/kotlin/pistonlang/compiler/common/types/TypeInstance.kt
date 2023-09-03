package pistonlang.compiler.common.types

import pistonlang.compiler.common.items.Qualifiable
import pistonlang.compiler.common.items.TypeParamId
import pistonlang.compiler.common.items.TypeVarId
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
val unspecifiedInstance = TypeInstance(TypeError.UnspecifiedType.asType(), emptyList(), false)
val conflictingArgumentInstance = TypeInstance(TypeError.ConflictingArgument.asType(), emptyList(), false)
val missingSTLType = TypeError.UnknownType.asType()

fun TypeInstance.asTypeParam(): TypeParamId? = type.asTypeParam
fun TypeInstance.asTypeVar(): TypeVarId? = type.asTypeVar

fun TypeParamId.toInstance(nullable: Boolean = false) = TypeInstance(this.asType(), emptyList(), nullable)
fun TypeVarId.toInstance(nullable: Boolean = false) = TypeInstance(this.asType(), emptyList(), nullable)