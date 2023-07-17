package pistonlang.compiler.common.main

import pistonlang.compiler.common.files.FilePath
import pistonlang.compiler.common.files.PackagePath
import pistonlang.compiler.common.items.*
import pistonlang.compiler.common.items.handles.MemberHandle
import pistonlang.compiler.common.items.handles.TypeParamHandle

typealias MemberInterner = Interner<MemberHandle, MemberId>
typealias TypeInterner = Interner<MemberId, TypeId>
typealias TypeParamInterner = Interner<TypeParamHandle, TypeParamId>
typealias FileInterner = Interner<FilePath, FileId>
typealias PackageInterner = Interner<PackagePath, PackageId>

interface MainInterners {
    val memberIds: MemberInterner
    val typeIds: TypeInterner
    val typeParamIds: TypeParamInterner
}

interface PathInterners {
    val fileIds: FileInterner
    val packIds: PackageInterner
}

internal class DefaultInterners : MainInterners {
    override val memberIds = MapBasedInterner<MemberHandle, MemberId>(::MemberId)
    override val typeIds = ListBasedInterner<MemberId, TypeId>(::TypeId)
    override val typeParamIds = MapBasedInterner<TypeParamHandle, TypeParamId>(::TypeParamId)
}

internal class DefaultPathInterners : PathInterners {
    override val fileIds = MapBasedInterner<FilePath, FileId>(::FileId)
    override val packIds = MapBasedInterner<PackagePath, PackageId>(::PackageId)
}