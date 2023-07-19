package pistonlang.compiler.common.queries

import pistonlang.compiler.common.items.Id
import pistonlang.compiler.common.items.IdList
import pistonlang.compiler.common.items.UnitId
import pistonlang.compiler.util.contextMember

private sealed interface DependentQueryValue<out V> {
    fun computed(): ComputedQueryValue<V>?
}

private data class ComputedQueryValue<out V>(
    val modified: QueryVersion,
    val checked: QueryVersion,
    val value: V,
    val dependencies: List<QueryKey<*, *>>
) : DependentQueryValue<V> {
    override fun computed(): ComputedQueryValue<V> = this
}

private data object InProgressQueryValue : DependentQueryValue<Nothing> {
    override fun computed(): ComputedQueryValue<Nothing>? = null
}

class DependentQuery<in K : Id, out V>(
    private val versionData: QueryVersionData,
    private val equalityFun: (old: V, new: V) -> Boolean = { a, b -> a == b },
    private val cycleHandler: (key: K) -> V = { throw QueryCycleException() },
    private val updateFn: (QueryAccessor.(key: K, oldValue: V) -> V)? = null,
    private val computeFn: QueryAccessor.(key: K) -> V,
) : Query<K, V> {
    private val backing: IdList<K, DependentQueryValue<V>> = IdList()

    private fun getFull(key: K): ComputedQueryValue<V> = run nested@{
        val version = versionData.current

        // If this is the first computation just compute the value and store its dependencies
        val last = backing[key] ?: return@nested run {
            backing[key] = InProgressQueryValue
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
        if (lastComputed.dependencies.all { it.lastModified() <= lastComputed.checked }) {
            lastComputed.copy(checked = version)
        } else {
            // The only option left is to recompute or update the value
            val deps = mutableListOf<QueryKey<*, *>>()
            val newValue = with(QueryAccessor(deps)) {
                updateFn?.let { it(key, lastComputed.value) } ?: computeFn(key)
            }

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

class DependentSingletonQuery<out V>(
    private val versionData: QueryVersionData,
    private val equalityFun: (old: V, new: V) -> Boolean = { a, b -> a == b },
    private val computeFn: QueryAccessor.() -> V,
) : SingletonQuery<V> {
    private var backing: ComputedQueryValue<V>? = null

    private fun getFull(): ComputedQueryValue<V> = run nested@{
        val version = versionData.current
        val last = backing

        if (last == null) {
            val newDeps = mutableListOf<QueryKey<*, *>>()
            val newValue = with(QueryAccessor(newDeps)) { computeFn() }
            return@nested ComputedQueryValue(version, version, newValue, newDeps)
        }

        if (last.checked >= version)
            return@nested last

        if (last.dependencies.all { it.lastModified() <= version }) {
            return last.copy(checked = version)
        }

        val newDeps = mutableListOf<QueryKey<*, *>>()
        val newValue = with(QueryAccessor(newDeps)) { computeFn() }
        ComputedQueryValue(
            modified = if (equalityFun(last.value, newValue)) last.modified else version,
            checked = version,
            value = newValue,
            dependencies = newDeps
        )
    }.also { backing = it }

    override val lastModified: QueryVersion
        get() = getFull().modified

    context(QueryAccessor) override val value: V
        get() {
            addDependency(QueryKey(this, UnitId))
            return getFull().value
        }
}