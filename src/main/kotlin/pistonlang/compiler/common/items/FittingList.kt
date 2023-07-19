package pistonlang.compiler.common.items

class IdList<Index: Id, Value>(initialCapacity: Int = 10) {
    @Suppress("UNCHECKED_CAST")
    private var backing = arrayOfNulls<Any>(initialCapacity) as Array<Value?>

    var size: Int = 0
        private set

    private fun expandToFit(index: Int) {
        if (index < backing.size) return

        val newSize = 1 shl 32 - index.countLeadingZeroBits()
        backing = backing.copyOf(newSize)
    }

    operator fun get(index: Index): Value? {
        expandToFit(index.value)

        return backing[index.value]
    }

    operator fun set(index: Index, value: Value) {
        expandToFit(index.value)

        backing[index.value] = value
    }
}