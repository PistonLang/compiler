package pistonlang.compiler.common.items

enum class ItemType(
    val type: Boolean = false,
    val callable: Boolean = false,
    val value: Boolean = false,
    val mutable: Boolean = false,
    val namespace: Boolean = type,
) {
    MultiInstanceClass(type = true, callable = true),
    SingletonClass(type = true, value = true),
    Trait(type = true),
    Val(value = true),
    Var(value = true, mutable = true),
    Function(callable = true),
    Getter(value = true),
    Setter(mutable = true),
    Constructor(callable = true),
    TypeParam(type = true),
    Package(namespace = true),
    Null(type = true, callable = true, value = true, mutable = true, namespace = true)
}