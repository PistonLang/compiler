package pistonlang.compiler.common.queries

import java.util.concurrent.ConcurrentHashMap

data class InputQueryValue<V>(val modified: QueryVersion, val value: V)
data class QueryValue<V>(val modified: QueryVersion, val checked: QueryVersion, val value: V)

fun <T> T.toQueryValue(version: QueryVersion) = QueryValue(version, version, this)

class InputQuery<K, V>(private val versionData: QueryVersionData, val default: () -> V) {
    private val backing = ConcurrentHashMap<K, InputQueryValue<V>>()

    operator fun get(key: K): InputQueryValue<V> = backing.getOrPut(key) {
        InputQueryValue(versionData.current, default())
    }

    operator fun set(key: K, value: V): InputQueryValue<V> =
        InputQueryValue(versionData.update(), value).also { backing[key] = it }
}

class Query<K, V>(
    private val versionData: QueryVersionData,
    val fn: (K) -> V,
    val update: (K, QueryValue<V>, QueryVersion) -> QueryValue<V>
) {
    private val backing = ConcurrentHashMap<K, QueryValue<V>>()

    operator fun get(key: K): QueryValue<V> {
        val ver = versionData.current
        val current = backing.getOrPut(key) {
            QueryValue(ver, ver, fn(key))
        }

        if (current.checked < ver) {
            val new = update(key, current, ver)
            backing[key] = new
            return new
        }

        return current
    }
}