package pistonlang.compiler.common.handles

import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.RelativeNodeLoc

/**
 * An object for referencing a parsed top-level item or sub-item
 * @property type - the type of item
 * @property handler - the handler for the language that item is parsed from
 * @property location - the location relative to [parent]
 * @property parent - a handle to the item's parent, be it a [pistonlang.compiler.common.handles.FileHandle]
 * or an [pistonlang.compiler.common.handles.ItemHandle]
 */
data class ItemReference<Type : SyntaxType>(
    val type: ItemType,
    val handler: LanguageHandler<Type>,
    val location: RelativeNodeLoc<Type>,
    val parent: ParentHandle,
)