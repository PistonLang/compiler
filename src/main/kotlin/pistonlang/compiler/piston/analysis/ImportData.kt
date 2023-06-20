package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.handles.HandleTree
import pistonlang.compiler.common.handles.emptyHandleTree
import pistonlang.compiler.piston.parser.PistonType

data class ImportData(val tree: HandleTree<PistonType>, val nameMap: Map<String, List<Int>>) {
    fun isEmpty() = tree.isEmpty()
}

val emptyImportData = ImportData(emptyHandleTree, emptyMap())