package pistonlang.compiler.common.items

import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.parser.RelativeNodeLoc

/**
 * An object for referencing a parsed top-level item or sub-item
 * @property type - the type of item
 * @property handler - the handler for the language that item is parsed from
 * @property location - the location relative to [parent]
 * @property parent - a reference to the item's parent, be it a
 * [pistonlang.compiler.common.items.FileReference] or an [pistonlang.compiler.common.items.ItemReference]
 */
data class ItemNodeReference<Type : SyntaxType>(
    val type: ItemType,
    val handler: LanguageHandler<Type>,
    val location: RelativeNodeLoc<Type>,
    val parent: ParentReference,
)

enum class ItemType(val type: Boolean = false, val callable: Boolean = false, val value: Boolean = false, val mutable: Boolean = false) {
    MultiInstanceClass(type = true, callable = true),
    SingletonClass(type = true, value = true),
    Trait(type = true),
    Val(value = true),
    Var(value = true, mutable = true),
    Function(callable = true),
    Getter(value = true),
    Setter(mutable = true)
}