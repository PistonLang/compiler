package pistonlang.compiler.common.language

import pistonlang.compiler.common.items.FileReference
import pistonlang.compiler.common.items.ItemList
import pistonlang.compiler.common.parser.nodes.GreenNode
import pistonlang.compiler.common.queries.Query

/**
 * A [LanguageHandler] handles language specific queries, as opposed to
 * a [pistonlang.compiler.common.main.CompilerInstance] which handles general queries
 */
interface LanguageHandler<Type : SyntaxType> {
    val extensions: List<String>
    val ast: Query<FileReference, GreenNode<Type>>
    val fileItems: Query<FileReference, Map<String, ItemList<Type>>>
}