package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.handles.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.parser.Lexer
import pistonlang.compiler.common.parser.Parser
import pistonlang.compiler.common.parser.nodes.*
import pistonlang.compiler.common.queries.Query
import pistonlang.compiler.common.queries.toQueryValue
import pistonlang.compiler.piston.parser.PistonSyntaxSets
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.assertNonEmpty
import pistonlang.compiler.util.findFirst


/**
 * The standard [LanguageHandler] for Piston code
 */
class PistonLanguageHandler(
    lexer: (String) -> Lexer<PistonType>,
    parsing: (Parser<PistonType>) -> GreenNode<PistonType>,
    private val instance: CompilerInstance,
) : LanguageHandler<PistonType> {
    override val extensions: List<String> = listOf("pi")

    override val ast: Query<FileHandle, GreenNode<PistonType>> = run {
        val parseFn = { key: FileHandle ->
            val codeString = instance.code[key].value
            val parser = Parser(lexer(codeString), PistonType.file)
            parsing(parser)
        }
        Query(instance.versionData, parseFn) { key, old, version ->
            val codeString = instance.code[key]
            if (codeString.modified <= old.checked) old.copy(checked = version)
            else parseFn(key).toQueryValue(version)
        }
    }


    override val fileItems: Query<FileHandle, Map<String, ItemList<PistonType>>> = run {
        val collectFn = { key: FileHandle ->
            val res = mutableMapOf<String, MutableItemList<PistonType>>()

            ast[key].value.childSequence
                .filter { child -> child.type in PistonSyntaxSets.defs }
                .forEach { child -> defNodeToReference(child, key, res) }

            res.mapValues { (_, list) -> list.toImmutable() }
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            if (ast[key].modified <= old.checked) return@Query old.copy(checked = version)

            val new = collectFn(key)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    val fileImportData: Query<FileHandle, ImportData> = run {
        val collectFn = fn@{ key: FileHandle ->
            val fileAst = ast[key].value

            val importNode = fileAst
                .firstDirectRawChildOr(PistonType.import) { return@fn emptyImportData }
                .firstDirectRawChildOr(PistonType.importGroup) { return@fn emptyImportData }
                .asRedRoot()

            val tree = instance.packageTree[Unit].value
            val data = mutableListOf<HandleData<PistonType>>()
            val nameMap = hashMapOf<String, MutableList<Int>>()
            val children = handleImportGroup(tree, importNode, data, nameMap)

            ImportData(HandleTree(data, children), nameMap)
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            val new = collectFn(key)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    private fun handleImportGroup(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        items: MutableList<HandleData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>
    ): List<HandleTreeNode<PistonType>> = node.childSequence
        .filter { it.type == PistonType.importSegment }
        .map { segment -> handleImportSegment(pack, segment, items, nameMap) }
        .toList()

    private fun handleImportSegment(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        items: MutableList<HandleData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ): HandleTreeNode<PistonType> {
        val group = node.lastDirectChild(PistonType.importGroup)
        val path = node.firstDirectChild(PistonSyntaxSets.importPath)!!

        if (group == null) {
            return handleDirectImportPath(pack, path, items, nameMap)
        }

        val children = mutableListOf<HandleTreeNode<PistonType>>()
        val newPack = handleImportPath(pack, path, items, children)
        children.addAll(handleImportGroup(newPack, group, items, nameMap))
        return HandleTreeNode(node.location, -1, children)
    }

    private fun handleImportPath(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        items: MutableList<HandleData<PistonType>>,
        resList: MutableList<HandleTreeNode<PistonType>>
    ): PackageTree? {
        val childList = mutableListOf<HandleTreeNode<PistonType>>()
        val identNode: RedNode<PistonType>
        val currPackage: PackageTree? = if (node.type == PistonType.identifier) {
            identNode = node
            pack
        } else {
            val iter = node.childIterator
            val left = iter.findFirst { it.type in PistonSyntaxSets.importPath }!!
            val newPackage = handleImportPath(pack, left, items, childList)
            identNode = iter.findFirst { it.type == PistonType.identifier } ?: run {
                resList.add(HandleTreeNode(node.location, -1, childList))
                return null
            }
            newPackage
        }

        val index = items.size

        val newName = identNode.content
        val subpackage = currPackage?.let { it.children[newName] } ?: run {
            items.add(HandleData(identNode.location, nullHandleList))
            resList.add(HandleTreeNode(node.location, index, childList))
            return null
        }
        resList.add(HandleTreeNode(node.location, index, childList))

        return subpackage
    }

    private fun handleDirectImportPath(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        items: MutableList<HandleData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ): HandleTreeNode<PistonType> {
        val childList = mutableListOf<HandleTreeNode<PistonType>>()
        val identNode: RedNode<PistonType>
        val currPackage: PackageTree? = if (node.type == PistonType.identifier) {
            identNode = node
            pack
        } else {
            val iter = node.childIterator
            val left = iter.findFirst { it.type in PistonSyntaxSets.importPath }!!
            val newPackage = handleImportPath(pack, left, items, childList)
            identNode = iter.findFirst { it.type == PistonType.identifier }
                ?: return HandleTreeNode(node.location, -1, childList)
            newPackage
        }

        val index = items.size

        if (currPackage == null) {
            items.add(HandleData(identNode.location, nullHandleList))
            return HandleTreeNode(node.location, index, childList)
        }

        val newName = identNode.content
        val handleList = instance.packageItems[currPackage.handle].value[newName]
        val handles = handleList?.assertNonEmpty() ?: nullHandleList
        items.add(HandleData(identNode.location, handles))
        nameMap.getOrPut(newName) { mutableListOf() }.add(index)

        return HandleTreeNode(node.location, index, childList)
    }

    private fun defNodeToReference(
        child: GreenChild<PistonType>,
        key: FileHandle,
        res: MutableMap<String, MutableItemList<PistonType>>
    ) {
        val node = child.value
        var name: String = node.firstDirectChild(PistonType.identifier)?.content ?: ""
        val type: ItemType = when (node.type) {
            PistonType.traitDef ->
                ItemType.Trait

            PistonType.classDef ->
                if (node.firstDirectChild(PistonType.functionParams) != null)
                    ItemType.MultiInstanceClass
                else
                    ItemType.SingletonClass

            PistonType.propertyDef ->
                if (node.firstDirectChild(PistonType.valKw) != null)
                    ItemType.Val
                else
                    ItemType.Var

            PistonType.functionDef -> when {
                node.firstDirectChild(PistonType.functionParams) == null ->
                    ItemType.Getter

                name.length > 2 && name.endsWith("_=") -> {
                    name = name.dropLast(2)
                    ItemType.Setter
                }

                else -> ItemType.Function
            }

            else -> error("Something went wrong with the syntax set")
        }

        val loc = child.parentRelativeLocation
        val ref = ItemReference(type, this, loc, key)
        res.getOrPut(name) { MutableItemList() }.add(ref)
    }
}