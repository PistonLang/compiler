package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.items.handles.ItemHandle
import pistonlang.compiler.common.items.handles.ItemType
import pistonlang.compiler.util.EmptySequence

interface Scope {
    operator fun get(name: String): Sequence<ItemHandle>

    val parent: Scope?
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
            .filter { it.type == ItemType.Package }
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

fun <T> Scope.findNotNull(name: String, mapping: (ItemHandle) -> T?): List<T> {
    var scope: Scope? = this
    var list = emptyList<T>()

    while (scope != null && list.isEmpty()) {
        list = scope[name].mapNotNull(mapping).toList()
        scope = scope.parent
    }

    return list
}
