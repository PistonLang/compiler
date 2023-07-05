package pistonlang.compiler.common.files

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import pistonlang.compiler.common.items.FileHandle
import pistonlang.compiler.common.items.PackageHandle
import pistonlang.compiler.common.queries.QueryVersion

/**
 * A persistent tree representing the package/file hierarchy of a project
 */
data class PackageTree(
    val handle: PackageHandle,
    val lastUpdated: QueryVersion,
    val children: PersistentMap<String, PackageTree> = persistentMapOf(),
    val files: PersistentSet<FileHandle> = persistentSetOf(),
    val validCount: Int = 0,
) {
    val isValid get(): Boolean = validCount > 0 || files.isNotEmpty()
    fun add(pack: PackageHandle, file: FileHandle, version: QueryVersion): PackageTree =
        add(pack.path, 0, file, version)

    private fun add(path: List<String>, index: Int, file: FileHandle, version: QueryVersion): PackageTree {
        if (index == path.size) return add(file, version)

        val key = path[index]
        val containsChild = children.contains(key)
        val node =
            if (containsChild) children[key]!!
            else PackageTree(handle.subpackage(key), version)
        val new = node.add(path, index + 1, file, version)
        return this.copy(lastUpdated = version, children = children.put(key, new), validCount = validCount + if (containsChild) 0 else 1)
    }

    private fun add(file: FileHandle, version: QueryVersion): PackageTree =
        this.copy(files = files.add(file), lastUpdated = version)

    fun update(pack: PackageHandle, file: FileHandle, version: QueryVersion): PackageTree =
        update(pack.path, 0, file, version)
    private fun update(path: List<String>, index: Int, file: FileHandle, version: QueryVersion): PackageTree {
        if (index == path.size) return this.copy(lastUpdated = version)

        val key = path[index]
        val new = children[key]!!.update(path, index + 1, file, version)
        return this.copy(lastUpdated = version, children = children.put(key, new))
    }

    fun remove(pack: PackageHandle, file: FileHandle, version: QueryVersion): PackageTree =
        remove(pack.path, 0, file, version)

    private fun remove(path: List<String>, index: Int, file: FileHandle, version: QueryVersion): PackageTree {
        if (index == path.size) return remove(file, version)

        val key = path[index]
        val newChild = children[key]?.remove(path, index + 1, file, version) ?: return this

        return this.copy(
            validCount = if (newChild.isValid) validCount else validCount - 1,
            children = children.put(key, newChild),
            lastUpdated = version,
        )
    }

    private fun remove(file: FileHandle, version: QueryVersion): PackageTree =
        this.copy(files = files.remove(file), lastUpdated = version)

    fun nodeFor(handle: PackageHandle) = nodeFor(handle.path, 0, this)
}

private tailrec fun nodeFor(path: List<String>, index: Int, tree: PackageTree): PackageTree? {
    return if (index == path.size) tree else {
        val node = tree.children[path[index]] ?: return null
        nodeFor(path, index + 1, node)
    }
}