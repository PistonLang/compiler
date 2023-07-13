package pistonlang.compiler.common.files

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import pistonlang.compiler.common.items.FileHandle
import pistonlang.compiler.common.items.PackageHandle
import pistonlang.compiler.common.queries.QueryVersion

internal const val packPathDelimiter = '.'

@JvmInline
value class PackageTree(val packages: PersistentMap<PackageHandle, PackageTreeNode>) {
    fun addFile(pack: PackageHandle, file: FileHandle, version: QueryVersion): PackageTree {
        val newPackages = packages.builder()

        val oldNode = packages[pack]
        val newNode = if (oldNode == null) {
            PackageTreeNode(version, persistentSetOf(file), persistentSetOf())
        } else {
            PackageTreeNode(version, oldNode.files.add(file), oldNode.children)
        }

        newPackages[pack] = newNode
        var child: PackageHandle = pack

        forEachParentPackage(pack.path) { handle ->
            val oldParent = packages[handle]
            val newParent =
                if (oldParent == null) PackageTreeNode(version, persistentSetOf(), persistentSetOf(child))
                else PackageTreeNode(version, oldParent.files, oldParent.children.add(child))
            newPackages[handle] = newParent
            child = handle
        }

        return PackageTree(newPackages.build())
    }

    fun updateFilePath(pack: PackageHandle, version: QueryVersion): PackageTree {
        val newPackages = packages.builder()

        val newNode = packages[pack]!!.copy(lastUpdated = version)
        newPackages[pack] = newNode

        forEachParentPackage(pack.path) { handle ->
            val newParent = packages[handle]!!.copy(lastUpdated = version)
            newPackages[handle] = newParent
        }

        return PackageTree(newPackages.build())
    }

    fun removeFile(pack: PackageHandle, file: FileHandle, version: QueryVersion): PackageTree {
        val newPackages = packages.builder()
        val oldNode = packages[pack]!!
        val newNode = PackageTreeNode(version, oldNode.files.remove(file), oldNode.children)

        newPackages[pack] = newNode

        var childValid = newNode.isValid
        var child = pack

        forEachParentPackage(pack.path) { handle ->
            val oldParent = packages[handle]!!
            val newParent = PackageTreeNode(
                lastUpdated = version,
                files = oldParent.files,
                children = if (childValid) oldParent.children else oldParent.children.remove(child),
            )

            newPackages[handle] = newParent

            childValid = newParent.isValid
            child = handle
        }

        return PackageTree(newPackages.build())
    }

    private inline fun forEachParentPackage(
        packPath: String,
        onParents: (PackageHandle) -> Unit
    ) {
        if (packPath.isEmpty()) return

        var index = packPath.length - 1

        while (index >= 0) {
            if (packPath[index] == packPathDelimiter) {
                onParents(PackageHandle(packPath.substring(0, index)))
            }
            index -= 1
        }

        onParents(PackageHandle(""))
    }
}


data class PackageTreeNode(
    val lastUpdated: QueryVersion,
    val files: PersistentSet<FileHandle>,
    val children: PersistentSet<PackageHandle>,
) {
    val isValid get(): Boolean = children.isNotEmpty() || files.isNotEmpty()
}