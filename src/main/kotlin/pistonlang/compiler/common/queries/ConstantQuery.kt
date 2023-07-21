package pistonlang.compiler.common.queries

import pistonlang.compiler.common.items.Id

class ConstantQuery<K: Id, V>(private val constant: V): Query<K, V> {
    context(QueryAccessor) override fun get(key: K): V = constant

    override fun lastModified(key: K): QueryVersion = firstVersion
}