package pistonlang.compiler.common.items

interface Interner<From, To: Id> {
    operator fun get(from: From): To?

    fun getKey(to: To): From
}

interface MutableInterner<From, To: Id> : Interner<From, To> {
    fun getOrAdd(from: From): To

    fun add(from: From)
}

class MapBasedInterner<From, To: Id>(private val constructor: (Int) -> To): MutableInterner<From, To> {
    private val forward = mutableMapOf<From, To>()
    private val reverse = ArrayList<From>(1024)

    override fun get(from: From) = forward[from]

    override fun getKey(to: To): From = reverse[to.value]

    override fun add(from: From) {
        if (forward.contains(from)) return
        forward[from] = constructor(reverse.size)
        reverse.add(from)
    }

    override fun getOrAdd(from: From): To {
        forward[from]?.let { return it }

        val result = constructor(reverse.size)

        forward[from] = result
        reverse.add(from)

        return result
    }
}

class ListBasedInterner<From: Id, To: Id>(private val constructor: (Int) -> To): MutableInterner<From, To> {
    private val forward = IdList<From, To>(1024)
    private val reverse = ArrayList<From>(1024)

    override fun get(from: From): To? = forward[from]

    override fun getKey(to: To): From  = reverse[to.value]

    override fun add(from: From) {
        if (forward[from] != null) return
        forward[from] = constructor(reverse.size)
        reverse.add(from)
    }

    override fun getOrAdd(from: From): To {
        forward[from]?.let { return it }

        val result = constructor(reverse.size)

        forward[from] = result
        reverse.add(from)

        return result
    }
}