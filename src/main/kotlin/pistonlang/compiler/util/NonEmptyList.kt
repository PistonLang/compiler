package pistonlang.compiler.util

@JvmInline
value class NonEmptyList<out T> internal constructor(private val nested: List<T>): Iterable<T> {
    override fun iterator(): Iterator<T> = nested.iterator()

    operator fun get(index: Int): T = nested[index]
}

fun <T> List<T>.assertNonEmpty(): NonEmptyList<T> =
    if (isEmpty()) error("The list should not be empty")
    else NonEmptyList(this)

fun <T> nonEmptyListOf(first: T) = NonEmptyList(listOf(first))
fun <T> nonEmptyListOf(first: T, vararg rest: T) = NonEmptyList(listOf(first, *rest))