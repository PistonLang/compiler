package pistonlang.compiler.common.files

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.util.EmptyIterator
import java.util.*


@JvmInline
value class VirtualPackageTree<out Data> internal constructor(
    internal val node: VirtualPackageTreeNode<Data>,
) : Iterable<Pair<FilePath, Data>> {
    fun traverse(fn: (PackagePath, FilePath, Data) -> Unit) = node.traverse(fn, "")

    fun <Res> mapValues(fn: (Data) -> Res) = VirtualPackageTree(node.mapValues(fn))

    override operator fun iterator(): Iterator<Pair<FilePath, Data>> = node.iterator()
}

operator fun <Data> VirtualPackageTree<Data>.plus(other: VirtualPackageTree<Data>) =
    VirtualPackageTree(this.node + other.node)

internal data class VirtualPackageTreeNode<out Data> internal constructor(
    internal val children: PersistentMap<String, VirtualPackageTreeNode<Data>>,
    internal val files: PersistentMap<String, Data>,
) {
    internal fun traverse(fn: (PackagePath, FilePath, Data) -> Unit, pathString: String) {
        files.forEach { (name, code) -> fn(PackagePath(pathString.dropLast(1)), FilePath(pathString + name), code) }
        children.forEach { (name, child) -> child.traverse(fn, "$pathString$name$packPathDelimiter") }
    }

    internal fun <Res> mapValues(fn: (Data) -> Res): VirtualPackageTreeNode<Res> = VirtualPackageTreeNode(
        children.asSequence().fold(persistentHashMapOf()) { map, (name, node) -> map.put(name, node.mapValues(fn)) },
        files.asSequence().fold(persistentHashMapOf()) { map, (name, data) -> map.put(name, fn(data)) }
    )

    internal operator fun iterator(): Iterator<Pair<FilePath, Data>> =
        object : Iterator<Pair<FilePath, Data>> {
            private val nodeStack = Stack<Iterator<Map.Entry<String, VirtualPackageTreeNode<Data>>>>()
            private var prefix = ""
            private var fileIter = this@VirtualPackageTreeNode.files.iterator()
            private var childIter = this@VirtualPackageTreeNode.children.iterator()

            init {
                findNextNode()
            }

            tailrec fun findNextNode(): Unit = when {
                fileIter.hasNext() -> Unit

                childIter.hasNext() -> {
                    nodeStack.push(childIter)
                    val (key, child) = childIter.next()
                    prefix = "$prefix${key}$packPathDelimiter"
                    fileIter = child.files.iterator()
                    childIter = child.children.iterator()
                    findNextNode()
                }

                nodeStack.isEmpty() -> Unit

                else -> {
                    val parent = nodeStack.pop()
                    fileIter = EmptyIterator
                    childIter = parent
                    if (prefix.isNotEmpty())
                        prefix = prefix.dropLast(1).dropLastWhile { it != packPathDelimiter }
                    findNextNode()
                }
            }

            override fun hasNext(): Boolean = fileIter.hasNext()

            override fun next(): Pair<FilePath, Data> {
                if (!hasNext()) error("Tried to access the next child of an iterator at the end of a Virtual Package Tree")

                val pair = fileIter.next()
                val res = FilePath("$prefix${pair.key}") to pair.value

                findNextNode()

                return res
            }
        }
}

private operator fun <Data> VirtualPackageTreeNode<Data>.plus(other: VirtualPackageTreeNode<Data>): VirtualPackageTreeNode<Data> {
    val newFiles = files.putAll(other.files)
    val newChildren = other.children.asSequence().fold(children) { newChildren, (key, child) ->
        val newChild = newChildren[key].let { if (it == null) child else it + child }
        newChildren.put(key, newChild)
    }
    return VirtualPackageTreeNode(newChildren, newFiles)
}

class VirtualPackageTreeBuilder<Data> {
    private var children = persistentHashMapOf<String, VirtualPackageTreeNode<Data>>()
    private var files = persistentHashMapOf<String, Data>()

    @PublishedApi
    internal fun addChild(name: String, node: VirtualPackageTreeNode<Data>) {
        children = children.put(name, node)
    }

    @PublishedApi
    internal fun addFile(name: String, data: Data) {
        files = files.put(name, data)
    }

    inline fun child(name: String, fn: VirtualPackageTreeBuilder<Data>.() -> Unit) {
        addChild(name, VirtualPackageTreeBuilder<Data>().apply(fn).build())
    }

    inline fun data(name: String, dataFn: () -> Data) {
        addFile(name, dataFn())
    }

    @PublishedApi
    internal fun build() = VirtualPackageTreeNode(children, files)

    @PublishedApi
    internal fun buildRoot() = VirtualPackageTree(VirtualPackageTreeNode(children, files))
}

inline fun <Data> virtualTree(fn: VirtualPackageTreeBuilder<Data>.() -> Unit): VirtualPackageTree<Data> =
    VirtualPackageTreeBuilder<Data>().apply(fn).buildRoot()

fun CompilerInstance.add(tree: VirtualPackageTree<String>) =
    tree.traverse { pack, file, code -> this.addFile(pack, file, code) }