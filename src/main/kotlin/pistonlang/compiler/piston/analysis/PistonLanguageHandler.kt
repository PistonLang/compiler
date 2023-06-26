package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.files.PackageTree
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.parser.Lexer
import pistonlang.compiler.common.parser.Parser
import pistonlang.compiler.common.parser.nodes.*
import pistonlang.compiler.common.queries.Query
import pistonlang.compiler.common.queries.QueryVersion
import pistonlang.compiler.common.queries.toQueryValue
import pistonlang.compiler.piston.parser.PistonSyntaxSets
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.assertNonEmpty
import pistonlang.compiler.util.findFirst
import java.util.*

val emptyNode = GreenLeaf(PistonType.file, "")

/**
 * The standard [LanguageHandler] for Piston code
 */
class PistonLanguageHandler(
    lexer: (String) -> Lexer<PistonType>,
    parsing: (Parser<PistonType>) -> GreenNode<PistonType>,
    private val instance: CompilerInstance,
) : LanguageHandler<PistonType> {
    override val extensions: List<String> = listOf("pi")

    override val ast: Query<FileReference, GreenNode<PistonType>> = run {
        val parseFn = { key: FileReference, _: QueryVersion ->
            val data = instance.code[key].value
            if (!data.valid) emptyNode else {
                val parser = Parser(lexer(data.code), PistonType.file)
                parsing(parser)
            }
        }
        Query(instance.versionData, parseFn) { key, old, version ->
            val codeString = instance.code[key]
            if (codeString.modified <= old.checked) old.copy(checked = version)
            else parseFn(key, version).toQueryValue(version)
        }
    }

    override val fileItems: Query<FileReference, Map<String, ItemList<PistonType>>> = run {
        val collectFn = { key: FileReference, _: QueryVersion ->
            val res = mutableMapOf<String, MutableItemList<PistonType>>()

            ast[key].value.childSequence
                .filter { child -> child.type in PistonSyntaxSets.defs }
                .forEach { child -> defNodeToReference(child, res) }

            res.mapValues { (_, list) -> list.toImmutable() }
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            if (ast[key].modified <= old.checked) return@Query old.copy(checked = version)

            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    override val childItems: Query<ItemReference, Map<String, ItemList<PistonType>>> = run {
        val collectFn = fn@{ key: ItemReference, _: QueryVersion ->
            val res = mutableMapOf<String, MutableItemList<PistonType>>()

            val node = (nodeFromItemRef(key) ?: return@fn emptyMap<String, ItemList<PistonType>>()).asRoot()

            node.firstDirectChild(PistonType.typeParams)?.let { params ->
                params.childSequence.filter { it.type == PistonType.identifier }.forEach { ident ->
                    res.getOrPut(ident.content) { MutableItemList() }
                        .add(ItemType.TypeParam, ident.location)
                }
            }

            node.lastDirectChild(PistonType.statementBlock)?.let { block ->
                block.childSequence
                    .filter { child -> child.type in PistonSyntaxSets.defs }
                    .forEach { child -> defNodeToReference(child, res) }
            }


            res.mapValues { (_, list) -> list.toImmutable() }
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            if (ast[key.findFile()].modified <= old.checked) return@Query old.copy(checked = version)

            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    val fileImportData: Query<FileReference, ImportData> = run {
        val collectFn = fn@{ key: FileReference, _: QueryVersion ->
            val fileAst = ast[key].value

            val importNode = fileAst
                .firstDirectRawChildOr(PistonType.import) { return@fn emptyImportData }
                .firstDirectRawChildOr(PistonType.importGroup) { return@fn emptyImportData }
                .asRedRoot()

            val tree = instance.packageTree[Unit].value
            val data = mutableListOf<ReferenceData<PistonType>>()
            val nameMap = hashMapOf<String, MutableList<Int>>()
            val children = handleImportGroup(tree, importNode, data, nameMap)

            ImportData(ReferenceTree(data, children), nameMap)
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    private fun handleImportGroup(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        items: MutableList<ReferenceData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ): List<ReferenceTreeNode<PistonType>> = node.childSequence
        .filter { it.type == PistonType.importSegment }
        .map { segment -> handleImportSegment(pack, segment, items, nameMap) }
        .toList()

    private fun handleImportSegment(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        items: MutableList<ReferenceData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ): ReferenceTreeNode<PistonType> {
        val group = node.lastDirectChild(PistonType.importGroup)
        val path = node.firstDirectChild(PistonSyntaxSets.importPath)!!

        if (group == null) {
            return handleDirectImportPath(pack, path, items, nameMap)
        }

        val children = mutableListOf<ReferenceTreeNode<PistonType>>()
        val newPack = handleImportPath(pack, path, items, children)
        children.addAll(handleImportGroup(newPack, group, items, nameMap))
        return ReferenceTreeNode(node.location, invalidIndex, children)
    }

    private fun handleImportPath(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        items: MutableList<ReferenceData<PistonType>>,
        resList: MutableList<ReferenceTreeNode<PistonType>>
    ): PackageTree? {
        val childList = mutableListOf<ReferenceTreeNode<PistonType>>()
        val identNode: RedNode<PistonType>
        val currPackage: PackageTree? = if (node.type == PistonType.identifier) {
            identNode = node
            pack
        } else {
            val iter = node.childIterator
            val left = iter.findFirst { it.type in PistonSyntaxSets.importPath }!!
            val newPackage = handleImportPath(pack, left, items, childList)
            identNode = iter.findFirst { it.type == PistonType.identifier } ?: run {
                resList.add(ReferenceTreeNode(node.location, invalidIndex, childList))
                return null
            }
            newPackage
        }

        val index = items.size

        val newName = identNode.content
        val subpackage = currPackage?.let { it.children[newName] } ?: run {
            items.add(ReferenceData(identNode.location, nullReferenceList))
            resList.add(ReferenceTreeNode(node.location, index, childList))
            return null
        }
        resList.add(ReferenceTreeNode(node.location, index, childList))

        return subpackage
    }

    private fun handleDirectImportPath(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        items: MutableList<ReferenceData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ): ReferenceTreeNode<PistonType> {
        val childList = mutableListOf<ReferenceTreeNode<PistonType>>()
        val identNode: RedNode<PistonType>
        val currPackage: PackageTree? = if (node.type == PistonType.identifier) {
            identNode = node
            pack
        } else {
            val iter = node.childIterator
            val left = iter.findFirst { it.type in PistonSyntaxSets.importPath }!!
            val newPackage = handleImportPath(pack, left, items, childList)
            identNode = iter.findFirst { it.type == PistonType.identifier }
                ?: return ReferenceTreeNode(node.location, invalidIndex, childList)
            newPackage
        }

        val index = items.size

        if (currPackage == null) {
            items.add(ReferenceData(identNode.location, nullReferenceList))
            return ReferenceTreeNode(node.location, index, childList)
        }

        val newName = identNode.content
        val handleList = instance.packageItems[currPackage.reference].value[newName]
        val handles = handleList?.assertNonEmpty() ?: nullReferenceList
        items.add(ReferenceData(identNode.location, handles))
        nameMap.getOrPut(newName) { mutableListOf() }.add(index)

        return ReferenceTreeNode(node.location, index, childList)
    }

    private fun defNodeToReference(
        child: GreenChild<PistonType>,
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
        res.getOrPut(name) { MutableItemList() }.add(type, loc)
    }

    private fun defNodeToReference(
        node: RedNode<PistonType>,
        res: MutableMap<String, MutableItemList<PistonType>>
    ) {
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

        val loc = node.location
        res.getOrPut(name) { MutableItemList() }.add(type, loc)
    }

    private tailrec fun findFile(ref: ParentReference, stack: Stack<ItemReference>): FileReference =
        if (ref.isFile) ref as FileReference else {
            stack.push(ref as ItemReference)
            findFile(ref.parent, stack)
        }

    private tailrec fun findNode(node: RedNode<PistonType>, stack: Stack<ItemReference>, parentRef: ItemReference): RedNode<PistonType>? {
        if (stack.empty())
            return node

        val itemRef = stack.pop()
        val pos = childItems[parentRef].value[itemRef.name]?.get(itemRef.type, itemRef.id) ?: return null
        return findNode(node.findAtRelative(pos) ?: return null, stack, itemRef)
    }

    fun nodeFromItemRef(ref: ItemReference): RedNode<PistonType>? {
        val refStack = Stack<ItemReference>()
        val fileRef = findFile(ref, refStack)
        val node = ast[fileRef].value.asRedRoot()
        val firstRef = refStack.pop()
        val firstPos = fileItems[fileRef].value[firstRef.name]?.get(firstRef.type, firstRef.id) ?: return null
        return findNode(node.findAtRelative(firstPos) ?: return null, refStack, firstRef)
    }
}