package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.files.PackagePath
import pistonlang.compiler.common.files.rootPackage
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.items.handles.ItemType
import pistonlang.compiler.common.items.handles.ParentHandle
import pistonlang.compiler.common.items.handles.asItem
import pistonlang.compiler.common.language.*
import pistonlang.compiler.common.main.FileInputQueries
import pistonlang.compiler.common.main.MainInterners
import pistonlang.compiler.common.main.MainQueries
import pistonlang.compiler.common.parser.Lexer
import pistonlang.compiler.common.parser.Parser
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.common.parser.nodes.*
import pistonlang.compiler.common.queries.*
import pistonlang.compiler.common.types.TypeInstance
import pistonlang.compiler.common.types.TypeParamBound
import pistonlang.compiler.common.types.unknownInstance
import pistonlang.compiler.common.types.unspecifiedInstance
import pistonlang.compiler.piston.parser.PistonSyntaxSets
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.findFirst
import pistonlang.compiler.util.lists.assertNonEmpty
import pistonlang.compiler.util.lists.nonEmptyListOf

val emptyNode = GreenLeaf(PistonType.file, "")

/**
 * The standard [LanguageHandler] for Piston code
 */
class PistonLanguageHandler(
    lexer: (String) -> Lexer<PistonType>,
    parsing: (Parser<PistonType>) -> GreenNode<PistonType>,
    versionData: QueryVersionData,
    private val inputQueries: FileInputQueries,
    private val mainQueries: MainQueries,
    private val interners: MainInterners,
) : LanguageHandler<PistonType> {
    override val extension: String get() = "pi"

    private val constants: SingletonQuery<PistonConstants> = DependentSingletonQuery(versionData) {
        val defaultImports = interners.packIds[PackagePath("piston")]?.let { pistonId ->
            mainQueries
                .packageTreeIterator(pistonId)
                .asSequence()
                .flatMap { id ->
                    mainQueries
                        .packageItems[id]
                        .asSequence()
                        .flatMap { (key, value) ->
                            value
                                .filter { it.type == ItemType.Member }
                                .map { key to it }
                        }
                }
                .groupBy({ it.first }) { it.second }
        } ?: emptyMap()

        val stlTypes = mainQueries.stlTypes.value

        PistonConstants(
            baseScope = StaticScope(null, defaultImports),
            errorSupertypeData = SupertypeData(emptyList(), nonEmptyListOf(unknownInstance)),
            emptySupertypeData = SupertypeData(emptyList(), nonEmptyListOf(stlTypes.anyInstance)),
            unitReturnData = ReturnData(emptyList(), stlTypes.unitInstance),
        )
    }

    private val ast: Query<FileId, GreenNode<PistonType>> =
        DependentQuery(versionData, equalityFun = { _, _ -> false }) { key ->
            val data = inputQueries.code[key]
            if (!data.valid) emptyNode else {
                val parser = Parser(lexer(data.code), PistonType.file)
                parsing(parser)
            }
        }

    context(QueryAccessor)
    private fun getASTNode(key: MemberId): GreenNode<PistonType>? {
        val handle = interners.memberIds.getKey(key)

        return handle.parent.match(
            onFile = { parent ->
                val node = ast[parent]
                val pos = fileItems[parent][handle.type, handle.name, handle.id] ?: return null
                node.asRoot().findAtRelative(pos)?.green
            },
            onMember = { parent ->
                val node = astNode[parent] ?: return null
                val pos = childItems[parent][handle.type, handle.name, handle.id] ?: return null
                node.asRoot().findAtRelative(pos)?.green
            }
        )
    }

    private val astNode: Query<MemberId, GreenNode<PistonType>?> =
        DependentQuery(versionData, computeFn = ::getASTNode)

    override val fileItems: Query<FileId, MemberList<PistonType>> = DependentQuery(versionData) { key ->
        val res = MutableMemberList<PistonType>()

        ast[key].childSequence
            .filter { child -> child.type in PistonSyntaxSets.defs }
            .forEach { child -> defNodeToReference(child, res) }

        res.toImmutable()
    }

    override val childItems: Query<MemberId, MemberList<PistonType>> = DependentQuery(versionData) fn@{ key ->
        val res = MutableMemberList<PistonType>()

        val node = (astNode[key] ?: return@fn emptyMemberList).asRoot()

        node.lastDirectChild(PistonType.statementBlock)?.let { block ->
            block.childSequence
                .filter { child -> child.type in PistonSyntaxSets.defs }
                .forEach { child -> defNodeToReference(child, res) }
        }

        res.toImmutable()
    }

    override val typeParams: Query<MemberId, List<Pair<String, RelativeNodeLoc<PistonType>>>> =
        DependentQuery(versionData) fn@{ key ->
            (astNode[key] ?: return@fn emptyList())
                .firstDirectRawChildOr(PistonType.typeParams) { return@fn emptyList<Pair<String, RelativeNodeLoc<PistonType>>>() }
                .asRoot().childSequence
                .filter { it.type == PistonType.identifier }
                .map { it.content to it.location }
                .toList()
        }

    internal val fileImportData: Query<FileId, ImportData> = DependentQuery(versionData) fn@{ key ->
        val fileAst = ast[key]

        val importNode = fileAst
            .firstDirectRawChildOr(PistonType.import) { return@fn emptyImportData }
            .firstDirectRawChildOr(PistonType.importGroup) { return@fn emptyImportData }
            .asRoot()

        val deps = mutableListOf<HandleData<PistonType>>()
        val nameMap = hashMapOf<String, MutableList<Int>>()
        handleImportGroup(interners.packIds[rootPackage], importNode, deps, nameMap)

        ImportData(deps, nameMap)
    }

    context(QueryAccessor)
    private fun handleImportGroup(
        pack: PackageId?,
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ) = node.childSequence
        .filter { it.type == PistonType.importSegment }
        .forEach { segment -> handleImportSegment(pack, segment, deps, nameMap) }

    context(QueryAccessor)
    private fun handleImportSegment(
        pack: PackageId?,
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

    context(QueryAccessor)
    private fun handleImportPath(
        pack: PackageId?,
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
    ): PackageId? {
        val identNode: RedNode<PistonType>
        val currPackage: PackageId? = if (node.type == PistonType.identifier) {
            identNode = node
            pack
        } else {
            val iter = node.childIterator
            val left = iter.findFirst { it.type in PistonSyntaxSets.importPath }!!
            val newPackage = handleImportPath(pack, left, deps) ?: return null
            identNode = iter.findFirst { it.type == PistonType.identifier } ?: return null
            newPackage
        }

        if (currPackage == null) {
            deps.add(HandleData(identNode.location, invalidPathHandleList))
            return null
        }

        val newName = identNode.content
        val subpackage = mainQueries.packageHandleNode[currPackage].children[newName]

        return if (subpackage == null) {
            deps.add(HandleData(identNode.location, invalidPathHandleList))
            return null
        } else {
            deps.add(HandleData(identNode.location, nonEmptyListOf(subpackage.asItem())))
            subpackage
        }
    }

    context(QueryAccessor)
    private fun handleDirectImportPath(
        pack: PackageId?,
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ) {
        val identNode: RedNode<PistonType>
        val currPackage: PackageId? = if (node.type == PistonType.identifier) {
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
            deps.add(HandleData(identNode.location, invalidPathHandleList))
            return
        }

        val newName = identNode.content
        val handleList = mainQueries.packageItems[currPackage][newName]
        val handles = handleList?.assertNonEmpty() ?: invalidPathHandleList
        deps.add(HandleData(identNode.location, handles))
        nameMap.getOrPut(newName) { mutableListOf() }.add(index)
    }

    private fun defNodeToReference(
        child: GreenChild<PistonType>,
        res: MutableMemberList<PistonType>
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
        res.add(type, name, loc)
    }

    private fun defNodeToReference(
        node: RedNode<PistonType>,
        res: MutableMemberList<PistonType>
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
        res.add(type, name, loc)
    }

    override val constructors: Query<TypeId, List<RelativeNodeLoc<PistonType>>> =
        DependentQuery(versionData) fn@{ key ->
            val memberId = interners.typeIds.getKey(key)
            (astNode[memberId] ?: return@fn emptyList())
                .firstDirectChildOr(PistonType.functionParams) { return@fn emptyList<RelativeNodeLoc<PistonType>>() }
                .let { listOf(it.parentRelativeLocation) }
        }

    override val supertypes: Query<TypeId, SupertypeData<PistonType>> =
        DependentQuery(versionData) fn@{ key ->
            val memberId = interners.typeIds.getKey(key)
            val consts = constants.value

            val node = (astNode[memberId] ?: return@fn consts.errorSupertypeData)
                .firstDirectRawChildOr(PistonType.supertypes) { return@fn consts.emptySupertypeData }
                .firstDirectRawChildOr(PistonType.intersectionType) { return@fn consts.errorSupertypeData }
                .asRoot()

            val deps = mutableListOf<HandleData<PistonType>>()
            val scope = typeParamScope[memberId]

            val types = node.childSequence
                .filter { it.type in PistonSyntaxSets.types }
                .map { handleTypeNode(it, deps, scope, false) }
                .toList()

            if (types.isEmpty()) consts.emptySupertypeData else SupertypeData(deps, types.assertNonEmpty())
        }

    override val returnType: Query<MemberId, ReturnData<PistonType>> =
        DependentQuery(versionData) fn@{ key ->
            val node = (astNode[key] ?: return@fn errorReturnData)
                .firstDirectRawChildOr(PistonType.typeAnnotation) { return@fn constants.value.unitReturnData }
                .firstDirectRawChildOr(PistonSyntaxSets.types) { return@fn errorReturnData }
                .asRoot()

            val deps = mutableListOf<HandleData<PistonType>>()
            val scope = typeParamScope[key]

            val type = handleTypeNode(node, deps, scope, false)

            ReturnData(deps, type)
        }

    override val params: Query<MemberId, ParamData<PistonType>> =
        DependentQuery(versionData) fn@{ key ->
            val node = astNode[key]
                ?.firstDirectRawChild(PistonType.functionParams)
                ?.asRoot()
                ?: return@fn emptyParamData

            val deps = mutableListOf<HandleData<PistonType>>()
            val scope = typeParamScope[key]

            val params = node.childSequence
                .filter { it.type == PistonType.functionParam }
                .map {
                    it
                        .firstDirectChild(PistonType.typeAnnotation)
                        ?.firstDirectChild(PistonSyntaxSets.types)
                        ?.let { typeNode -> handleTypeNode(typeNode, deps, scope, false) }
                        ?: unknownInstance
                }
                .toList()

            ParamData(deps, params)
        }

    override val typeParamBounds: Query<MemberId, TypeBoundData<PistonType>> = DependentQuery(versionData) fn@{ key ->
        val params = mainQueries.typeParams[key].nameMap
        if (params.isEmpty()) return@fn emptyTypeBoundData
        val node = astNode[key]
            ?.firstDirectRawChild(PistonType.typeParams)
            ?.firstDirectRawChild(PistonType.typeGuard)
            ?.asRoot() ?: return@fn emptyTypeBoundData

        val deps = mutableListOf<HandleData<PistonType>>()
        val scope = typeParamScope[key]

        val res = mutableListOf<TypeParamBound>()

        node.childSequence
            .filter { it.type == PistonType.typeBound }
            .forEach { bound ->
                val ident = bound.firstDirectChild(PistonType.identifier) ?: return@forEach
                val name = ident.content
                val param = params[name]?.first() ?: run {
                    deps.add(HandleData(ident.location, invalidPathHandleList))
                    return@forEach
                }
                deps.add(HandleData(ident.location, nonEmptyListOf(param.asItem())))
                val typeNode = bound.firstDirectChild(PistonSyntaxSets.types)
                val instance =
                    if (typeNode == null) unknownInstance
                    else handleTypeNode(typeNode, deps, scope, false)
                res.add(TypeParamBound(param, instance))
            }

        TypeBoundData(deps, res)
    }

    context(QueryAccessor)
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

    context(QueryAccessor)
    private fun handlePathType(
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        scope: Scope,
        nullable: Boolean,
    ): TypeInstance {
        val path = node.firstDirectChildOr(PistonSyntaxSets.typePath) { return unknownInstance }
        return handleTypePath(path, deps, scope, nullable)
    }

    context(QueryAccessor)
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

        val firstIdent = currNode.firstDirectChild(PistonType.identifier) ?: return unknownInstance
        val firstPacks = scope.find(firstIdent.content) { it.type == ItemType.Package }

        if (firstPacks.isNotEmpty())
            deps.add(HandleData(firstIdent.location, firstPacks.assertNonEmpty()))

        if (firstPacks.size != 1)
            return unknownInstance

        var pack = firstPacks.first().asPackage!!

        while (level != 1) {
            val identNode = currNode
                .firstDirectChildOr(PistonType.pathSegment) { return unknownInstance }
                .firstDirectChildOr(PistonType.identifier) { return unknownInstance }

            pack = mainQueries.packageHandleNode[pack].children[identNode.content] ?: return unknownInstance

            deps.add(HandleData(identNode.location, nonEmptyListOf(pack.asItem())))

            currNode = currNode.parent!!
            level -= 1
        }

        val lastSegment = currNode.firstDirectChild(PistonType.pathSegment) ?: return unknownInstance
        val packScope = StaticScope(null, mainQueries.packageItems[pack])
        return handlePathSegmentType(lastSegment, packScope, deps, nullable)
    }

    context(QueryAccessor)
    private fun handlePathSegmentType(
        node: RedNode<PistonType>,
        scope: Scope,
        deps: MutableList<HandleData<PistonType>>,
        nullable: Boolean
    ): TypeInstance {
        val ident = node.firstDirectChild(PistonType.identifier) ?: return unknownInstance
        val types = scope.find(ident.content) { item ->
            item.match(
                onPackage = { false },
                onMember = { interners.memberIds.getKey(it).type.newType },
                onTypeParam = { true },
                onError = { false }
            )
        }

        if (types.isNotEmpty())
            deps.add(HandleData(ident.location, types.assertNonEmpty()))

        val args = node.firstDirectChild(PistonType.typeArgs)
            ?.childSequence
            ?.filter { it.type == PistonType.typeArg }
            ?.map { argNode ->
                val typeNode = argNode.firstDirectChild(PistonSyntaxSets.types)
                if (typeNode == null)
                    unknownInstance
                else
                    handleTypeNode(typeNode, deps, scope, false)
            }?.toList() ?: emptyList()

        if (types.size != 1)
            return unknownInstance

        val first = types.first()

        val expectedArgs = first.asMember?.let { mainQueries.typeParams[it].ids.size } ?: 0

        val endArgs = when {
            args.size < expectedArgs -> args + List(expectedArgs - args.size) { unspecifiedInstance }
            args.size > expectedArgs -> args.dropLast(args.size - expectedArgs)
            else -> args
        }

        return TypeInstance(first.toTypeHandle(interners)!!, endArgs, nullable)
    }

    context(QueryAccessor)
    private fun handleNestedTypeNode(
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        scope: Scope,
        nullable: Boolean,
    ): TypeInstance {
        val child = node.firstDirectChild(PistonSyntaxSets.types) ?: return unknownInstance
        return handleTypeNode(child, deps, scope, nullable)
    }

    context(QueryAccessor)
    private fun handleNullableTypeNode(
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        scope: Scope,
    ): TypeInstance {
        val child = node.firstDirectChild(PistonSyntaxSets.types) ?: return unknownInstance
        return handleTypeNode(child, deps, scope, true)
    }

    private val fileScope: Query<FileId, Scope> = DependentQuery(versionData) { key ->
        val pack = inputQueries.filePackage[key]!!
        val packItems = mainQueries.packageItems[pack]
        val importData = fileImportData[key]
        val packScope = StaticScope(constants.value.baseScope, packItems)

        ImportScope(packScope, importData)
    }

    private val staticScopeWithTypeParams: Query<MemberId, Scope> = DependentQuery(versionData) { key ->
        val parentScope = typeParamScope[key]
        val items = mainQueries.childItems[key].mapValues { (_, value) -> value.map { it.asItem() } }

        StaticTypeScope(parentScope, items)
    }

    private val staticScopeWithoutTypeParams: Query<MemberId, Scope> = DependentQuery(versionData) { key ->
        val instance = interners.memberIds.getKey(key)
        val parentScope = getStaticParentScope(instance.parent, false)
        val items = mainQueries.childItems[key].mapValues { (_, value) -> value.map { it.asItem() } }

        StaticTypeScope(parentScope, items)
    }

    context(QueryAccessor)
    private fun getStaticParentScope(handle: ParentHandle, withTypeParams: Boolean) = handle.match(
        onFile = { fileScope[it] },
        onMember = { (if (withTypeParams) staticScopeWithTypeParams else staticScopeWithoutTypeParams)[it] }
    )

    private val typeParamScope: Query<MemberId, Scope> = DependentQuery(versionData) { key ->
        val handle = interners.memberIds.getKey(key)
        val parent = handle.parent

        val parentScope = getStaticParentScope(parent, !handle.type.newType)
        val typeParams = mainQueries.typeParams[key].nameMap.mapValues { (_, value) -> value.map { it.asItem() } }

        StaticScope(parentScope, typeParams)
    }
}