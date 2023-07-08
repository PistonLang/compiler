package pistonlang.compiler.common.queries

import java.util.concurrent.ConcurrentHashMap

data class InputQueryValue<V>(val modified: QueryVersion, val value: V)
data class QueryValue<V>(val modified: QueryVersion, val checked: QueryVersion, val value: V)

fun <T> T.toQueryValue(version: QueryVersion) = QueryValue(version, version, this)

interface Query<K, V> {
    operator fun get(key: K): V

    fun lastModified(key: K): QueryVersion

}

class InputQuery<K, V>(private val versionData: QueryVersionData, private val default: () -> V): Query<K, V> {
    private val backing: MutableMap<K, InputQueryValue<V>> = ConcurrentHashMap<K, InputQueryValue<V>>()

    operator fun contains(key: K) = backing.contains(key)

    private fun getFull(key: K): InputQueryValue<V> = backing
        .getOrPut(key) { InputQueryValue(versionData.current, default()) }

    override fun get(key: K): V = getFull(key).value

    override fun lastModified(key: K): QueryVersion = getFull(key).modified

    operator fun set(key: K, value: V): InputQueryValue<V> =
        InputQueryValue(versionData.update(), value).also { backing[key] = it }
}

class DependentQuery<K, V>(
    private val versionData: QueryVersionData,
    private val fn: (K, QueryVersion) -> V,
    private val update: (K, QueryValue<V>, QueryVersion) -> QueryValue<V>
): Query<K, V> {
    private val backing: MutableMap<K, QueryValue<V>> = ConcurrentHashMap<K, QueryValue<V>>()

    private fun getFull(key: K): QueryValue<V> {
        val version = versionData.current
        val last = backing[key]

        val value = when {
            last == null -> QueryValue(version, version, fn(key, version))
            last.checked < version -> update(key, last, version)
            else -> return last
        }

        backing[key] = value
        return value
    }

    override fun lastModified(key: K): QueryVersion = getFull(key).modified

    override fun get(key: K): V = getFull(key).value
}