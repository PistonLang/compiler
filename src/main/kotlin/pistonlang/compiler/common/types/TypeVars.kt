package pistonlang.compiler.common.types

import java.util.concurrent.atomic.AtomicInteger

@JvmInline
value class TypeVar @PublishedApi internal constructor(val id: Int)

internal class TypeVarFactory {
    private val num = AtomicInteger(0)

    fun next() = TypeVar(num.getAndIncrement())
}

typealias TypeVarMap = Map<TypeVar, TypeInstance>