package pistonlang.compiler.common.language

import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.common.queries.Query
import pistonlang.compiler.common.types.TypeInstance
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.lists.NonEmptyList

/**
 * A [LanguageHandler] handles language specific queries, as opposed to
 * a [pistonlang.compiler.common.main.CompilerInstance] which handles general queries
 */
interface LanguageHandler<out Type : SyntaxType> {
    /**
     * The unique extension this handler is made for
     */
    val extension: String

    /**
     * List of items contained in a file
     */
    val fileItems: Query<FileId, MemberList<Type>>

    /**
     * List of type params
     */
    val typeParams: Query<MemberId, List<Pair<String, RelativeNodeLoc<Type>>>>

    /**
     * List of children of a given item
     */
    val childItems: Query<MemberId, MemberList<Type>>

    /**
     * List of constructors of a class
     */
    val constructors: Query<TypeId, List<RelativeNodeLoc<Type>>>

    /**
     * List of supertypes of a declared type
     */
    val supertypes: Query<TypeId, SupertypeData<Type>>

    /**
     * List of type parameter bounds
     */
    val typeParamBounds: Query<MemberId, TypeBoundData<Type>>

    /**
     * The types of a function or setter's parameters
     * This is expected to return an empty list for other types of handles
     */
    val params: Query<MemberId, ParamData<Type>>

    /**
     * The return type of a function, getter, setter; the type of a val or var
     * It may return any type instance for type definitions
     */
    val returnType: Query<MemberId, ReturnData<Type>>

    /**
     * Whether a function or property is implemented
     * It may return anything for types
     */
    val isImplemented: Query<MemberId, Boolean>
}