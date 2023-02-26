package pistonlang.compiler.common.files

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

val emptyTree = PackageTree(persistentMapOf(), emptyList())

data class PackageTree(val children: PersistentMap<String, PackageTree>, val files: List<FileHandle>) {
    fun add(pack: PackageHandle, file: FileHandle): PackageTree =
        add(pack.path, 0, file)

    private fun add(path: List<String>, index: Int, file: FileHandle): PackageTree {
        if (index == path.size) return add(file)

        val key = path[index]
        val node = children[key] ?: PackageTree(persistentMapOf(), emptyList())
        val new = node.add(path, index + 1, file)
        return PackageTree(children.put(key, new), files)
    }

    private fun add(file: FileHandle): PackageTree = PackageTree(children, files + file)

    private fun nullifyEmpty() = if (this == emptyTree) null else this

    fun remove(pack: PackageHandle, file: FileHandle): PackageTree =
        remove(pack.path, 0, file) ?: emptyTree

    private fun remove(path: List<String>, index: Int, file: FileHandle): PackageTree? {
        if (index == path.size) return remove(file)

        val key = path[index]
        val node = children[key] ?: return emptyTree
        return node.remove(path, index + 1, file)?.let { new ->
            PackageTree(children.put(key, new), files)
        } ?: PackageTree(children.remove(key), files).nullifyEmpty()
    }

    private fun remove(file: FileHandle): PackageTree? =
        PackageTree(children, files - file).nullifyEmpty()
}