package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.items.ReferenceTree
import pistonlang.compiler.common.items.emptyReferenceTree
import pistonlang.compiler.piston.parser.PistonType

data class ImportData(val tree: ReferenceTree<PistonType>, val nameMap: Map<String, List<Int>>) {
    fun isEmpty() = tree.isEmpty()
}

val emptyImportData = ImportData(emptyReferenceTree, emptyMap())