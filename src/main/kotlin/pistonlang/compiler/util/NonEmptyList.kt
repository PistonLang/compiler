package pistonlang.compiler.util

@JvmInline
value class NonEmptyList<out T> internal constructor(val list: List<T>)

fun <T> List<T>.assertNonEmpty(): NonEmptyList<T> =
    if (isEmpty()) error("The list should not be empty")
    else NonEmptyList(this)

fun <T> nonEmptyListOf(first: T, vararg rest: T) = NonEmptyList(listOf(first, *rest))