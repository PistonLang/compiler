package pistonlang.compiler.util.lists

@JvmInline
value class NonEmptyList<out T> internal constructor(private val nested: List<T>) : Iterable<T> {
    override fun iterator(): Iterator<T> = nested.iterator()

    operator fun get(index: Int): T = nested[index]
}

fun <T> List<T>.assertNonEmpty(): NonEmptyList<T> =
    if (isEmpty()) error("The list should not be empty")
    else NonEmptyList(this)

fun <T> nonEmptyListOf(first: T) = NonEmptyList(listOf(first))