package pistonlang.compiler.common.main

import pistonlang.compiler.common.files.ChangeType
import pistonlang.compiler.common.files.FileChange
import pistonlang.compiler.common.files.FileData
import pistonlang.compiler.common.files.PackageTree
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.queries.DependentQuery
import pistonlang.compiler.common.queries.QueryAccessor
import pistonlang.compiler.common.queries.QueryVersion
import pistonlang.compiler.common.queries.QueryVersionData
import java.util.*

class GeneralQueries(versionData: QueryVersionData, handlers: Map<String, LanguageHandler<*>>, changes: Queue<FileChange>) {
    val code: CodeQuery = CodeQuery(versionData) { FileData(false, "") }
    private val options: OptionsQuery = OptionsQuery(versionData) { CompilerOptions("") }

    private val fileHandler: DependentQuery<FileHandle, LanguageHandler<*>?> =
        DependentQuery(versionData) { key: FileHandle, _: QueryVersion ->
            val ext = key.path.substringAfterLast('.')
            handlers[ext]
        }

    val filePackage: DependentQuery<FileHandle, PackageHandle> =
        DependentQuery(versionData) { key: FileHandle, _: QueryVersion ->
            val ops = options[Unit]
            val path = key.path.removePrefix(ops.startPath).split('/').dropLast(1)
            PackageHandle(path)
        }

    val packageTree: DependentQuery<Unit, PackageTree> = DependentQuery(versionData) { _: Unit, version: QueryVersion ->
        var tree = PackageTree(PackageHandle(emptyList()), version)
        while (changes.isNotEmpty()) {
            val change = changes.poll()
            val pack = filePackage[change.file]

            tree = when (change.type) {
                ChangeType.Addition -> tree.add(pack, change.file, change.version)

                ChangeType.Update -> tree.update(pack, change.file, change.version)

                ChangeType.Removal -> tree.remove(pack, change.file, change.version)
            }
        }
        tree
    }


    val packageItems: DependentQuery<PackageHandle, Map<String, List<ItemHandle>>> =
        DependentQuery(versionData) fn@{ key: PackageHandle, _: QueryVersion ->
            val node = packageTree[Unit].nodeFor(key) ?: return@fn emptyMap<String, List<ItemHandle>>()
            val res = mutableMapOf<String, MutableList<ItemHandle>>()
            node.children.forEach { (name, node) ->
                if (node.isValid) res
                    .getOrPut(name) { mutableListOf() }
                    .add(PackageHandle(key.path + name))
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
        DependentQuery(versionData) fn@{ key: MemberHandle, _: QueryVersion ->
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
        DependentQuery(versionData) fn@{ key: MemberHandle, _: QueryVersion ->
            val handler = fileHandler[key.findFile()] ?: return@fn emptyMap<String, List<TypeParamHandle>>()
            handler.typeParams[key].withIndex().groupBy({ it.value.first }) {
                TypeParamHandle(key, it.index)
            }
        }

    val constructors: DependentQuery<MultiInstanceClassHandle, List<ConstructorHandle>> =
        DependentQuery(versionData) fn@{ key: MultiInstanceClassHandle, _: QueryVersion ->
            val handler = fileHandler[key.findFile()] ?: return@fn emptyList<ConstructorHandle>()
            handler.constructors[key].indices.map { ConstructorHandle(key, it) }
        }
}

context(QueryAccessor)
fun PackageHandle.hierarchyIterator(queries: GeneralQueries) =
    object : Iterator<MemberHandle> {
        private var iterStack = Stack<Iterator<MemberHandle>>()
        private var iter = queries.packageItems[this@hierarchyIterator]
            .asSequence()
            .flatMap { (_, list) -> list }
            .filterIsInstance<MemberHandle>()
            .iterator()

        override fun hasNext(): Boolean = iter.hasNext()

        private fun findNext(handle: MemberHandle) {
            val next = queries.childItems[handle]
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