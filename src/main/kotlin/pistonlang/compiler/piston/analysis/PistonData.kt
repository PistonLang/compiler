package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.items.Dependent
import pistonlang.compiler.piston.parser.PistonType

typealias ImportData = Dependent<PistonType, Map<String, List<Int>>>

val emptyImportData = ImportData(emptyList(), emptyMap())