package pistonlang.compiler.common.files

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import pistonlang.compiler.common.items.FileReference
import pistonlang.compiler.common.items.PackageReference
import pistonlang.compiler.common.queries.QueryVersion

/**
 * A persistent tree representing the package/file hierarchy of a project
 */
data class PackageTree(
    val reference: PackageReference,
    val lastUpdated: QueryVersion,
    val children: PersistentMap<String, PackageTree> = persistentMapOf(),
    val files: List<FileReference> = emptyList(),
    val validCount: Int = 0,
) {
    private fun isValid(): Boolean = validCount > 0 || files.isNotEmpty()
    fun add(pack: PackageReference, file: FileReference, version: QueryVersion): PackageTree =
        add(pack.path, 0, file, version)

    private fun add(path: List<String>, index: Int, file: FileReference, version: QueryVersion): PackageTree {
        if (index == path.size) return add(file, version)

        val key = path[index]
        val containsChild = children.contains(key)
        val node =
            if (containsChild) children[key]!!
            else PackageTree(PackageReference(reference.path + key), version)
        val new = node.add(path, index + 1, file, version)
        return this.copy(lastUpdated = version, children = children.put(key, new), validCount = validCount + if (containsChild) 0 else 1)
    }

    private fun add(file: FileReference, version: QueryVersion): PackageTree =
        this.copy(files = files + file, lastUpdated = version)

    fun update(pack: PackageReference, file: FileReference, version: QueryVersion): PackageTree =
        update(pack.path, 0, file, version)
    private fun update(path: List<String>, index: Int, file: FileReference, version: QueryVersion): PackageTree {
        if (index == path.size) return this.copy(lastUpdated = version)

        val key = path[index]
        val new = children[key]!!.update(path, index + 1, file, version)
        return this.copy(lastUpdated = version, children = children.put(key, new))
    }

    fun remove(pack: PackageReference, file: FileReference, version: QueryVersion): PackageTree =
        remove(pack.path, 0, file, version)

    private fun remove(path: List<String>, index: Int, file: FileReference, version: QueryVersion): PackageTree {
        if (index == path.size) return remove(file, version)

        val key = path[index]
        val newChild = children[key]?.remove(path, index + 1, file, version) ?: return this

        return this.copy(
            validCount = if (newChild.isValid()) validCount else validCount - 1,
            children = children.put(key, newChild),
            lastUpdated = version,
        )
    }

    private fun remove(file: FileReference, version: QueryVersion): PackageTree =
        this.copy(files = files - file, lastUpdated = version)

    fun nodeFor(handle: PackageReference) = nodeFor(handle.path, 0, this)
}

private tailrec fun nodeFor(path: List<String>, index: Int, tree: PackageTree): PackageTree? {
    return if (index == path.size) tree else {
        val node = tree.children[path[index]] ?: return null
        nodeFor(path, index + 1, node)
    }
}