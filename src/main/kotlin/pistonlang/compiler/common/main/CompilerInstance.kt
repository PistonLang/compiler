package pistonlang.compiler.common.main

import pistonlang.compiler.common.files.FileData
import pistonlang.compiler.common.files.FilePath
import pistonlang.compiler.common.files.PackagePath
import pistonlang.compiler.common.items.FileId
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.queries.QueryAccessor
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.util.lists.VoidList

@PublishedApi
internal val voidAccessor = QueryAccessor(VoidList)

class CompilerInstance(private val versionData: QueryVersionData) {
    private val extensionHandlers = mutableMapOf<String, LanguageHandler<*>>()
    private val defaultHandler = DefaultHandler()
    private val inputs: DefaultInputQueries = DefaultInputQueries(versionData, defaultHandler)
    private val mainInterners: DefaultInterners = DefaultInterners()
    private val unhandled = mutableMapOf<String, MutableList<FileId>>()

    @PublishedApi
    internal val queries: MainQueries = DefaultMainQueries(versionData, mainInterners, inputs)

    val interners: MainInterners
        get() = mainInterners

    fun <Type : SyntaxType, Handler : LanguageHandler<Type>> addHandler(
        handlerFn: (QueryVersionData, FileInputQueries, MainQueries, MainInterners) -> Handler
    ): Handler {
        val handler = handlerFn(versionData, inputs, queries, mainInterners)
        val ext = handler.extension

        if (extensionHandlers.contains(ext))
            error("Multiple handlers with the same extension: .$ext")

        val filesTohHandle = unhandled[ext]

        if (filesTohHandle != null) {
            versionData.update()
            filesTohHandle.forEach { inputs.fileHandler[it] = handler }
        }

        extensionHandlers[ext] = handler
        return handler
    }

    inline fun <T> access(fn: context(QueryAccessor) (MainQueries) -> T) =
        fn(voidAccessor, queries)

    fun addFile(pack: PackagePath, path: FilePath, code: String) {
        val id = mainInterners.fileIds.getOrAdd(path)
        val packInterner = mainInterners.packIds
        val packageId = packInterner.getOrAdd(pack)

        val newVersion = versionData.update()
        val extension = path.path.takeLastWhile { it != '.' }
        val handler = extensionHandlers[extension] ?: run {
            unhandled.getOrPut(extension) { mutableListOf() }.add(id)
            defaultHandler
        }

        with(voidAccessor) {
            inputs.fileHandler[id] = handler
            inputs.filePackage[id] = packageId
            inputs.packageTree.update(inputs.packageTree.value.run {
                if (inputs.code.contains(id)) updateFilePath(packageId, newVersion, packInterner)
                else addFile(packageId, id, newVersion, packInterner)
            })
            inputs.code[id] = FileData(true, code)
        }
    }

    fun removeFile(path: FilePath) {
        val id = mainInterners.fileIds[path] ?: return

        with(voidAccessor) {
            val current = inputs.code[id]
            if (!current.valid) return

            val newVersion = versionData.update()
            inputs.packageTree.update(
                inputs.packageTree.value.removeFile(
                    with(voidAccessor) { inputs.filePackage[id] }!!,
                    id,
                    newVersion,
                    mainInterners.packIds
                )
            )
            inputs.filePackage[id] = null
            inputs.code[id] = current.copy(valid = false)
        }
    }
}