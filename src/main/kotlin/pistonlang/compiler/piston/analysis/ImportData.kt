package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.items.DependencyList
import pistonlang.compiler.piston.parser.PistonType

data class ImportData(val deps: DependencyList<PistonType>, val nameMap: Map<String, List<Int>>) {
    fun isEmpty() = deps.isEmpty()
}

val emptyImportData = ImportData(emptyList(), emptyMap())