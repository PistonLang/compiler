package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.items.DependencyList
import pistonlang.compiler.common.items.TypeInstance
import pistonlang.compiler.common.items.errorInstance
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.NonEmptyList
import pistonlang.compiler.util.nonEmptyListOf

data class SupertypeData(val tree: DependencyList<PistonType>, val types: NonEmptyList<TypeInstance>)

val errorSupertypeData = SupertypeData(emptyList(), nonEmptyListOf(errorInstance))
val emptySuperTypeData = SupertypeData(emptyList(), nonEmptyListOf(anyInstance))