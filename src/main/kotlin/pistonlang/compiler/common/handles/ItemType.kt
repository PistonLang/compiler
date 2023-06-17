package pistonlang.compiler.common.handles

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