package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.items.Dependent
import pistonlang.compiler.common.types.TypeInstance
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.lists.NonEmptyList

typealias ImportData = Dependent<PistonType, Map<String, List<Int>>>

typealias SupertypeData = Dependent<PistonType, NonEmptyList<TypeInstance>>

typealias ReturnData = Dependent<PistonType, TypeInstance>

typealias ParamData = Dependent<PistonType, List<TypeInstance>>

typealias TypeBoundData = Dependent<PistonType, List<List<TypeInstance>>>