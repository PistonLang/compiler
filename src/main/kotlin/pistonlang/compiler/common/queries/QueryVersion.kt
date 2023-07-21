package pistonlang.compiler.common.queries

import java.util.concurrent.atomic.AtomicInteger

class QueryVersionData {
    private val atomic: AtomicInteger = AtomicInteger(0)
    val current get() = QueryVersion(atomic.get())

    internal fun update() = atomic.incrementAndGet().let(::QueryVersion)
}

@JvmInline
value class QueryVersion internal constructor(private val version: Int) {
    operator fun compareTo(other: QueryVersion) = version.compareTo(other.version)
}

val firstVersion = QueryVersion(0)