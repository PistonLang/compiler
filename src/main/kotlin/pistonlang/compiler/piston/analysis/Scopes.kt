package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.items.ItemHandle
import pistonlang.compiler.common.main.stlItems
import pistonlang.compiler.util.EmptyIterator
import pistonlang.compiler.util.EmptySequence
import pistonlang.compiler.util.SingletonIterator
import pistonlang.compiler.util.SingletonSequence

interface Scope {
    operator fun get(name: String): Sequence<ItemHandle>

    val parent: Scope?
}

data object BaseScope : Scope {
    override val parent: Scope?
        get() = null

    override fun get(name: String): Sequence<ItemHandle> = stlItems[name]
        ?.let { SingletonSequence(it) }
        ?: EmptySequence
}

data class StaticScope(override val parent: Scope?, private val map: Map<String, List<ItemHandle>>) : Scope {
    override fun get(name: String): Sequence<ItemHandle> = map[name]?.asSequence() ?: EmptySequence
}

data class ImportScope(override val parent: Scope, private val data: ImportData) : Scope {
    override fun get(name: String): Sequence<ItemHandle> {
        val ids = data.data[name] ?: return EmptySequence

        return ids
            .asSequence()
            .flatMap { index -> data.dependencies[index].handles.asSequence() }
    }
}

data class StaticTypeScope(override val parent: Scope?, private val map: Map<String, List<ItemHandle>>) : Scope {
    override fun get(name: String): Sequence<ItemHandle> {
        val handles = map[name] ?: return EmptySequence

        return handles
            .asSequence()
            .filter { it.itemType.namespace }
    }
}

fun Scope.find(name: String, predicate: (ItemHandle) -> Boolean): List<ItemHandle> {
    var scope: Scope? = this
    var list = emptyList<ItemHandle>()

    while (scope != null && list.isEmpty()) {
        list = scope[name].filter(predicate).toList()
        scope = scope.parent
    }

    return list
}
