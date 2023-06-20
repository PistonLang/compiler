package pistonlang.compiler.common.main

import kotlinx.collections.immutable.persistentMapOf
import pistonlang.compiler.common.handles.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.queries.InputQuery
import pistonlang.compiler.common.queries.Query
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.common.queries.toQueryValue
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

typealias CodeQuery = InputQuery<FileHandle, String>
typealias OptionsQuery = InputQuery<Unit, CompilerOptions>

class CompilerInstance(val versionData: QueryVersionData) {
    val code: CodeQuery = CodeQuery(versionData) { "" }
    val options: OptionsQuery = OptionsQuery(versionData) { CompilerOptions("") }
    private val changes: Queue<FileChange> = ConcurrentLinkedQueue()
    private val handlers: MutableMap<String, LanguageHandler<*>> = hashMapOf()

    fun <T : SyntaxType> addHandler(handler: LanguageHandler<T>) {
        handler.extensions.forEach { ext ->
            handlers[ext] = handler
        }
    }

    private val fileHandler: Query<FileHandle, LanguageHandler<*>?> = run {
        val handleFun = { key: FileHandle ->
            val ext = key.path.substringAfterLast('.')
            handlers[ext]
        }
        Query(versionData, handleFun) { _, old, version ->
            old.copy(checked = version)
        }
    }

    private val filePackage: Query<FileHandle, PackageHandle> = run {
        val packFun = { key: FileHandle ->
            val ops = options[Unit]
            val path = key.path.removePrefix(ops.value.startPath).split('/').dropLast(1)
            PackageHandle(path)
        }
        Query(versionData, packFun) { key, old, version ->
            val ops = options[Unit]
            if (ops.modified <= old.checked) old.copy(checked = version)
            else packFun(key).toQueryValue(version)
        }
    }

    // TODO: Handle option changes
    val packageTree: Query<Unit, PackageTree> = run {
        val default = { _: Unit ->
            var tree = PackageTree(PackageHandle(emptyList()), persistentMapOf(), emptyList())
            while (changes.isNotEmpty()) {
                tree = applyFileChange(tree, changes.poll())
            }
            tree
        }
        Query(versionData, default) { _, old, version ->
            var tree = old.value
            while (changes.isNotEmpty() && changes.peek().version <= version) {
                tree = applyFileChange(tree, changes.poll())
            }
            tree.toQueryValue(version)
        }
    }

    private fun applyFileChange(tree: PackageTree, change: FileChange): PackageTree =
        when (change.type) {
            ChangeType.Addition -> {
                val pack = filePackage[change.file]
                tree.add(pack.value, change.file)
            }

            ChangeType.Update -> tree

            ChangeType.Removal -> {
                val pack = filePackage[change.file]
                tree.remove(pack.value, change.file)
            }
        }

    val packageItems: Query<PackageHandle, Map<String, List<ReferencableHandle>>> = run {
        val collectFn = fn@{ key: PackageHandle ->
            val node = packageTree[Unit].value.nodeFor(key) ?: return@fn emptyMap<String, List<ReferencableHandle>>()
            val res = mutableMapOf<String, MutableList<ReferencableHandle>>()
            node.children.forEach { (name, _) ->
                res.getOrPut(name) { mutableListOf() }.add(PackageHandle(key.path + name))
            }
            node.files.forEach { file ->
                val handler = fileHandler[file].value ?: return@forEach
                handler.fileItems[file].value.forEach { (name, list) ->
                    ItemType.values().forEach { type ->
                        list.iteratorFor(type).withIndex().forEach { (index, value) ->
                            res.getOrPut(name) { mutableListOf() }.add(ItemHandle(value.parent, name, type, index))
                        }
                    }
                }
            }
            res
        }
        Query(versionData, collectFn) { handle, old, version ->
            val new = collectFn(handle)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    fun addFile(handle: FileHandle, code: String) {
        changes.offer(FileChange(handle, ChangeType.Addition, this.code.set(handle, code).modified))
    }

    // TODO: Handle incrementally
    fun updateFile(handle: FileHandle, code: String) {
        changes.offer(FileChange(handle, ChangeType.Update, this.code.set(handle, code).modified))
    }

    fun removeFile(handle: FileHandle) {
        changes.offer(FileChange(handle, ChangeType.Removal, code.set(handle, "").modified))
    }
}