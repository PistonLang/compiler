package pistonlang.compiler.common.main

import pistonlang.compiler.common.files.FileData
import pistonlang.compiler.common.items.FileHandle
import pistonlang.compiler.common.items.PackageHandle
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.queries.QueryAccessor
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.util.VoidList

private val voidAccessor = QueryAccessor(VoidList)

class CompilerInstance(private val versionData: QueryVersionData) {
    private val inputs: DefaultInputQueries = DefaultInputQueries(versionData)
    private val queries = GeneralQueries(versionData, inputs)

    fun <Type : SyntaxType, Handler : LanguageHandler<Type>> addHandler(
        handlerFn: (QueryVersionData, FileInputQueries, GeneralQueries) -> Handler
    ): Handler {
        val handler = handlerFn(versionData, inputs, queries)
        handler.extensions.forEach { ext ->
            inputs.postfixHandler[ext] = handler
        }
        return handler
    }

    fun <T> access(fn: context(QueryAccessor) (GeneralQueries) -> T) =
        fn(voidAccessor, queries)

    fun addFile(pack: PackageHandle, handle: FileHandle, code: String) {
        inputs.filePackage[handle] = pack
        val newVersion = versionData.update()
        inputs.packageTree.value = inputs.packageTree.value.run {
            if (inputs.code.contains(handle)) updateFilePath(pack, newVersion)
            else addFile(pack, handle, newVersion)
        }
        inputs.code[handle] = FileData(true, code)
    }

    fun removeFile(handle: FileHandle) {
        if (!inputs.code.contains(handle)) return

        val current = with(voidAccessor) { inputs.code[handle] }
        if (current.valid) {
            val newVersion = versionData.update()
            inputs.packageTree.value.removeFile(with(voidAccessor) { inputs.filePackage[handle] }!!, handle, newVersion)
            inputs.code[handle] = current.copy(valid = false)
        }
    }
}