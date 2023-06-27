package pistonlang.compiler.common.items

import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.util.NonEmptyList
import pistonlang.compiler.util.nonEmptyListOf

data class HandleTree<out Type: SyntaxType>(
    val dataList: List<HandleData<Type>>,
    val nodes: List<HandleTreeNode<Type>>,
) {
    fun isEmpty() = nodes.isEmpty()
}

val emptyHandleTree = HandleTree<Nothing>(emptyList(), emptyList())

/**
 * The index of the handle data in a corresponding handle data list
 * -1 is used for invalid indices, everything else is expected to be an actual index
 */
typealias HandleDataIndex = Int

const val invalidIndex: HandleDataIndex = -1

data class HandleTreeNode<out Type : SyntaxType>(
    val fullRange: RelativeNodeLoc<Type>,
    val index: HandleDataIndex,
    val children: List<HandleTreeNode<Type>>
)

data class HandleData<out Type: SyntaxType>(
    val location: RelativeNodeLoc<Type>,
    val handles: NonEmptyList<ItemHandle>,
)

val nullReferenceList = nonEmptyListOf(NullHandle)