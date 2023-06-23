package pistonlang.compiler.common.items

import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.util.NonEmptyList
import pistonlang.compiler.util.nonEmptyListOf

data class ReferenceTree<out Type: SyntaxType>(
    val dataList: List<ReferenceData<Type>>,
    val nodes: List<ReferenceTreeNode<Type>>,
) {
    fun isEmpty() = nodes.isEmpty()
}

val emptyReferenceTree = ReferenceTree<Nothing>(emptyList(), emptyList())

/**
 * The index of the reference data in a corresponding reference data list
 * -1 is used for invalid indices, everything else is expected to be an actual index
 */
typealias ReferenceDataIndex = Int

const val invalidIndex: ReferenceDataIndex = -1

data class ReferenceTreeNode<out Type : SyntaxType>(
    val fullRange: RelativeNodeLoc<Type>,
    val index: ReferenceDataIndex,
    val children: List<ReferenceTreeNode<Type>>
)

data class ReferenceData<out Type: SyntaxType>(
    val location: RelativeNodeLoc<Type>,
    val references: NonEmptyList<UsableReference>,
)

val nullReferenceList = nonEmptyListOf(NullReference)