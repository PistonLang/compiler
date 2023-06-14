package pistonlang.compiler.common

import pistonlang.compiler.common.files.FileHandle
import pistonlang.compiler.common.parser.GreenNode
import pistonlang.compiler.common.parser.NodeLocation
import pistonlang.compiler.common.parser.SyntaxType
import pistonlang.compiler.common.queries.Query

/**
 * A [LanguageHandler] handles language specific queries, as opposed to
 * a [pistonlang.compiler.common.main.CompilerInstance] which handles general queries
 */
interface LanguageHandler<Type : SyntaxType> {
    val extensions: List<String>
    val ast: Query<FileHandle, GreenNode<Type>>
    val fileItems: Query<FileHandle, Map<String, List<NodeLocation<Type>>>>
}