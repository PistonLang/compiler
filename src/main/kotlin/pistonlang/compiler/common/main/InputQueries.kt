package pistonlang.compiler.common.main

import kotlinx.collections.immutable.persistentMapOf
import pistonlang.compiler.common.files.FileData
import pistonlang.compiler.common.files.PackageTree
import pistonlang.compiler.common.files.invalidFileData
import pistonlang.compiler.common.items.FileId
import pistonlang.compiler.common.items.PackageId
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.queries.*

interface FileInputQueries {
    val filePackage: Query<FileId, PackageId?>
    val code: Query<FileId, FileData>
}

interface InputQueries : FileInputQueries {
    val packageTree: SingletonQuery<PackageTree>
    val fileHandler: Query<FileId, LanguageHandler<*>>
}

internal class DefaultInputQueries(versionData: QueryVersionData, defaultHandler: DefaultHandler) : InputQueries {
    override val code: InputQuery<FileId, FileData> =
        InputQuery(versionData) { invalidFileData }
    override val filePackage: InputQuery<FileId, PackageId?> =
        InputQuery(versionData) { null }
    override val packageTree: SingletonInputQuery<PackageTree> =
        SingletonInputQuery(versionData, PackageTree(persistentMapOf()))
    override val fileHandler: InputQuery<FileId, LanguageHandler<*>> =
        InputQuery(versionData) { defaultHandler }
}