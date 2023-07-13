package pistonlang.compiler.common.main

import pistonlang.compiler.common.files.PackageTreeNode
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.queries.*
import java.util.*

class GeneralQueries internal constructor(
    versionData: QueryVersionData,
    private val inputs: InputQueries,
) {
    private val fileHandler: DependentQuery<FileHandle, LanguageHandler<*>?> =
        DependentQuery(versionData) { key: FileHandle ->
            val ext = key.path.substringAfterLast('.')
            inputs.postfixHandler[ext]
        }

    val packageHandleNode: DependentQuery<PackageHandle, PackageTreeNode?> = DependentQuery(
        versionData,
        equalityFun = { old, new -> old?.lastUpdated == new?.lastUpdated },
        computeFn = { key -> inputs.packageTree[Unit].packages[key] }
    )

    val packageItems: DependentQuery<PackageHandle, Map<String, List<ItemHandle>>> =
        DependentQuery(versionData) fn@{ key: PackageHandle ->
            val node = packageHandleNode[key] ?: return@fn emptyMap<String, List<ItemHandle>>()
            val res = mutableMapOf<String, MutableList<ItemHandle>>()
            node.children.forEach { handle ->
                res.getOrPut(handle.suffix) { mutableListOf() }.add(handle)
            }
            node.files.forEach { file ->
                val handler = fileHandler[file] ?: return@forEach
                handler.fileItems[file].forEach { (name, list) ->
                    MemberType.entries.forEach { type ->
                        list.iteratorFor(type).withIndex().forEach { (index, _) ->
                            res.getOrPut(name) { mutableListOf() }.add(type.buildHandle(file, name, index))
                        }
                    }
                }
            }
            res
        }

    val childItems: DependentQuery<MemberHandle, Map<String, List<ItemHandle>>> =
        DependentQuery(versionData) fn@{ key: MemberHandle ->
            val res = mutableMapOf<String, MutableList<ItemHandle>>()
            val handler = fileHandler[key.findFile()] ?: return@fn emptyMap<String, List<ItemHandle>>()
            handler.childItems[key].forEach { (name, list) ->
                MemberType.entries.forEach { type ->
                    list.iteratorFor(type).withIndex().forEach { (index, _) ->
                        res.getOrPut(name) { mutableListOf() }.add(type.buildHandle(key, name, index))
                    }
                }
            }
            res
        }

    val typeParams: DependentQuery<MemberHandle, Map<String, List<TypeParamHandle>>> =
        DependentQuery(versionData) fn@{ key: MemberHandle ->
            val handler = fileHandler[key.findFile()] ?: return@fn emptyMap<String, List<TypeParamHandle>>()
            handler.typeParams[key].withIndex().groupBy({ it.value.first }) {
                TypeParamHandle(key, it.index)
            }
        }

    val constructors: DependentQuery<MultiInstanceClassHandle, List<ConstructorHandle>> =
        DependentQuery(versionData) fn@{ key: MultiInstanceClassHandle ->
            val handler = fileHandler[key.findFile()] ?: return@fn emptyList<ConstructorHandle>()
            handler.constructors[key].indices.map { ConstructorHandle(key, it) }
        }
}

context(QueryAccessor)
fun PackageHandle.hierarchyIterator(queries: GeneralQueries) = object : Iterator<MemberHandle> {
    private var iterStack = Stack<Iterator<MemberHandle>>()
    private var iter = queries
        .packageItems[this@hierarchyIterator]
        .asSequence()
        .flatMap { (_, list) -> list }
        .filterIsInstance<MemberHandle>().iterator()

    override fun hasNext(): Boolean = iter.hasNext()

    private fun findNext(handle: MemberHandle) {
        val next = queries
            .childItems[handle]
            .asSequence()
            .flatMap { (_, list) -> list }
            .filterIsInstance<MemberHandle>()
            .iterator()

        if (next.hasNext()) {
            iterStack.push(iter)
            iter = next
        } else findNext()
    }

    private tailrec fun findNext() {
        if (iter.hasNext() || iterStack.isEmpty()) return
        iter = iterStack.pop()
        findNext()
    }

    override fun next(): MemberHandle {
        val res = iter.next()
        findNext(res)
        return res
    }
}