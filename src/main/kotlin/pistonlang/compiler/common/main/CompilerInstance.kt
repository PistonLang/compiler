package pistonlang.compiler.common.main

import pistonlang.compiler.common.files.FileData
import pistonlang.compiler.common.files.FilePath
import pistonlang.compiler.common.files.PackagePath
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.queries.QueryAccessor
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.util.lists.VoidList

@PublishedApi internal val voidAccessor = QueryAccessor(VoidList)

class CompilerInstance(private val versionData: QueryVersionData) {
    private val extensionHandlers = mutableMapOf<String, LanguageHandler<*>>()
    private val inputs: DefaultInputQueries = DefaultInputQueries(versionData)
    private val mainInterners: DefaultInterners = DefaultInterners()
    @PublishedApi internal val queries = MainQueries(versionData, mainInterners, inputs)

    val interners: MainInterners
        get() = mainInterners

    fun <Type : SyntaxType, Handler : LanguageHandler<Type>> addHandler(
        handlerFn: (QueryVersionData, FileInputQueries, MainQueries, MainInterners) -> Handler
    ): Handler {
        val handler = handlerFn(versionData, inputs, queries, mainInterners)
        handler.extensions.forEach { ext ->
            extensionHandlers[ext] = handler
        }
        return handler
    }

    inline fun <T> access(fn: context(QueryAccessor) (MainQueries) -> T) =
        fn(voidAccessor, queries)

    fun addFile(pack: PackagePath, path: FilePath, code: String) {
        val id = mainInterners.fileIds.getOrAdd(path)
        val packInterner = mainInterners.packIds
        val packageId = packInterner.getOrAdd(pack)

        val newVersion = versionData.update()

        with(voidAccessor) {
            inputs.fileHandler[id] = extensionHandlers[path.path.takeLastWhile { it != '.' }]
            inputs.filePackage[id] = packageId
            inputs.packageTree.update(inputs.packageTree.value.run {
                if (inputs.code.contains(id)) updateFilePath(packageId, newVersion, packInterner)
                else addFile(packageId, id, newVersion, packInterner)
            })
            inputs.code[id] = FileData(true, code)
        }
    }

    fun removeFile(path: FilePath) {
        val id = mainInterners.fileIds.get(path) ?: return

        if (!inputs.code.contains(id)) return

        with(voidAccessor) {
            val current = inputs.code[id]

            if (current.valid) {
                val newVersion = versionData.update()
                inputs.packageTree.update(
                    inputs.packageTree.value.removeFile(
                        with(voidAccessor) { inputs.filePackage[id] }!!,
                        id,
                        newVersion,
                        mainInterners.packIds
                    )
                )
                inputs.code[id] = current.copy(valid = false)
            }
        }
    }
}