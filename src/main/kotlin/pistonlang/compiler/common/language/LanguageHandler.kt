package pistonlang.compiler.common.language

import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.common.parser.nodes.GreenNode
import pistonlang.compiler.common.queries.DependentQuery

/**
 * A [LanguageHandler] handles language specific queries, as opposed to
 * a [pistonlang.compiler.common.main.CompilerInstance] which handles general queries
 */
interface LanguageHandler<Type : SyntaxType> {
    val extensions: List<String>
    val ast: DependentQuery<FileHandle, GreenNode<Type>>
    val fileItems: DependentQuery<FileHandle, Map<String, MemberList<Type>>>
    val typeParams: DependentQuery<MemberHandle, List<Pair<String, RelativeNodeLoc<Type>>>>
    val childItems: DependentQuery<MemberHandle, Map<String, MemberList<Type>>>
    val constructors: DependentQuery<MultiInstanceClassHandle, List<RelativeNodeLoc<Type>>>
}