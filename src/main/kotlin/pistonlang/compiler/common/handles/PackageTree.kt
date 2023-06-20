package pistonlang.compiler.common.handles

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * A persistent tree representing the package/file hierarchy of a project
 */
data class PackageTree(
    val handle: PackageHandle,
    val children: PersistentMap<String, PackageTree>,
    val files: List<FileHandle>
) {
    fun add(pack: PackageHandle, file: FileHandle): PackageTree =
        add(pack.path, 0, file)

    private fun add(path: List<String>, index: Int, file: FileHandle): PackageTree {
        if (index == path.size) return add(file)

        val key = path[index]
        val node = children[key]
            ?: PackageTree(PackageHandle(handle.path + key), persistentMapOf(), emptyList())
        val new = node.add(path, index + 1, file)
        return PackageTree(handle, children.put(key, new), files)
    }

    private fun add(file: FileHandle): PackageTree = this.copy(files = files + file)

    private fun isEmpty() = children.isEmpty() && files.isEmpty()

    fun remove(pack: PackageHandle, file: FileHandle): PackageTree =
        remove(pack.path, 0, file)

    private fun remove(path: List<String>, index: Int, file: FileHandle): PackageTree {
        if (index == path.size) return remove(file)

        val key = path[index]
        val newChild = children[key]?.remove(path, index + 1, file) ?: return this

        return if (newChild.isEmpty()) this.copy(children = children.remove(key))
        else this.copy(children = children.put(key, newChild))
    }

    private fun remove(file: FileHandle): PackageTree =
        this.copy(files = files - file)

    fun nodeFor(handle: PackageHandle) = nodeFor(handle.path, 0, this)
}

private tailrec fun nodeFor(path: List<String>, index: Int, tree: PackageTree): PackageTree? {
    return if (index == path.size) tree else {
        val node = tree.children[path[index]] ?: return null
        nodeFor(path, index + 1, node)
    }
}