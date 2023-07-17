package pistonlang.compiler.common.queries

import pistonlang.compiler.common.items.Id
import pistonlang.compiler.common.items.IdList
import pistonlang.compiler.common.items.UnitId
import pistonlang.compiler.util.contextMember
import java.util.concurrent.ConcurrentHashMap

data class InputQueryValue<out V>(
    val modified: QueryVersion,
    val value: V
)

sealed interface DependentQueryValue<out V> {
    fun computed(): ComputedQueryValue<V>?
}

data class ComputedQueryValue<out V>(
    val modified: QueryVersion,
    val checked: QueryVersion,
    val value: V,
    val dependencies: List<QueryKey<*, *>>
) : DependentQueryValue<V> {
    override fun computed(): ComputedQueryValue<V> = this
}

data object InProgressQueryValue : DependentQueryValue<Nothing> {
    override fun computed(): ComputedQueryValue<Nothing>? = null
}


sealed interface Query<in K: Id, out V> {
    context(QueryAccessor)
    operator fun get(key: K): V


    fun lastModified(key: K): QueryVersion
}

class SingletonInputQuery<V>(private val versionData: QueryVersionData, starting: V) : Query<UnitId, V> {
    private var updated = versionData.current

    var value = starting
        set(new) {
            updated = versionData.current
            field = new
        }

    context(QueryAccessor)
    override fun get(key: UnitId): V = run {
        contextMember<QueryAccessor>().addDependency(QueryKey(this, UnitId))
        value
    }

    override fun lastModified(key: UnitId): QueryVersion = updated
}

class InputQuery<in K: Id, V>(private val versionData: QueryVersionData, private val default: () -> V) : Query<K, V> {
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

class DependentQuery<in K: Id, out V>(
    private val versionData: QueryVersionData,
    private val equalityFun: (old: V, new: V) -> Boolean = { a, b -> a == b },
    private val cycleHandler: (key: K) -> V = { error("Unexpected cycle") },
    private val updateFn: (QueryAccessor.(key: K, oldValue: V) -> V)? = null,
    private val computeFn: QueryAccessor.(key: K) -> V,
) : Query<K, V> {
    private val backing: IdList<K, DependentQueryValue<V>> = IdList()

    private fun getFull(key: K): ComputedQueryValue<V> = run nested@{
        val version = versionData.current

        // If this is the first computation just compute the value and store its dependencies
        val last = backing[key] ?: return@nested run {
            val deps = mutableListOf<QueryKey<*, *>>()
            val newValue = with(QueryAccessor(deps)) { computeFn(key) }
            ComputedQueryValue(modified = version, checked = version, newValue, deps)
        }

        // If it is already being computed we've hit a cycle
        val lastComputed = last.computed() ?: return@nested run {
            val value = cycleHandler(key)
            ComputedQueryValue(modified = version, checked = version, value, emptyList())
        }

        // If we've already checked the value this version, return it
        if (lastComputed.checked >= version)
            return lastComputed

        // Set the value as currently being computed to catch cycles
        backing[key] = InProgressQueryValue

        // If none of the dependencies have changed, we just updated the version checked
        if (lastComputed.dependencies.isNotEmpty() && lastComputed.dependencies.all { it.lastModified() <= lastComputed.checked }) {
            lastComputed.copy(checked = version)
        } else {
            // The only option left is to recompute or update the value
            val deps = mutableListOf<QueryKey<*, *>>()
            val newValue = with(QueryAccessor(deps)) {
                updateFn?.let { it(key, lastComputed.value) } ?: computeFn(key)
            }

            // Check if we've already hit a cycle
            backing[key]!!.computed()?.let { return it }

            ComputedQueryValue(
                modified = if (equalityFun(newValue, lastComputed.value)) lastComputed.modified else version,
                checked = version,
                value = newValue,
                dependencies = deps
            )
        }
    }.also { backing[key] = it }

    override fun lastModified(key: K): QueryVersion =
        getFull(key).modified

    context(QueryAccessor)
    override fun get(key: K): V = run {
        val accessor = contextMember<QueryAccessor>()
        accessor.addDependency(QueryKey(this, key))
        getFull(key).value
    }
}

data class QueryKey<K: Id, out V>(val query: Query<K, V>, val key: K) {
    fun lastModified() = query.lastModified(key)
}

@JvmInline
value class QueryAccessor internal constructor(private val dependencies: MutableList<in QueryKey<*, *>>) {
    internal fun addDependency(key: QueryKey<*, *>) {
        dependencies.add(key)
    }
}