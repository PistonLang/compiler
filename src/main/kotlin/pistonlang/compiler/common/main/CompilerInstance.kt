package pistonlang.compiler.common.main

import pistonlang.compiler.common.files.ChangeType
import pistonlang.compiler.common.files.FileChange
import pistonlang.compiler.common.files.FileData
import pistonlang.compiler.common.files.PackageTree
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.queries.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

typealias CodeQuery = InputQuery<FileReference, FileData>
typealias OptionsQuery = InputQuery<Unit, CompilerOptions>

class CompilerInstance(val versionData: QueryVersionData) {
    val code: CodeQuery = CodeQuery(versionData) { FileData(false, "") }
    private val options: OptionsQuery = OptionsQuery(versionData) { CompilerOptions("") }
    private val changes: Queue<FileChange> = ConcurrentLinkedQueue()
    private val handlers: MutableMap<String, LanguageHandler<*>> = hashMapOf()

    fun <T : SyntaxType> addHandler(handler: LanguageHandler<T>) {
        handler.extensions.forEach { ext ->
            handlers[ext] = handler
        }
    }

    private val fileHandler: Query<FileReference, LanguageHandler<*>?> = run {
        val handleFun = { key: FileReference, _: QueryVersion ->
            val ext = key.path.substringAfterLast('.')
            handlers[ext]
        }
        Query(versionData, handleFun) { _, old, version ->
            old.copy(checked = version)
        }
    }

    private val filePackage: Query<FileReference, PackageReference> = run {
        val packFun = { key: FileReference, _: QueryVersion ->
            val ops = options[Unit]
            val path = key.path.removePrefix(ops.value.startPath).split('/').dropLast(1)
            PackageReference(path)
        }
        Query(versionData, packFun) { key, old, version ->
            val ops = options[Unit]
            if (ops.modified <= old.checked) old.copy(checked = version)
            else packFun(key, version).toQueryValue(version)
        }
    }

    // TODO: Handle option changes
    val packageTree: Query<Unit, PackageTree> = run {
        val default = { _: Unit, version: QueryVersion ->
            var tree = PackageTree(PackageReference(emptyList()), version)
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

    private fun applyFileChange(tree: PackageTree, change: FileChange): PackageTree {
        val pack = filePackage[change.file]

        return when (change.type) {
            ChangeType.Addition -> tree.add(pack.value, change.file, change.version)

            ChangeType.Update -> tree.update(pack.value, change.file, change.version)

            ChangeType.Removal -> tree.remove(pack.value, change.file, change.version)
        }
    }

    val packageItems: Query<PackageReference, Map<String, List<UsableReference>>> = run {
        val collectFn = fn@{ key: PackageReference, _: QueryVersion ->
            val node = packageTree[Unit].value.nodeFor(key) ?: return@fn emptyMap<String, List<UsableReference>>()
            val res = mutableMapOf<String, MutableList<UsableReference>>()
            node.children.forEach { (name, _) ->
                res.getOrPut(name) { mutableListOf() }.add(PackageReference(key.path + name))
            }
            node.files.forEach { file ->
                val handler = fileHandler[file].value ?: return@forEach
                handler.fileItems[file].value.forEach { (name, list) ->
                    ItemType.values().forEach { type ->
                        list.iteratorFor(type).withIndex().forEach { (index, _) ->
                            res.getOrPut(name) { mutableListOf() }.add(ItemReference(file, name, type, index))
                        }
                    }
                }
            }
            res
        }
        Query(versionData, collectFn) { key, old, version ->
            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    val childItems: Query<ItemReference, Map<String, List<UsableReference>>> = run {
        val collectFn = fn@{ key: ItemReference, _: QueryVersion ->
            val res = mutableMapOf<String, MutableList<UsableReference>>()
            val handler = fileHandler[key.findFile()].value ?: return@fn res
            handler.childItems[key].value.forEach { (name, list) ->
                ItemType.values().forEach { type ->
                    list.iteratorFor(type).withIndex().forEach { (index, _) ->
                        res.getOrPut(name) { mutableListOf() }.add(ItemReference(key, name, type, index))
                    }
                }
            }
            res
        }
        Query(versionData, collectFn) { key, old, version ->
            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    fun addFile(ref: FileReference, code: String) {
        val type = if (this.code.contains(ref)) ChangeType.Update else ChangeType.Addition
        val newVersion = this.code.set(ref, FileData(true, code)).modified
        changes.offer(FileChange(ref, type, newVersion))
    }

    fun removeFile(ref: FileReference) {
        if (!code.contains(ref)) return

        val current = code[ref].value
        if (current.valid) {
            val newVersion = code.set(ref, current.copy(valid = false)).modified
            changes.offer(FileChange(ref, ChangeType.Removal, newVersion))
        }
    }
}