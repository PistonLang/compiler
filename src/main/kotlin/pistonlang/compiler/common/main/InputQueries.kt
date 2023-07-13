package pistonlang.compiler.common.main

import kotlinx.collections.immutable.persistentMapOf
import pistonlang.compiler.common.files.FileData
import pistonlang.compiler.common.files.PackageTree
import pistonlang.compiler.common.files.invalidFileData
import pistonlang.compiler.common.items.FileHandle
import pistonlang.compiler.common.items.PackageHandle
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.queries.InputQuery
import pistonlang.compiler.common.queries.Query
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.common.queries.SingletonInputQuery

interface FileInputQueries {
    val filePackage: Query<FileHandle, PackageHandle?>
    val code: Query<FileHandle, FileData>
}

interface InputQueries : FileInputQueries {
    val postfixHandler: Query<String, LanguageHandler<*>?>
    val packageTree: Query<Unit, PackageTree>
}

internal class DefaultInputQueries(versionData: QueryVersionData) : InputQueries {
    override val code: InputQuery<FileHandle, FileData> =
        InputQuery(versionData) { invalidFileData }
    override val filePackage: InputQuery<FileHandle, PackageHandle?> =
        InputQuery(versionData) { null }
    override val postfixHandler: InputQuery<String, LanguageHandler<*>?> =
        InputQuery(versionData) { null }
    override val packageTree: SingletonInputQuery<PackageTree> =
        SingletonInputQuery(versionData, PackageTree(persistentMapOf()))
}