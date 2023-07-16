package pistonlang.compiler.common.language

import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.common.queries.Query
import pistonlang.compiler.common.types.TypeInstance
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.NonEmptyList

/**
 * A [LanguageHandler] handles language specific queries, as opposed to
 * a [pistonlang.compiler.common.main.CompilerInstance] which handles general queries
 */
interface LanguageHandler<Type : SyntaxType> {
    /**
     * List of file extensions that correspond to this handler
     */
    val extensions: List<String>

    /**
     * List of items contained in a file
     */
    val fileItems: Query<FileHandle, MemberList<Type>>

    /**
     * List of type params
     */
    val typeParams: Query<MemberHandle, List<Pair<String, RelativeNodeLoc<Type>>>>

    /**
     * List of children of a given item
     */
    val childItems: Query<MemberHandle, MemberList<Type>>

    /**
     * List of constructors of a class
     */
    val constructors: Query<MultiInstanceClassHandle, List<RelativeNodeLoc<Type>>>

    /**
     * List of supertypes of a declared type
     */
    val supertypes: Query<NewTypeHandle, Dependent<PistonType, NonEmptyList<TypeInstance>>>
}