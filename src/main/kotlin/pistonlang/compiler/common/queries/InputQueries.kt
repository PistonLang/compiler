package pistonlang.compiler.common.queries

import pistonlang.compiler.common.items.Id
import pistonlang.compiler.common.items.IdList
import pistonlang.compiler.common.items.UnitId
import pistonlang.compiler.util.contextMember

private data class InputQueryValue<out V>(
    val modified: QueryVersion,
    val value: V
)

class InputQuery<in K : Id, V>(private val versionData: QueryVersionData, private val default: () -> V) : Query<K, V> {
    private val backing: IdList<K, InputQueryValue<V>> = IdList()

    operator fun contains(key: K) = backing[key] != null

    private fun getFull(key: K): InputQueryValue<V> = backing[key] ?: run {
        val result = InputQueryValue(versionData.current, default())
        backing[key] = result
        result
    }

    context(QueryAccessor)
    override fun get(key: K): V = run {
        contextMember<QueryAccessor>().addDependency(QueryKey(this, key))
        getFull(key).value
    }

    override fun lastModified(key: K): QueryVersion =
        getFull(key).modified

    operator fun set(key: K, value: V) {
        backing[key] = InputQueryValue(versionData.current, value)
    }
}

class SingletonInputQuery<V>(private val versionData: QueryVersionData, starting: V) : SingletonQuery<V> {
    override var lastModified = versionData.current
        private set


    private var backing: V = starting

    fun update(new: V) {
        lastModified = versionData.current
        backing = new
    }

    context(QueryAccessor) override val value: V
        get() {
            contextMember().addDependency(QueryKey(this, UnitId))
            return backing
        }
}