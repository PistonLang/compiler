package pistonlang.compiler.common.files

import pistonlang.compiler.common.items.FileReference
import pistonlang.compiler.common.main.CompilerInstance
import java.util.*

@JvmInline
value class VirtualPackageTree<Data> internal constructor(
    private val node: VirtualPackageTreeNode<Data>,
) : Iterable<Pair<FileReference, Data>> {
    fun traverse(fn: (FileReference, Data) -> Unit) = node.traverse(fn, "")

    fun <Res> mapValues(fn: (Data) -> Res) = VirtualPackageTree(node.mapValues(fn))

    override operator fun iterator(): Iterator<Pair<FileReference, Data>> = node.iterator()
}

data class VirtualPackageTreeNode<Data> internal constructor(
    private val children: List<Pair<String, VirtualPackageTreeNode<Data>>>,
    private val files: List<Pair<String, Data>>
) {
    internal fun traverse(fn: (FileReference, Data) -> Unit, pathString: String) {
        files.forEach { (name, code) -> fn(FileReference(pathString + name), code) }
        children.forEach { (name, child) -> child.traverse(fn, "$pathString$name/") }
    }

    internal fun <Res> mapValues(fn: (Data) -> Res): VirtualPackageTreeNode<Res> = VirtualPackageTreeNode(
        children.map { (name, node) -> name to node.mapValues(fn) },
        files.map { (name, data) -> name to fn(data) }
    )

    internal operator fun iterator(): Iterator<Pair<FileReference, Data>> =
        object : Iterator<Pair<FileReference, Data>> {
            private val nodeStack = Stack<Pair<VirtualPackageTreeNode<Data>, Int>>()
            private var currentNode = this@VirtualPackageTreeNode
            private var prefix = ""
            private var fileIndex = 0
            private var childIndex = 0

            init {
                findNextNode()
            }

            tailrec fun findNextNode(): Unit = when {
                fileIndex < currentNode.files.size -> Unit

                childIndex < currentNode.children.size -> {
                    nodeStack.push(currentNode to childIndex + 1)
                    val child = currentNode.children[childIndex]
                    prefix = "$prefix${child.first}/"
                    currentNode = child.second
                    fileIndex = 0
                    childIndex = 0
                    findNextNode()
                }

                nodeStack.isEmpty() -> {
                    childIndex = -1
                }

                else -> {
                    val parent = nodeStack.pop()
                    currentNode = parent.first
                    fileIndex = currentNode.files.size
                    childIndex = parent.second
                    if (prefix.isNotEmpty())
                        prefix = prefix.dropLast(1).dropLastWhile { it != '/' }
                    findNextNode()
                }
            }

            override fun hasNext(): Boolean = childIndex != -1

            override fun next(): Pair<FileReference, Data> {
                if (childIndex == -1) error("Tried to access the next child of an iterator at the end of a Virtual Package Tree")

                val pair = currentNode.files[fileIndex]
                val res = FileReference("$prefix${pair.first}") to pair.second

                fileIndex += 1
                findNextNode()

                return res
            }
        }
}

class VirtualPackageTreeBuilder<Data> {
    private val children = mutableListOf<Pair<String, VirtualPackageTreeNode<Data>>>()
    private val files = mutableListOf<Pair<String, Data>>()

    @PublishedApi
    internal fun addChild(name: String, node: VirtualPackageTreeNode<Data>) {
        children.add(name to node)
    }

    @PublishedApi
    internal fun addFile(name: String, data: Data) {
        files.add(name to data)
    }

    inline fun child(name: String, fn: VirtualPackageTreeBuilder<Data>.() -> Unit) {
        addChild(name, VirtualPackageTreeBuilder<Data>().apply(fn).build())
    }

    inline fun file(name: String, dataFn: () -> Data) {
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
    tree.traverse { reference, code -> this.addFile(reference, code) }