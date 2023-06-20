package pistonlang.compiler.piston

import pistonlang.compiler.common.handles.FileHandle
import pistonlang.compiler.common.main.CompilerInstance

@JvmInline
value class TestFileTree internal constructor(internal val node: TestFileTreeNode)

data class TestFileTreeNode internal constructor(
    internal val children: List<Pair<String, TestFileTreeNode>>,
    internal val files: List<Pair<String, String>>
)

class TestFileTreeBuilder {
    private val children = mutableListOf<Pair<String, TestFileTreeNode>>()
    private val files = mutableListOf<Pair<String, String>>()

    @PublishedApi
    internal fun addChild(name: String, node: TestFileTreeNode) {
        children.add(name to node)
    }

    @PublishedApi
    internal fun addFile(name: String, code: String) {
        files.add(name to code)
    }

    inline fun child(name: String, fn: TestFileTreeBuilder.() -> Unit) {
        addChild(name, TestFileTreeBuilder().apply(fn).build())
    }

    inline fun file(name: String, codeFn: () -> String) {
        addFile(name, codeFn())
    }

    @PublishedApi
    internal fun build() = TestFileTreeNode(children, files)

    @PublishedApi
    internal fun buildRoot() = TestFileTree(TestFileTreeNode(children, files))
}

inline fun fileTree(fn: TestFileTreeBuilder.() -> Unit): TestFileTree =
    TestFileTreeBuilder().apply(fn).buildRoot()

fun CompilerInstance.add(tree: TestFileTree) = addTo(this, tree.node, "")

private fun addTo(instance: CompilerInstance, node: TestFileTreeNode, pathString: String) {
    node.files.forEach { (name, code) -> instance.addFile(FileHandle(pathString + name), code) }
    node.children.forEach { (name, child) -> addTo(instance, child, "$pathString$name/") }
}