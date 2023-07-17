package pistonlang.compiler.common.main

import pistonlang.compiler.common.files.FileData
import pistonlang.compiler.common.files.FilePath
import pistonlang.compiler.common.files.PackagePath
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.queries.QueryAccessor
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.util.lists.VoidList

private val voidAccessor = QueryAccessor(VoidList)

class CompilerInstance(
    private val versionData: QueryVersionData,
    handlers: List<(QueryVersionData, FileInputQueries, GeneralQueries, PathInterners) -> LanguageHandler<*>>
) {
    private val extensionHandlers = mutableMapOf<String, LanguageHandler<*>>()
    private val inputs: DefaultInputQueries = DefaultInputQueries(versionData)
    private val pathInterners: DefaultPathInterners = DefaultPathInterners()
    private val queries = GeneralQueries(versionData, inputs)

    init {
        handlers.forEach { fn ->
            val handler = fn(versionData, inputs, queries, pathInterners)
            handler.extensions.forEach { ext ->
                extensionHandlers[ext] = handler
            }
        }
    }

    fun <T> access(fn: context(QueryAccessor) (GeneralQueries) -> T) =
        fn(voidAccessor, queries)

    fun addFile(pack: PackagePath, path: FilePath, code: String) {
        val id = pathInterners.fileIds.getOrAdd(path)
        val packInterner = pathInterners.packIds
        val packageId = packInterner.getOrAdd(pack)

        val newVersion = versionData.update()

        inputs.fileHandler[id] = extensionHandlers[path.path.takeLastWhile { it != '.' }]
        inputs.filePackage[id] = packageId
        inputs.packageTree.value = inputs.packageTree.value.run {
            if (inputs.code.contains(id)) updateFilePath(packageId, newVersion, packInterner)
            else addFile(packageId, id, newVersion, packInterner)
        }
        inputs.code[id] = FileData(true, code)
    }

    fun removeFile(path: FilePath) {
        val id = pathInterners.fileIds[path]

        if (!inputs.code.contains(id)) return

        val current = with(voidAccessor) { inputs.code[id] }
        if (current.valid) {
            val newVersion = versionData.update()
            inputs.packageTree.value.removeFile(
                with(voidAccessor) { inputs.filePackage[id] }!!,
                id,
                newVersion,
                pathInterners.packIds
            )
            inputs.code[id] = current.copy(valid = false)
        }
    }
}