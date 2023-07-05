package pistonlang.compiler.common.items

import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.util.NonEmptyList
import pistonlang.compiler.util.nonEmptyListOf

data class HandleData<out Type: SyntaxType>(
    val location: RelativeNodeLoc<Type>,
    val handles: NonEmptyList<ItemHandle>,
)

val nullReferenceList = nonEmptyListOf(ErrorHandle)

typealias DependencyList<Type> = List<HandleData<Type>>

data class Dependent<Type: SyntaxType, Data>(val dependencies: DependencyList<Type>, val data: Data)