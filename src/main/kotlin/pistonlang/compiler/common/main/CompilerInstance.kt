package pistonlang.compiler.common.main

import pistonlang.compiler.common.files.ChangeType
import pistonlang.compiler.common.files.FileChange
import pistonlang.compiler.common.files.FileData
import pistonlang.compiler.common.items.FileHandle
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.language.SyntaxType
import pistonlang.compiler.common.queries.InputQuery
import pistonlang.compiler.common.queries.QueryAccessor
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.util.voidList
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

typealias CodeQuery = InputQuery<FileHandle, FileData>
typealias OptionsQuery = InputQuery<Unit, CompilerOptions>

class CompilerInstance(val versionData: QueryVersionData) {
    private val handlers: MutableMap<String, LanguageHandler<*>> = hashMapOf()
    private val changes: Queue<FileChange> = ConcurrentLinkedQueue()
    private val queries = GeneralQueries(versionData, handlers, changes)

    fun <Type : SyntaxType, Handler : LanguageHandler<Type>> addHandler(handlerFn: (QueryVersionData, GeneralQueries) -> Handler): Handler {
        val handler = handlerFn(versionData, queries)
        handler.extensions.forEach { ext ->
            handlers[ext] = handler
        }
        return handler
    }

    fun <T> access(fn: context(QueryAccessor) (GeneralQueries) -> T) =
        fn(QueryAccessor(hashSetOf(), voidList()), queries)

    fun addFile(ref: FileHandle, code: String) {
        val type = if (queries.code.contains(ref)) ChangeType.Update else ChangeType.Addition
        val newVersion = queries.code.set(ref, FileData(true, code))
        changes.offer(FileChange(ref, type, newVersion))
    }

    fun removeFile(ref: FileHandle) {
        if (!queries.code.contains(ref)) return

        val current = access { it.code[ref] }
        if (current.valid) {
            val newVersion = queries.code.set(ref, current.copy(valid = false))
            changes.offer(FileChange(ref, ChangeType.Removal, newVersion))
        }
    }
}