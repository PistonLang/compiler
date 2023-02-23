package pistonlang.compiler.util

object EmptyIterator : Iterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun next(): Nothing = error("Called next() on an empty iterator")
}