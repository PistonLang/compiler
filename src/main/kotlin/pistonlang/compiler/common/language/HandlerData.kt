package pistonlang.compiler.common.language

import pistonlang.compiler.common.items.Dependent
import pistonlang.compiler.common.types.TypeInstance
import pistonlang.compiler.common.types.TypeParamBound
import pistonlang.compiler.common.types.unknownInstance
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.lists.NonEmptyList

typealias SupertypeData<Type> = Dependent<Type, List<TypeInstance>>

val errorSupertypeData = SupertypeData<Nothing>(emptyList(), emptyList())

typealias ReturnData<Type> = Dependent<Type, TypeInstance>

val errorReturnData = ReturnData<Nothing>(emptyList(), unknownInstance)

typealias ParamData<Type> = Dependent<Type, List<TypeInstance>>

val emptyParamData = ParamData<Nothing>(emptyList(), emptyList())

typealias TypeBoundData<Type> = Dependent<Type, List<TypeParamBound>>

val emptyTypeBoundData = TypeBoundData<Nothing>(emptyList(), emptyList())