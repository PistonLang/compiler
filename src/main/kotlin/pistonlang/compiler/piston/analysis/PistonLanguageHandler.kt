package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.files.PackageTree
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.parser.Lexer
import pistonlang.compiler.common.parser.Parser
import pistonlang.compiler.common.parser.RelativeNodeLoc
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

    override val ast: Query<FileHandle, GreenNode<PistonType>> = run {
        val parseFn = { key: FileHandle, _: QueryVersion ->
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

    override val fileItems: Query<FileHandle, Map<String, MemberList<PistonType>>> = run {
        val collectFn = { key: FileHandle, _: QueryVersion ->
            val res = mutableMapOf<String, MutableMemberList<PistonType>>()

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

    override val childItems: Query<MemberHandle, Map<String, MemberList<PistonType>>> = run {
        val collectFn = fn@{ key: MemberHandle, _: QueryVersion ->
            val res = mutableMapOf<String, MutableMemberList<PistonType>>()

            val node = (nodeFromItemRef(key) ?: return@fn emptyMap<String, MemberList<PistonType>>()).asRoot()

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

    override val typeParams: Query<MemberHandle, List<Pair<String, RelativeNodeLoc<PistonType>>>> = run {
        val collectFn = fn@{ key: MemberHandle, _: QueryVersion ->
            (nodeFromItemRef(key) ?: return@fn emptyList<Pair<String, RelativeNodeLoc<PistonType>>>())
                .firstDirectChildOr(PistonType.typeParams) { return@fn emptyList<Pair<String, RelativeNodeLoc<PistonType>>>() }
                .asRoot().childSequence
                .filter { it.type == PistonType.identifier }
                .map { it.content to it.location }
                .toList()
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            if (ast[key.findFile()].modified <= old.checked) return@Query old.copy(checked = version)

            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    val fileImportData: Query<FileHandle, ImportData> = run {
        val collectFn = fn@{ key: FileHandle, _: QueryVersion ->
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
            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    private fun handleImportGroup(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        items: MutableList<HandleData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
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
        return HandleTreeNode(node.location, invalidIndex, children)
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
                resList.add(HandleTreeNode(node.location, invalidIndex, childList))
                return null
            }
            newPackage
        }

        val index = items.size

        val newName = identNode.content
        val subpackage = currPackage?.let { it.children[newName] } ?: run {
            items.add(HandleData(identNode.location, nullReferenceList))
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
                ?: return HandleTreeNode(node.location, invalidIndex, childList)
            newPackage
        }

        val index = items.size

        if (currPackage == null) {
            items.add(HandleData(identNode.location, nullReferenceList))
            return HandleTreeNode(node.location, index, childList)
        }

        val newName = identNode.content
        val handleList = instance.packageItems[currPackage.reference].value[newName]
        val handles = handleList?.assertNonEmpty() ?: nullReferenceList
        items.add(HandleData(identNode.location, handles))
        nameMap.getOrPut(newName) { mutableListOf() }.add(index)

        return HandleTreeNode(node.location, index, childList)
    }

    private fun defNodeToReference(
        child: GreenChild<PistonType>,
        res: MutableMap<String, MutableMemberList<PistonType>>
    ) {
        val node = child.value
        var name: String = node.firstDirectChild(PistonType.identifier)?.content ?: ""
        val type: MemberType = when (node.type) {
            PistonType.traitDef ->
                MemberType.Trait

            PistonType.classDef ->
                if (node.firstDirectChild(PistonType.functionParams) != null)
                    MemberType.MultiInstanceClass
                else
                    MemberType.SingletonClass

            PistonType.propertyDef ->
                if (node.firstDirectChild(PistonType.valKw) != null)
                    MemberType.Val
                else
                    MemberType.Var

            PistonType.functionDef -> when {
                node.firstDirectChild(PistonType.functionParams) == null ->
                    MemberType.Getter

                name.length > 2 && name.endsWith("_=") -> {
                    name = name.dropLast(2)
                    MemberType.Setter
                }

                else -> MemberType.Function
            }

            else -> error("Something went wrong with the syntax set")
        }

        val loc = child.parentRelativeLocation
        res.getOrPut(name) { MutableMemberList() }.add(type, loc)
    }

    private fun defNodeToReference(
        node: RedNode<PistonType>,
        res: MutableMap<String, MutableMemberList<PistonType>>
    ) {
        var name: String = node.firstDirectChild(PistonType.identifier)?.content ?: ""
        val type: MemberType = when (node.type) {
            PistonType.traitDef ->
                MemberType.Trait

            PistonType.classDef ->
                if (node.firstDirectChild(PistonType.functionParams) != null)
                    MemberType.MultiInstanceClass
                else
                    MemberType.SingletonClass

            PistonType.propertyDef ->
                if (node.firstDirectChild(PistonType.valKw) != null)
                    MemberType.Val
                else
                    MemberType.Var

            PistonType.functionDef -> when {
                node.firstDirectChild(PistonType.functionParams) == null ->
                    MemberType.Getter

                name.length > 2 && name.endsWith("_=") -> {
                    name = name.dropLast(2)
                    MemberType.Setter
                }

                else -> MemberType.Function
            }

            else -> error("Something went wrong with the syntax set")
        }

        val loc = node.location
        res.getOrPut(name) { MutableMemberList() }.add(type, loc)
    }

    override val constructors: Query<MultiInstanceClassHandle, List<RelativeNodeLoc<PistonType>>> = run {
        val collectFn = fn@{ key: MultiInstanceClassHandle, _: QueryVersion ->
            (nodeFromItemRef(key) ?: return@fn emptyList<RelativeNodeLoc<PistonType>>())
                .firstDirectChildOr(PistonType.functionParams) { return@fn emptyList<RelativeNodeLoc<PistonType>>() }
                .let { listOf(it.location) }
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            if (ast[key.findFile()].modified <= old.checked) return@Query old.copy(checked = version)

            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    private tailrec fun findFile(ref: ParentHandle, stack: Stack<MemberHandle>): FileHandle =
        if (ref.isFile) ref as FileHandle else {
            stack.push(ref as MemberHandle)
            findFile(ref.parent, stack)
        }

    private tailrec fun findNode(
        node: RedNode<PistonType>,
        stack: Stack<MemberHandle>,
        parentRef: MemberHandle
    ): RedNode<PistonType>? {
        if (stack.empty())
            return node

        val itemRef = stack.pop()
        val pos = childItems[parentRef].value[itemRef.name]?.get(itemRef.memberType, itemRef.id) ?: return null
        return findNode(node.findAtRelative(pos) ?: return null, stack, itemRef)
    }

    fun nodeFromItemRef(ref: MemberHandle): RedNode<PistonType>? {
        val refStack = Stack<MemberHandle>()
        val fileRef = findFile(ref, refStack)
        val node = ast[fileRef].value.asRedRoot()
        val firstRef = refStack.pop()
        val firstPos = fileItems[fileRef].value[firstRef.name]?.get(firstRef.memberType, firstRef.id) ?: return null
        return findNode(node.findAtRelative(firstPos) ?: return null, refStack, firstRef)
    }
}