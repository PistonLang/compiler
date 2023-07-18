package pistonlang.compiler.common.files

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import pistonlang.compiler.common.items.FileId
import pistonlang.compiler.common.items.Interner
import pistonlang.compiler.common.items.MutableInterner
import pistonlang.compiler.common.items.PackageId
import pistonlang.compiler.common.queries.QueryVersion

internal const val packPathDelimiter = '.'

// TODO: Use a persistent IdList instead of a persistent map
class PackageTree(val packages: PersistentMap<PackageId, PackageTreeNode>) {
    fun addFile(
        pack: PackageId,
        file: FileId,
        version: QueryVersion,
        interner: MutableInterner<PackagePath, PackageId>
    ): PackageTree {
        val newPackages = packages.builder()

        val oldNode = packages[pack]
        val newNode = if (oldNode == null) {
            PackageTreeNode(version, persistentSetOf(file), persistentMapOf())
        } else {
            PackageTreeNode(version, oldNode.files.add(file), oldNode.children)
        }

        newPackages[pack] = newNode
        var child: PackageId = pack

        forEachParentPackage(interner.getKey(pack).path) { handle, childName ->
            val currId = interner.getOrAdd(handle)
            val oldParent = packages[currId]
            val newParent =
                if (oldParent == null) PackageTreeNode(
                    version,
                    persistentSetOf(),
                    persistentMapOf<String, PackageId>().put(childName, child)
                )
                else PackageTreeNode(version, oldParent.files, oldParent.children.put(childName, child))
            newPackages[currId] = newParent
            child = currId
        }

        return PackageTree(newPackages.build())
    }

    fun updateFilePath(
        pack: PackageId,
        version: QueryVersion,
        interner: Interner<PackagePath, PackageId>
    ): PackageTree {
        val newPackages = packages.builder()

        val newNode = packages[pack]!!.copy(lastUpdated = version)
        newPackages[pack] = newNode

        forEachParentPackage(interner.getKey(pack).path) { handle, _ ->
            val currId = interner[handle]
            newPackages[currId] = packages[currId]!!.copy(lastUpdated = version)
        }

        return PackageTree(newPackages.build())
    }

    fun removeFile(
        pack: PackageId,
        file: FileId,
        version: QueryVersion,
        interner: Interner<PackagePath, PackageId>
    ): PackageTree {
        val newPackages = packages.builder()
        val oldNode = packages[pack]!!
        val newNode = PackageTreeNode(version, oldNode.files.remove(file), oldNode.children)

        newPackages[pack] = newNode

        var childValid = newNode.isValid

        forEachParentPackage(interner.getKey(pack).path) { handle, childName ->
            val currId = interner[handle]
            val oldParent = packages[currId]!!
            val newParent = PackageTreeNode(
                lastUpdated = version,
                files = oldParent.files,
                children = if (childValid) oldParent.children else oldParent.children.remove(childName),
            )

            newPackages[currId] = newParent

            childValid = newParent.isValid
        }

        return PackageTree(newPackages.build())
    }

    private inline fun forEachParentPackage(
        packPath: String,
        onParents: (parentPath: PackagePath, childName: String) -> Unit
    ) {
        if (packPath.isEmpty()) return

        var index = packPath.length - 1
        var lastIndex = packPath.length

        while (index >= 0) {
            if (packPath[index] == packPathDelimiter) {
                onParents(
                    PackagePath(packPath.substring(0, index)),
                    packPath.substring(index + 1, lastIndex)
                )
                lastIndex = index
            }
            index -= 1
        }

        onParents(PackagePath(""), packPath.substring(0, lastIndex))
    }
}


data class PackageTreeNode(
    val lastUpdated: QueryVersion,
    val files: PersistentSet<FileId>,
    val children: PersistentMap<String, PackageId>,
) {
    val isValid get(): Boolean = children.isNotEmpty() || files.isNotEmpty()
}