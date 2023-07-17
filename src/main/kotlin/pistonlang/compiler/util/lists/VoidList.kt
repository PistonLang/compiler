package pistonlang.compiler.util.lists

import pistonlang.compiler.util.EmptyIterator

data object VoidList : MutableList<Any> {
    override val size: Int
        get() = 0

    override fun add(element: Any): Boolean = false

    override fun clear() {
    }

    override fun add(index: Int, element: Any) {

    }

    override fun addAll(elements: Collection<Any>): Boolean = false

    override fun addAll(index: Int, elements: Collection<Any>): Boolean = false

    override fun contains(element: Any): Boolean = false

    override fun containsAll(elements: Collection<Any>): Boolean = false

    override fun get(index: Int): Nothing = error("Tried to access an element from a void list")

    override fun indexOf(element: Any): Int = -1

    override fun isEmpty(): Boolean = true

    override fun iterator(): MutableIterator<Any> = EmptyIterator

    override fun lastIndexOf(element: Any): Int = -1

    override fun listIterator(): MutableListIterator<Any> = EmptyListIterator

    override fun listIterator(index: Int): MutableListIterator<Any> = EmptyListIterator

    override fun remove(element: Any): Boolean = false

    override fun removeAll(elements: Collection<Any>): Boolean = false

    override fun removeAt(index: Int): Nothing = error("Tried to remove an element of a void list")

    override fun retainAll(elements: Collection<Any>): Boolean = false

    override fun set(index: Int, element: Any): Nothing = error("Tried to set an element of a void list")

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Any> = VoidList
}

private object EmptyListIterator : MutableListIterator<Any> {
    override fun add(element: Any) {
    }

    override fun hasNext(): Boolean = false

    override fun next(): Any = error("Tried to get the next element of an empty iterator")

    override fun hasPrevious(): Boolean = false

    override fun nextIndex(): Int = 0

    override fun previous(): Any = error("Tried to get the next element of an empty iterator")

    override fun previousIndex(): Int = 0

    override fun remove() {
    }

    override fun set(element: Any) {
    }
}