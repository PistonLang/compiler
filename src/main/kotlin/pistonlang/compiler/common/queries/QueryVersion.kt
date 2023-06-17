package pistonlang.compiler.common.queries

import java.util.concurrent.atomic.AtomicInteger

@JvmInline
value class QueryVersionData(private val atomic: AtomicInteger = AtomicInteger(0)) {
    val current get() = QueryVersion(atomic.get())

    fun update() = atomic.getAndIncrement().let(::QueryVersion)
}

@JvmInline
value class QueryVersion internal constructor(private val version: Int) {
    operator fun compareTo(other: QueryVersion) = version.compareTo(other.version)
}