package pistonlang.compiler.common.queries

import pistonlang.compiler.common.items.Id
import pistonlang.compiler.common.items.UnitId


sealed interface Query<in K : Id, out V> {
    context(QueryAccessor)
    operator fun get(key: K): V

    fun lastModified(key: K): QueryVersion
}

sealed interface SingletonQuery<out V> : Query<UnitId, V> {
    context(QueryAccessor)
    val value: V
    val lastModified: QueryVersion
    context(QueryAccessor) override fun get(key: UnitId): V = value
    override fun lastModified(key: UnitId): QueryVersion = lastModified
}

data class QueryKey<K : Id, out V>(val query: Query<K, V>, val key: K) {
    fun lastModified() = query.lastModified(key)
}

@JvmInline
value class QueryAccessor internal constructor(private val dependencies: MutableList<in QueryKey<*, *>>) {
    internal fun addDependency(key: QueryKey<*, *>) {
        dependencies.add(key)
    }
}