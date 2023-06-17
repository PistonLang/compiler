package pistonlang.compiler.common.handles

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

val emptyPackageTree = PackageTree(persistentMapOf(), emptyList())

/**
 * A persistent tree representing the package/file hierarchy of a project
 */
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

    private fun isEmpty() = children.isEmpty() && files.isEmpty()

    fun remove(pack: PackageHandle, file: FileHandle): PackageTree =
        remove(pack.path, 0, file)

    private fun remove(path: List<String>, index: Int, file: FileHandle): PackageTree {
        if (index == path.size) return remove(file)

        val key = path[index]
        val newChild = children[key]?.remove(path, index + 1, file) ?: return this

        return if (newChild.isEmpty()) PackageTree(children.remove(key), files)
        else PackageTree(children.put(key, newChild), files)
    }

    private fun remove(file: FileHandle): PackageTree =
        PackageTree(children, files - file)

    fun nodeFor(handle: PackageHandle) = nodeFor(handle.path, 0, this)
}

private tailrec fun nodeFor(path: List<String>, index: Int, tree: PackageTree): PackageTree? {
    return if (index == path.size) tree else {
        val node = tree.children[path[index]] ?: return null
        nodeFor(path, index + 1, node)
    }
}