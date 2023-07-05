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
import pistonlang.compiler.util.nonEmptyListOf
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

            ast[key].childSequence
                .filter { child -> child.type in PistonSyntaxSets.defs }
                .forEach { child -> defNodeToReference(child, res) }

            res.mapValues { (_, list) -> list.toImmutable() }
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            if (ast.lastModified(key) <= old.checked) return@Query old.copy(checked = version)

            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    override val childItems: Query<MemberHandle, Map<String, MemberList<PistonType>>> = run {
        val collectFn = fn@{ key: MemberHandle, _: QueryVersion ->
            val res = mutableMapOf<String, MutableMemberList<PistonType>>()

            val node = (nodeFromMemberHandle(key) ?: return@fn emptyMap<String, MemberList<PistonType>>()).asRoot()

            node.lastDirectChild(PistonType.statementBlock)?.let { block ->
                block.childSequence
                    .filter { child -> child.type in PistonSyntaxSets.defs }
                    .forEach { child -> defNodeToReference(child, res) }
            }

            res.mapValues { (_, list) -> list.toImmutable() }
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            if (ast.lastModified(key.findFile()) <= old.checked) return@Query old.copy(checked = version)

            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    override val typeParams: Query<MemberHandle, List<Pair<String, RelativeNodeLoc<PistonType>>>> = run {
        val collectFn = fn@{ key: MemberHandle, _: QueryVersion ->
            (nodeFromMemberHandle(key) ?: return@fn emptyList<Pair<String, RelativeNodeLoc<PistonType>>>())
                .firstDirectChildOr(PistonType.typeParams) { return@fn emptyList<Pair<String, RelativeNodeLoc<PistonType>>>() }
                .asRoot().childSequence
                .filter { it.type == PistonType.identifier }
                .map { it.content to it.location }
                .toList()
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            if (ast.lastModified(key.findFile()) <= old.checked) return@Query old.copy(checked = version)

            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    val fileImportData: Query<FileHandle, ImportData> = run {
        val collectFn = fn@{ key: FileHandle, _: QueryVersion ->
            val fileAst = ast[key]

            val importNode = fileAst
                .firstDirectRawChildOr(PistonType.import) { return@fn emptyImportData }
                .firstDirectRawChildOr(PistonType.importGroup) { return@fn emptyImportData }
                .asRedRoot()

            val tree = instance.packageTree[Unit]
            val deps = mutableListOf<HandleData<PistonType>>()
            val nameMap = hashMapOf<String, MutableList<Int>>()
            handleImportGroup(tree, importNode, deps, nameMap)

            ImportData(deps, nameMap)
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    private fun handleImportGroup(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ) = node.childSequence
        .filter { it.type == PistonType.importSegment }
        .forEach { segment -> handleImportSegment(pack, segment, deps, nameMap) }

    private fun handleImportSegment(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ) {
        val group = node.lastDirectChild(PistonType.importGroup)
        val path = node.firstDirectChild(PistonSyntaxSets.importPath)!!

        if (group == null) {
            return handleDirectImportPath(pack, path, deps, nameMap)
        }

        val newPack = handleImportPath(pack, path, deps)
        handleImportGroup(newPack, group, deps, nameMap)
    }

    private fun handleImportPath(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
    ): PackageTree? {
        val identNode: RedNode<PistonType>
        val currPackage: PackageTree? = if (node.type == PistonType.identifier) {
            identNode = node
            pack
        } else {
            val iter = node.childIterator
            val left = iter.findFirst { it.type in PistonSyntaxSets.importPath }!!
            val newPackage = handleImportPath(pack, left, deps)
            identNode = iter.findFirst { it.type == PistonType.identifier } ?: return null
            newPackage
        }

        val newName = identNode.content
        val subpackage = currPackage?.let { it.children[newName] } ?: run {
            deps.add(HandleData(identNode.location, nullReferenceList))
            return null
        }

        return subpackage
    }

    private fun handleDirectImportPath(
        pack: PackageTree?,
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ) {
        val identNode: RedNode<PistonType>
        val currPackage: PackageTree? = if (node.type == PistonType.identifier) {
            identNode = node
            pack
        } else {
            val iter = node.childIterator
            val left = iter.findFirst { it.type in PistonSyntaxSets.importPath }!!
            val newPackage = handleImportPath(pack, left, deps)
            identNode = iter.findFirst { it.type == PistonType.identifier } ?: return
            newPackage
        }

        val index = deps.size

        if (currPackage == null) {
            deps.add(HandleData(identNode.location, nullReferenceList))
            return
        }

        val newName = identNode.content
        val handleList = instance.packageItems[currPackage.handle][newName]
        val handles = handleList?.assertNonEmpty() ?: nullReferenceList
        deps.add(HandleData(identNode.location, handles))
        nameMap.getOrPut(newName) { mutableListOf() }.add(index)
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
            (nodeFromMemberHandle(key) ?: return@fn emptyList<RelativeNodeLoc<PistonType>>())
                .firstDirectChildOr(PistonType.functionParams) { return@fn emptyList<RelativeNodeLoc<PistonType>>() }
                .let { listOf(it.location) }
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            if (ast.lastModified(key.findFile()) <= old.checked) return@Query old.copy(checked = version)

            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    val supertypes: Query<NewTypeHandle, SupertypeData> = run {
        val collectFn = fn@{ key: NewTypeHandle, _: QueryVersion ->
            val node = (nodeFromMemberHandle(key) ?: return@fn errorSupertypeData)
                .firstDirectChildOr(PistonType.supertypes) { return@fn emptySuperTypeData }
                .firstDirectChildOr(PistonType.intersectionType) { return@fn errorSupertypeData }
                .asRoot()

            val deps = mutableListOf<HandleData<PistonType>>()
            val scope = buildTypeParamScopeFor(key)

            val types = node.childSequence
                .filter { it.type in PistonSyntaxSets.types }
                .map { handleTypeNode(it, deps, scope, false) }
                .toList()

            if (types.isEmpty()) emptySuperTypeData else SupertypeData(deps, types.assertNonEmpty())
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            if (ast.lastModified(key.findFile()) <= old.checked) return@Query old.copy(checked = version)

            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    val returnType: Query<TypedHandle, ReturnData> = run {
        val collectFn = fn@{ key: TypedHandle, _: QueryVersion ->
            val node = (nodeFromMemberHandle(key) ?: return@fn errorReturnData)
                .firstDirectChildOr(PistonType.typeAnnotation) { return@fn unitReturnData }
                .firstDirectChildOr(PistonSyntaxSets.types) { return@fn errorReturnData }
                .asRoot()

            val deps = mutableListOf<HandleData<PistonType>>()
            val scope = buildTypeParamScopeFor(key)

            val type = handleTypeNode(node, deps, scope, false)

            ReturnData(deps, type)
        }
        Query(instance.versionData, collectFn) { key, old, version ->
            if (ast.lastModified(key.findFile()) <= old.checked) return@Query old.copy(checked = version)

            val new = collectFn(key, version)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    private fun handleTypeNode(
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        scope: Scope,
        nullable: Boolean,
    ): TypeInstance = when (node.type) {
        PistonType.nullableType -> handleNestedTypeNode(node, deps, scope, nullable)
        PistonType.nestedType -> handleNullableTypeNode(node, deps, scope)
        PistonType.pathType -> handlePathType(node, deps, scope, nullable)
        else -> error("Error with the types syntax set")
    }

    private fun handlePathType(
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        scope: Scope,
        nullable: Boolean,
    ): TypeInstance {
        val path = node.firstDirectChildOr(PistonSyntaxSets.typePath) { return errorInstance }
        return handleTypePath(path, deps, scope, nullable)
    }

    private fun handleTypePath(
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        scope: Scope,
        nullable: Boolean,
    ): TypeInstance {
        if (node.type == PistonType.pathSegment) {
            return handlePathSegmentType(node, scope, deps, nullable)
        }

        var level = 0
        var currNode = node

        while (currNode.type == PistonType.pathAccess) {
            currNode = node.firstDirectChild(PistonSyntaxSets.typePath)!!
            level++
        }

        currNode = currNode.parent!!
        level -= 1

        val firstIdent = currNode.firstDirectChild(PistonType.identifier) ?: return errorInstance
        val firstPacks = scope.find(firstIdent.content) { it.itemType == ItemType.Package }

        if (firstPacks.isNotEmpty())
            deps.add(HandleData(firstIdent.location, firstPacks.assertNonEmpty()))

        if (firstPacks.size != 1)
            return errorInstance

        var pack = instance.packageTree[Unit].nodeFor(firstPacks.first() as PackageHandle)!!

        while (level != 1) {
            val identNode = currNode
                .firstDirectChildOr(PistonType.pathSegment) { return errorInstance }
                .firstDirectChildOr(PistonType.identifier) { return errorInstance }

            pack = pack.children[identNode.content] ?: return errorInstance
            deps.add(HandleData(identNode.location, nonEmptyListOf(pack.handle)))

            currNode = currNode.parent!!
            level -= 1
        }

        val lastSegment = currNode.firstDirectChild(PistonType.pathSegment) ?: return errorInstance
        val packScope = StaticScope(null, instance.packageItems[pack.handle])
        return handlePathSegmentType(lastSegment, packScope, deps, nullable)
    }

    private fun handlePathSegmentType(
        node: RedNode<PistonType>,
        scope: Scope,
        deps: MutableList<HandleData<PistonType>>,
        nullable: Boolean
    ): TypeInstance {
        val ident = node.firstDirectChild(PistonType.identifier) ?: return errorInstance
        val types = scope.find(ident.content) { it.itemType.type }

        if (types.isNotEmpty())
            deps.add(HandleData(ident.location, types.assertNonEmpty()))

        val args = node.firstDirectChild(PistonType.typeArgs)
            ?.childSequence
            ?.filter { it.type == PistonType.typeArg }
            ?.map { argNode ->
                val typeNode = argNode.firstDirectChild(PistonSyntaxSets.types)
                if (typeNode == null)
                    errorInstance
                else
                    handleTypeNode(typeNode, deps, scope, false)
            }?.toList() ?: emptyList()

        if (types.size != 1)
            return errorInstance

        val type = types.first() as TypeHandle

        val expectedArgs = if (type is NewTypeHandle) instance.typeParams[type].size else 0

        // TODO: Keep track of errors
        val endArgs = when {
            args.size < expectedArgs -> args + List(expectedArgs - args.size) { errorInstance }
            args.size > expectedArgs -> args.dropLast(args.size - expectedArgs)
            else -> args
        }

        return TypeInstance(type, endArgs, nullable)
    }

    private fun handleNestedTypeNode(
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        scope: Scope,
        nullable: Boolean,
    ): TypeInstance {
        val child = node.firstDirectChild(PistonSyntaxSets.types) ?: return errorInstance
        return handleTypeNode(child, deps, scope, nullable)
    }

    private fun handleNullableTypeNode(
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        scope: Scope,
    ): TypeInstance {
        val child = node.firstDirectChild(PistonSyntaxSets.types) ?: return errorInstance
        return handleTypeNode(child, deps, scope, true)
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
        if (stack.isEmpty())
            return node

        val itemRef = stack.pop()
        val pos = childItems[parentRef][itemRef.name]?.get(itemRef.memberType, itemRef.id) ?: return null
        return findNode(node.findAtRelative(pos) ?: return null, stack, itemRef)
    }

    fun nodeFromMemberHandle(ref: MemberHandle): RedNode<PistonType>? {
        val refStack = Stack<MemberHandle>()
        val fileRef = findFile(ref, refStack)
        val node = ast[fileRef].asRedRoot()
        val firstRef = refStack.pop()
        val firstPos = fileItems[fileRef][firstRef.name]?.get(firstRef.memberType, firstRef.id) ?: return null
        return findNode(node.findAtRelative(firstPos) ?: return null, refStack, firstRef)
    }

    private fun buildFileScope(fileHandle: FileHandle): Scope {
        val pack = instance.filePackage[fileHandle]
        val packItems = instance.packageItems[pack]
        val importData = fileImportData[fileHandle]
        val packScope = StaticScope(BaseScope, packItems)

        return ImportScope(packScope, importData)
    }

    private fun buildStaticMemberScope(handle: MemberHandle, withTypeParams: Boolean): Scope {
        val parentScope =
            if (withTypeParams) buildTypeParamScopeFor(handle)
            else buildStaticParentScope(handle.parent, false)
        val items = instance.childItems[handle]

        return StaticTypeScope(parentScope, items)
    }

    private fun buildStaticParentScope(handle: ParentHandle, withTypeParams: Boolean) =
        if (handle.isFile) buildFileScope(handle as FileHandle)
        else buildStaticMemberScope(handle as MemberHandle, withTypeParams)

    private fun buildTypeParamScopeFor(handle: MemberHandle): Scope {
        val parent = handle.parent

        val parentScope = buildStaticParentScope(parent, !handle.itemType.type)
        val typeParams = instance.typeParams[handle]

        return StaticScope(parentScope, typeParams)
    }
}