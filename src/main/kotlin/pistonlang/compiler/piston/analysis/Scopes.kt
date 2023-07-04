package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.items.ItemHandle
import pistonlang.compiler.util.EmptyIterator
import pistonlang.compiler.util.SingletonIterator

interface Scope {
    operator fun get(name: String): Iterator<ItemHandle>

    val parent: Scope?
}

object BaseScope : Scope {
    override val parent: Scope?
        get() = null

    override fun get(name: String): Iterator<ItemHandle> = stlItems[name]
        ?.let { SingletonIterator(it) }
        ?: EmptyIterator
}

class StaticScope(override val parent: Scope?, private val map: Map<String, List<ItemHandle>>) : Scope {
    override fun get(name: String): Iterator<ItemHandle> = map[name]?.iterator() ?: EmptyIterator
}

class ImportScope(override val parent: Scope, private val data: ImportData) : Scope {
    override fun get(name: String): Iterator<ItemHandle> {
        val ids = data.nameMap[name] ?: return EmptyIterator

        return ids
            .asSequence()
            .flatMap { index -> data.deps[index].handles.asSequence() }
            .iterator()
    }
}

fun Scope.find(name: String, predicate: (ItemHandle) -> Boolean): List<ItemHandle> {
    var scope: Scope? = this
    var list = emptyList<ItemHandle>()

    while (scope != null && list.isEmpty()) {
        list = scope[name].asSequence().filter(predicate).toList()
        scope = scope.parent
    }

    return list
}
