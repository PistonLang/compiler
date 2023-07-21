package pistonlang.compiler.common.main

import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.language.*
import pistonlang.compiler.common.parser.RelativeNodeLoc
import pistonlang.compiler.common.queries.ConstantQuery
import pistonlang.compiler.common.queries.Query

internal class DefaultHandler : LanguageHandler<Nothing> {
    override val childItems: Query<MemberId, MemberList<Nothing>> =
        ConstantQuery(emptyMemberList)

    override val constructors: Query<TypeId, List<RelativeNodeLoc<Nothing>>> =
        ConstantQuery(emptyList())
    override val extension: String get() = ""

    override val fileItems: Query<FileId, MemberList<Nothing>> =
        ConstantQuery(emptyMemberList)

    override val params: Query<MemberId, ParamData<Nothing>> =
        ConstantQuery(emptyParamData)

    override val returnType: Query<MemberId, ReturnData<Nothing>> =
        ConstantQuery(errorReturnData)

    override val supertypes: Query<TypeId, SupertypeData<Nothing>> =
        ConstantQuery(errorSupertypeData)

    override val typeParamBounds: Query<MemberId, TypeBoundData<Nothing>> =
        ConstantQuery(emptyTypeBoundData)

    override val typeParams: Query<MemberId, List<Pair<String, RelativeNodeLoc<Nothing>>>> =
        ConstantQuery(emptyList())
}