package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.main.FileInputQueries
import pistonlang.compiler.common.main.GeneralQueries
import pistonlang.compiler.common.parser.Lexer
import pistonlang.compiler.common.parser.Parser
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.common.parser.nodes.*
import pistonlang.compiler.common.queries.DependentQuery
import pistonlang.compiler.common.queries.Query
import pistonlang.compiler.common.queries.QueryAccessor
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.common.types.TypeInstance
import pistonlang.compiler.common.types.errorInstance
import pistonlang.compiler.piston.parser.PistonSyntaxSets
import pistonlang.compiler.piston.parser.PistonType
import pistonlang.compiler.util.assertNonEmpty
import pistonlang.compiler.util.findFirst
import pistonlang.compiler.util.nonEmptyListOf

val emptyNode = GreenLeaf(PistonType.file, "")

/**
 * The standard [LanguageHandler] for Piston code
 */
class PistonLanguageHandler(
    lexer: (String) -> Lexer<PistonType>,
    parsing: (Parser<PistonType>) -> GreenNode<PistonType>,
    versionData: QueryVersionData,
    private val inputQueries: FileInputQueries,
    private val generalQueries: GeneralQueries,
) : LanguageHandler<PistonType> {
    override val extensions: List<String> = listOf("pi")

    private val ast: DependentQuery<FileHandle, GreenNode<PistonType>> =
        DependentQuery(versionData, equalityFun = { _, _ -> false }) { key: FileHandle ->
            val data = inputQueries.code[key]
            if (!data.valid) emptyNode else {
                val parser = Parser(lexer(data.code), PistonType.file)
                parsing(parser)
            }
        }

    context(QueryAccessor)
    private fun getASTNode(key: MemberHandle): GreenNode<PistonType>? {
        val parent = key.parent
        return if (parent.isFile) {
            val node = ast[parent as FileHandle]
            val pos = fileItems[parent][key.name]?.get(key.memberType, key.id) ?: return null
            node.asRoot().findAtRelative(pos)?.green
        } else {
            val node = astNode[parent as MemberHandle] ?: return null
            val pos = childItems[parent][key.name]?.get(key.memberType, key.id) ?: return null
            node.asRoot().findAtRelative(pos)?.green
        }
    }

    private val astNode: DependentQuery<MemberHandle, GreenNode<PistonType>?> =
        DependentQuery(versionData, computeFn = ::getASTNode)

    context(QueryAccessor)
    fun parentRelativeLocation(handle: MemberHandle): RelativeNodeLoc<PistonType>? {
        val parent = handle.parent
        return (if (parent.isFile) fileItems[parent as FileHandle] else childItems[parent as MemberHandle])[handle.name]
            ?.get(handle.memberType, handle.id)
            ?: return null
    }

    override val fileItems: Query<FileHandle, Map<String, MemberList<PistonType>>> =
        DependentQuery(versionData) { key: FileHandle ->
            val res = mutableMapOf<String, MutableMemberList<PistonType>>()

            ast[key].childSequence
                .filter { child -> child.type in PistonSyntaxSets.defs }
                .forEach { child -> defNodeToReference(child, res) }

            res.mapValues { (_, list) -> list.toImmutable() }
        }

    override val childItems: Query<MemberHandle, Map<String, MemberList<PistonType>>> =
        DependentQuery(versionData) fn@{ key: MemberHandle ->
            val res = mutableMapOf<String, MutableMemberList<PistonType>>()

            val node = (astNode[key] ?: return@fn emptyMap<String, MemberList<PistonType>>()).asRoot()

            node.lastDirectChild(PistonType.statementBlock)?.let { block ->
                block.childSequence
                    .filter { child -> child.type in PistonSyntaxSets.defs }
                    .forEach { child -> defNodeToReference(child, res) }
            }

            res.mapValues { (_, list) -> list.toImmutable() }
        }

    override val typeParams: Query<MemberHandle, List<Pair<String, RelativeNodeLoc<PistonType>>>> =
        DependentQuery(versionData) fn@{ key: MemberHandle ->
            (astNode[key] ?: return@fn emptyList())
                .firstDirectRawChildOr(PistonType.typeParams) { return@fn emptyList<Pair<String, RelativeNodeLoc<PistonType>>>() }
                .asRoot().childSequence
                .filter { it.type == PistonType.identifier }
                .map { it.content to it.location }
                .toList()
        }

    val fileImportData: Query<FileHandle, ImportData> =
        DependentQuery(versionData) fn@{ key: FileHandle ->
            val fileAst = ast[key]

            val importNode = fileAst
                .firstDirectRawChildOr(PistonType.import) { return@fn emptyImportData }
                .firstDirectRawChildOr(PistonType.importGroup) { return@fn emptyImportData }
                .asRoot()

            val deps = mutableListOf<HandleData<PistonType>>()
            val nameMap = hashMapOf<String, MutableList<Int>>()
            handleImportGroup(rootPackage, importNode, deps, nameMap)

            ImportData(deps, nameMap)
        }

    context(QueryAccessor)
    private fun handleImportGroup(
        pack: PackageHandle?,
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ) = node.childSequence
        .filter { it.type == PistonType.importSegment }
        .forEach { segment -> handleImportSegment(pack, segment, deps, nameMap) }

    context(QueryAccessor)
    private fun handleImportSegment(
        pack: PackageHandle?,
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
        pack: PackageHandle?,
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
    ): PackageHandle? {
        val identNode: RedNode<PistonType>
        val currPackage: PackageHandle? = if (node.type == PistonType.identifier) {
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
            deps.add(HandleData(identNode.location, nullReferenceList))
            return null
        }

        val newName = identNode.content
        val subpackage = currPackage.subPackage(newName)

        return if (generalQueries.packageHandleNode[subpackage] == null) {
            deps.add(HandleData(identNode.location, nullReferenceList))
            return null
        } else {
            deps.add(HandleData(identNode.location, nonEmptyListOf(subpackage)))
            subpackage
        }
    }

    context(QueryAccessor)
    private fun handleDirectImportPath(
        pack: PackageHandle?,
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        nameMap: MutableMap<String, MutableList<Int>>,
    ) {
        val identNode: RedNode<PistonType>
        val currPackage: PackageHandle? = if (node.type == PistonType.identifier) {
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
        val handleList = generalQueries.packageItems[currPackage][newName]
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

    override val constructors: Query<MultiInstanceClassHandle, List<RelativeNodeLoc<PistonType>>> =
        DependentQuery(versionData) fn@{ key: MultiInstanceClassHandle ->
            (astNode[key] ?: return@fn emptyList())
                .firstDirectChildOr(PistonType.functionParams) { return@fn emptyList<RelativeNodeLoc<PistonType>>() }
                .let { listOf(it.parentRelativeLocation) }
        }

    override val supertypes: Query<NewTypeHandle, SupertypeData> =
        DependentQuery(versionData) fn@{ key: NewTypeHandle ->
            val node = (astNode[key] ?: return@fn errorSupertypeData)
                .firstDirectRawChildOr(PistonType.supertypes) { return@fn emptySuperTypeData }
                .firstDirectRawChildOr(PistonType.intersectionType) { return@fn errorSupertypeData }
                .asRoot()

            val deps = mutableListOf<HandleData<PistonType>>()
            val scope = typeParamScope[key]

            val types = node.childSequence
                .filter { it.type in PistonSyntaxSets.types }
                .map { handleTypeNode(it, deps, scope, false) }
                .toList()

            if (types.isEmpty()) emptySuperTypeData else SupertypeData(deps, types.assertNonEmpty())
        }

    val returnType: Query<TypedHandle, ReturnData> =
        DependentQuery(versionData) fn@{ key: TypedHandle ->
            val node = (astNode[key] ?: return@fn errorReturnData)
                .firstDirectRawChildOr(PistonType.typeAnnotation) { return@fn unitReturnData }
                .firstDirectRawChildOr(PistonSyntaxSets.types) { return@fn errorReturnData }
                .asRoot()

            val deps = mutableListOf<HandleData<PistonType>>()
            val scope = typeParamScope[key]

            val type = handleTypeNode(node, deps, scope, false)

            ReturnData(deps, type)
        }

    val params: Query<ParameterizedHandle, ParamData> =
        DependentQuery(versionData) fn@{ key: ParameterizedHandle ->
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
                        ?: errorInstance
                }
                .toList()

            ParamData(deps, params)
        }

    val typeParamBounds: DependentQuery<MemberHandle, TypeBoundData> =
        DependentQuery(versionData) fn@{ key: MemberHandle ->
            val params = generalQueries.typeParams[key]
            if (params.isEmpty()) return@fn emptyTypeBoundData
            val node = astNode[key]!!
                .firstDirectRawChild(PistonType.typeParams)!!
                .firstDirectRawChildOr(PistonType.typeGuard) {
                    return@fn TypeBoundData(emptyList(), List(params.size) { emptyList<TypeInstance>() })
                }
                .asRoot()

            val deps = mutableListOf<HandleData<PistonType>>()
            val scope = typeParamScope[key]

            val res = MutableList(params.size) { mutableListOf<TypeInstance>() }

            node.childSequence
                .filter { it.type == PistonType.typeBound }
                .forEach { bound ->
                    // TODO: Handle errors
                    val ident = bound.firstDirectChild(PistonType.identifier) ?: return@forEach
                    val name = ident.content
                    val param = params[name]?.first() ?: return@forEach
                    deps.add(HandleData(ident.location, nonEmptyListOf(param)))
                    val typeNode = bound.firstDirectChild(PistonSyntaxSets.types)
                    val instance =
                        if (typeNode == null) errorInstance
                        else handleTypeNode(typeNode, deps, scope, false)
                    res[param.id].add(instance)
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
        val path = node.firstDirectChildOr(PistonSyntaxSets.typePath) { return errorInstance }
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

        val firstIdent = currNode.firstDirectChild(PistonType.identifier) ?: return errorInstance
        val firstPacks = scope.find(firstIdent.content) { it.itemType == ItemType.Package }

        if (firstPacks.isNotEmpty())
            deps.add(HandleData(firstIdent.location, firstPacks.assertNonEmpty()))

        if (firstPacks.size != 1)
            return errorInstance

        var pack = firstPacks.first() as PackageHandle

        while (level != 1) {
            val identNode = currNode
                .firstDirectChildOr(PistonType.pathSegment) { return errorInstance }
                .firstDirectChildOr(PistonType.identifier) { return errorInstance }

            pack = pack.subPackage(identNode.content)

            if (generalQueries.packageHandleNode[pack] == null) return errorInstance
            deps.add(HandleData(identNode.location, nonEmptyListOf(pack)))

            currNode = currNode.parent!!
            level -= 1
        }

        val lastSegment = currNode.firstDirectChild(PistonType.pathSegment) ?: return errorInstance
        val packScope = StaticScope(null, generalQueries.packageItems[pack])
        return handlePathSegmentType(lastSegment, packScope, deps, nullable)
    }

    context(QueryAccessor)
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

        val expectedArgs = if (type is NewTypeHandle) generalQueries.typeParams[type].size else 0

        // TODO: Keep track of errors
        val endArgs = when {
            args.size < expectedArgs -> args + List(expectedArgs - args.size) { errorInstance }
            args.size > expectedArgs -> args.dropLast(args.size - expectedArgs)
            else -> args
        }

        return TypeInstance(type, endArgs, nullable)
    }

    context(QueryAccessor)
    private fun handleNestedTypeNode(
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        scope: Scope,
        nullable: Boolean,
    ): TypeInstance {
        val child = node.firstDirectChild(PistonSyntaxSets.types) ?: return errorInstance
        return handleTypeNode(child, deps, scope, nullable)
    }

    context(QueryAccessor)
    private fun handleNullableTypeNode(
        node: RedNode<PistonType>,
        deps: MutableList<HandleData<PistonType>>,
        scope: Scope,
    ): TypeInstance {
        val child = node.firstDirectChild(PistonSyntaxSets.types) ?: return errorInstance
        return handleTypeNode(child, deps, scope, true)
    }

    private val fileScope: Query<FileHandle, Scope> = DependentQuery(versionData) { key ->
        val pack = inputQueries.filePackage[key]!!
        val packItems = generalQueries.packageItems[pack]
        val importData = fileImportData[key]
        val packScope = StaticScope(BaseScope, packItems)

        ImportScope(packScope, importData)
    }

    private val staticScopeWithTypeParams: Query<MemberHandle, Scope> = DependentQuery(versionData) { key ->
        val parentScope = typeParamScope[key]
        val items = generalQueries.childItems[key]

        StaticTypeScope(parentScope, items)
    }

    private val staticScopeWithoutTypeParams: Query<MemberHandle, Scope> = DependentQuery(versionData) { key ->
        val parentScope = getStaticParentScope(key.parent, false)
        val items = generalQueries.childItems[key]

        StaticTypeScope(parentScope, items)
    }

    context(QueryAccessor)
    private fun getStaticParentScope(handle: ParentHandle, withTypeParams: Boolean) = when {
        handle.isFile -> fileScope[handle as FileHandle]
        withTypeParams -> staticScopeWithTypeParams[handle as MemberHandle]
        else -> staticScopeWithoutTypeParams[handle as MemberHandle]
    }

    private val typeParamScope: Query<MemberHandle, Scope> = DependentQuery(versionData) { key ->
        val parent = key.parent

        val parentScope = getStaticParentScope(parent, !key.itemType.type)
        val typeParams = generalQueries.typeParams[key]

        StaticScope(parentScope, typeParams)
    }
}