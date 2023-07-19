package pistonlang.compiler.util.lists

@JvmInline
value class NonEmptyList<out T> internal constructor(private val nested: List<T>) : List<T> by nested {
    override fun isEmpty(): Boolean = false
}


fun <T> List<T>.assertNonEmpty(): NonEmptyList<T> =
    if (isEmpty()) error("The list should not be empty")
    else NonEmptyList(this)

fun <T> nonEmptyListOf(first: T) = NonEmptyList(listOf(first))