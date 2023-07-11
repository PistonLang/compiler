package pistonlang.compiler.common.queries

import pistonlang.compiler.util.VoidList
import pistonlang.compiler.util.contextMember
import java.util.concurrent.ConcurrentHashMap

sealed interface QueryValue<out V> {
    val modified: QueryVersion
    val value: V
}

data class InputQueryValue<out V>(
    override val modified: QueryVersion,
    override val value: V
) : QueryValue<V>

data class DependentQueryValue<out V>(
    override val modified: QueryVersion,
    val checked: QueryVersion,
    override val value: V,
    val dependencies: List<QueryKey<*, *>>
) : QueryValue<V>

sealed interface Query<in K, out V> {
    context(QueryAccessor)
    operator fun get(key: K): V

    context(QueryAccessor)

    fun lastModified(key: K): QueryVersion
}

class InputQuery<in K, V>(private val versionData: QueryVersionData, private val default: () -> V) : Query<K, V> {
    private val backing: MutableMap<K, InputQueryValue<V>> = ConcurrentHashMap<K, InputQueryValue<V>>()

    operator fun contains(key: K) = backing.contains(key)

    private fun getFull(key: K): InputQueryValue<V> = run {
        backing.getOrPut(key) { InputQueryValue(versionData.current, default()) }
    }

    context(QueryAccessor)
    override fun get(key: K): V = run {
        contextMember<QueryAccessor>().addDependency(QueryKey(this, key))
        getFull(key).value
    }

    context(QueryAccessor)
    override fun lastModified(key: K): QueryVersion =
        getFull(key).modified

    operator fun set(key: K, value: V): QueryVersion {
        val newValue = InputQueryValue(versionData.update(), value)
        backing[key] = newValue
        return newValue.modified
    }
}

class DependentQuery<in K, out V>(
    private val versionData: QueryVersionData,
    private val checkEquality: Boolean = true,
    private val cycleHandler: (K) -> V = { error("Unexpected cycle") },
    private val updateFn: (QueryAccessor.(K, V) -> V)? = null,
    private val fn: QueryAccessor.(K) -> V,
) : Query<K, V> {
    private val backing: MutableMap<K, DependentQueryValue<V>> = ConcurrentHashMap<K, DependentQueryValue<V>>()

    private fun getFull(key: K, accessor: QueryAccessor): DependentQueryValue<V> {
        val version = versionData.current
        val last = backing[key]

        val value = when {
            last == null -> {
                val deps = mutableListOf<QueryKey<*, *>>()
                val newValue = accessor.access(QueryKey(this, key), deps, { cycleHandler(key) }) {
                    fn(key)
                }
                DependentQueryValue(modified = version, checked = version, newValue, deps)
            }

            last.checked >= version || (last.dependencies.isNotEmpty() && accessor.access(QueryKey(this, key), VoidList, { false }) { last.dependencies.all { it.lastModified() <= last.checked } }) ->
                return last

            else -> {
                val deps = mutableListOf<QueryKey<*, *>>()
                val newValue = accessor.access(QueryKey(this, key), deps, { cycleHandler(key) }) {
                    updateFn?.let { it(key, last.value) } ?: fn(key)
                }
                DependentQueryValue(
                    modified = if (checkEquality && newValue == last.value) last.modified else version,
                    checked = version,
                    value = newValue,
                    dependencies = deps
                )
            }
        }

        backing[key] = value
        return value
    }

    context(QueryAccessor)
    override fun lastModified(key: K): QueryVersion =
        getFull(key, contextMember()).modified

    context(QueryAccessor)
    override fun get(key: K): V = run {
        val accessor = contextMember<QueryAccessor>()
        accessor.addDependency(QueryKey(this, key))
        getFull(key, accessor).value
    }
}

data class QueryKey<K, out V>(val query: Query<K, V>, val key: K) {
    context(QueryAccessor)
    fun lastModified() = query.lastModified(key)
}

class QueryAccessor internal constructor(
    internal val running: MutableSet<QueryKey<*, *>>,
    private val dependencies: MutableList<in QueryKey<*, *>>
) {
    internal inline fun <T> access(
        key: QueryKey<*, *>,
        deps: MutableList<in QueryKey<*, *>>,
        onError: () -> T,
        onSuccess: QueryAccessor.() -> T
    ): T {
        if (running.contains(key)) {
            return onError()
        }

        running.add(key)
        val res = QueryAccessor(running, deps).onSuccess()
        running.remove(key)
        return res
    }

    internal fun addDependency(key: QueryKey<*, *>) {
        dependencies.add(key)
    }
}