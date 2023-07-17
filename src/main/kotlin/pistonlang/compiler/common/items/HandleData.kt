package pistonlang.compiler.common.items

import pistonlang.compiler.common.items.handles.HandleError
import pistonlang.compiler.common.items.handles.ItemHandle
import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.util.lists.NonEmptyList
import pistonlang.compiler.util.lists.nonEmptyListOf

data class HandleData<out Type : SyntaxType>(
    val location: RelativeNodeLoc<Type>,
    val handles: NonEmptyList<ItemHandle>,
)

val invalidPathHandleList = nonEmptyListOf(ItemHandle(HandleError.InvalidPathToken))

typealias DependencyList<Type> = List<HandleData<Type>>

data class Dependent<Type : SyntaxType, Data>(val dependencies: DependencyList<Type>, val data: Data)