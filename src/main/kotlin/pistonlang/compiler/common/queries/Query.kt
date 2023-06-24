package pistonlang.compiler.common.queries

import java.util.concurrent.ConcurrentHashMap

data class InputQueryValue<V>(val modified: QueryVersion, val value: V)
data class QueryValue<V>(val modified: QueryVersion, val checked: QueryVersion, val value: V)

fun <T> T.toQueryValue(version: QueryVersion) = QueryValue(version, version, this)

class InputQuery<K, V>(private val versionData: QueryVersionData, private val default: () -> V) {
    private val backing: MutableMap<K, InputQueryValue<V>> = ConcurrentHashMap<K, InputQueryValue<V>>()

    operator fun contains(key: K) = backing.contains(key)

    operator fun get(key: K): InputQueryValue<V> = backing
        .getOrPut(key) { InputQueryValue(versionData.current, default()) }

    operator fun set(key: K, value: V): InputQueryValue<V> =
        InputQueryValue(versionData.update(), value).also { backing[key] = it }
}

class Query<K, V>(
    private val versionData: QueryVersionData,
    private val fn: (K, QueryVersion) -> V,
    private val update: (K, QueryValue<V>, QueryVersion) -> QueryValue<V>
) {
    private val backing: MutableMap<K, QueryValue<V>> = ConcurrentHashMap<K, QueryValue<V>>()

    operator fun get(key: K): QueryValue<V> {
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
}