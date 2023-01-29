package pistonlang.compiler.common.utl

import java.lang.IllegalArgumentException

fun <T> bufferListOf(vararg items: T): BufferList<T> {
    val res = BufferList<T>()
    items.forEach { res.add(it) }
    return res
}

@Suppress("UNCHECKED_CAST")
class BufferList<T>(private var cap: Int = 1000) {
    private var backing = arrayOfNulls<Any>(cap) as Array<T>

    var size: Int = 0
        private set

    fun removeRange(start: Int, end: Int) {
        val diff = size - end

        for (idx in 0 until diff)
            set(idx + start, get(end + idx))

        size = start + diff
    }

    private fun expand() {
        cap *= 2
        val new = arrayOfNulls<Any>(cap) as Array<T>
        backing.copyInto(new)
        backing = new
    }

    val lastIndex get() = size - 1

    fun contains(element: T): Boolean = backing.indexOf(element) in 0..lastIndex

    fun containsAll(elements: Collection<T>): Boolean = elements.all(this::contains)

    operator fun get(index: Int): T =
        if (index !in 0..lastIndex) throw IllegalArgumentException()
        else backing[index]

    operator fun set(index: Int, item: T) {
        backing[index] = item
    }

    fun last() = get(lastIndex)

    fun indexOf(element: T): Int {
        val back = backing.indexOf(element)
        return if (back < size) back else -1
    }

    fun isEmpty(): Boolean = size != 0

    fun add(element: T) {
        if (cap == size) expand()
        backing[size] = element
        size += 1
    }
}