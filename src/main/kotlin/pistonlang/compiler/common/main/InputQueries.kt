package pistonlang.compiler.common.main

import kotlinx.collections.immutable.persistentMapOf
import pistonlang.compiler.common.files.FileData
import pistonlang.compiler.common.files.PackageTree
import pistonlang.compiler.common.files.invalidFileData
import pistonlang.compiler.common.items.FileId
import pistonlang.compiler.common.items.PackageId
import pistonlang.compiler.common.items.UnitId
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.queries.InputQuery
import pistonlang.compiler.common.queries.Query
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.common.queries.SingletonInputQuery

interface FileInputQueries {
    val filePackage: Query<FileId, PackageId?>
    val code: Query<FileId, FileData>
}

interface InputQueries : FileInputQueries {
    val packageTree: Query<UnitId, PackageTree>
    val fileHandler: Query<FileId, LanguageHandler<*>?>
}

internal class DefaultInputQueries(versionData: QueryVersionData) : InputQueries {
    override val code: InputQuery<FileId, FileData> =
        InputQuery(versionData) { invalidFileData }
    override val filePackage: InputQuery<FileId, PackageId?> =
        InputQuery(versionData) { null }
    override val packageTree: SingletonInputQuery<PackageTree> =
        SingletonInputQuery(versionData, PackageTree(persistentMapOf()))
    override val fileHandler: InputQuery<FileId, LanguageHandler<*>?> =
        InputQuery(versionData) { null }
}