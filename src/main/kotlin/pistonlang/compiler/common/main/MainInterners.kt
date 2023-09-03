package pistonlang.compiler.common.main

import pistonlang.compiler.common.files.FilePath
import pistonlang.compiler.common.files.PackagePath
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.items.handles.MemberHandle
import pistonlang.compiler.common.items.handles.TypeParamHandle
import pistonlang.compiler.common.items.handles.TypeVarHandle

typealias MemberInterner = Interner<MemberHandle, MemberId>
typealias TypeInterner = Interner<MemberId, TypeId>
typealias TypeParamInterner = Interner<TypeParamHandle, TypeParamId>
typealias FileInterner = Interner<FilePath, FileId>
typealias PackageInterner = Interner<PackagePath, PackageId>
typealias TypeVarInterner = Interner<TypeVarHandle, TypeVarId>

interface MainInterners {
    val memberIds: MemberInterner
    val typeIds: TypeInterner
    val typeParamIds: TypeParamInterner
    val fileIds: FileInterner
    val packIds: PackageInterner
    val typeVars: TypeVarInterner
}

internal interface InstanceInterners : MainInterners {
    override val memberIds: MutableInterner<MemberHandle, MemberId>
    override val typeIds: MutableInterner<MemberId, TypeId>
    override val typeParamIds: MutableInterner<TypeParamHandle, TypeParamId>
    override val typeVars: MutableInterner<TypeVarHandle, TypeVarId>
}

internal class DefaultInterners : InstanceInterners {
    override val memberIds = MapBasedInterner<MemberHandle, MemberId>(::MemberId)
    override val typeIds = ListBasedInterner<MemberId, TypeId>(::TypeId)
    override val typeParamIds = MapBasedInterner<TypeParamHandle, TypeParamId>(::TypeParamId)
    override val fileIds = MapBasedInterner<FilePath, FileId>(::FileId)
    override val packIds = MapBasedInterner<PackagePath, PackageId>(::PackageId)
    override val typeVars = MapBasedInterner<TypeVarHandle, TypeVarId>(::TypeVarId)
}