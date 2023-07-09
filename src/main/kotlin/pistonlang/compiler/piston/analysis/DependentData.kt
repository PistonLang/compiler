package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.items.Dependent
import pistonlang.compiler.common.items.TypeInstance
import pistonlang.compiler.common.items.TypeParamHandle
import pistonlang.compiler.common.items.errorInstance
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.NonEmptyList
import pistonlang.compiler.util.nonEmptyListOf

typealias ImportData = Dependent<PistonType, Map<String, List<Int>>>

val emptyImportData = ImportData(emptyList(), emptyMap())

typealias SupertypeData = Dependent<PistonType, NonEmptyList<TypeInstance>>

val errorSupertypeData = SupertypeData(emptyList(), nonEmptyListOf(errorInstance))
val emptySuperTypeData = SupertypeData(emptyList(), nonEmptyListOf(anyInstance))

typealias ReturnData = Dependent<PistonType, TypeInstance>

val errorReturnData = ReturnData(emptyList(), errorInstance)
val unitReturnData = ReturnData(emptyList(), unitInstance)

typealias ParamData = Dependent<PistonType, List<TypeInstance>>

val emptyParamData = ParamData(emptyList(), emptyList())

typealias TypeBoundData = Dependent<PistonType, List<List<TypeInstance>>>

val emptyTypeBoundData = TypeBoundData(emptyList(), emptyList())