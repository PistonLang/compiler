package pistonlang.compiler.util

object EmptyIterator : Iterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun next(): Nothing = error("Called next() on an empty iterator")
}

class SingletonIterator<T>(val value: T) : Iterator<T> {
    private var used: Boolean = false

    override fun next(): T {
        if (used) error("Value of singleton iterator already used")
        used = true
        return value
    }

    override fun hasNext(): Boolean = !used
}

inline fun <T> Iterator<T>.findFirst(fn: (T) -> Boolean): T? {
    while (hasNext()) {
        val curr = next()
        if (fn(curr)) return curr
    }

    return null
}

object EmptySequence : Sequence<Nothing> {
    override fun iterator(): Iterator<Nothing> = EmptyIterator
}

class SingletonSequence<T>(value: T) : Sequence<T> {
    private val iter = SingletonIterator(value)
    override fun iterator() = iter
}