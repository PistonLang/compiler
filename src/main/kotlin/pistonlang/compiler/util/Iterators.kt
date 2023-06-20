package pistonlang.compiler.util

object EmptyIterator : Iterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun next(): Nothing = error("Called next() on an empty iterator")
}

inline fun <T> Iterator<T>.findFirst(fn: (T) -> Boolean): T? {
    while (hasNext()) {
        val curr = next()
        if (fn(curr)) return curr
    }

    return null
}