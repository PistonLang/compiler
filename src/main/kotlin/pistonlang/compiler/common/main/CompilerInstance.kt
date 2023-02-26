package pistonlang.compiler.common.main

import pistonlang.compiler.common.files.*
import pistonlang.compiler.common.parser.Parser
import pistonlang.compiler.common.queries.InputQuery
import pistonlang.compiler.common.queries.Query
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.common.queries.toQueryValue
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing
import pistonlang.compiler.piston.parser.PistonType
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

class CompilerInstance(versionData: QueryVersionData) {
    private val codeQuery = InputQuery<FileHandle, String>(versionData) { "" }
    private val options = InputQuery<Unit, CompilerOptions>(versionData) { CompilerOptions("") }
    private val changes: Queue<FileChange> = ConcurrentLinkedQueue()

    private val parseQuery = run {
        val parseFn = { key: FileHandle ->
            val code = codeQuery[key].value
            val lexer = PistonLexer(code)
            val parser = Parser(lexer, PistonType.file)
            PistonParsing.parseFile(parser)
        }
        Query(versionData, parseFn) { key, old, version ->
            val code = codeQuery[key]
            if (code.modified < version) old.copy(checked = version)
            else parseFn(key).toQueryValue(version)
        }
    }

    private val filePackageQuery = run {
        val packFun = { key: FileHandle ->
            val ops = options[Unit]
            val path = key.path.removePrefix(ops.value.startPath).split('/').dropLast(1)
            PackageHandle(path)
        }
        Query(versionData, packFun) { key, old, version ->
            val ops = options[Unit]
            if (ops.modified < version) old.copy(checked = version)
            else packFun(key).toQueryValue(version)
        }
    }

    // TODO: Handle option changes
    private val packageTree = run {
        val default = { _: Unit -> emptyTree }
        Query(versionData, default) { _, old, version ->
            var tree = old.value
            while (changes.isNotEmpty() && changes.peek().version <= version) {
                val change = changes.poll()
                when (change.type) {
                    ChangeType.Addition -> {
                        val pack = filePackageQuery[change.file]
                        tree = tree.add(pack.value, change.file)
                    }
                    ChangeType.Update -> { }
                    ChangeType.Removal -> {
                        val pack = filePackageQuery[change.file]
                        tree = tree.remove(pack.value, change.file)
                    }
                }
            }
            tree.toQueryValue(version)
        }
    }

    fun addFile(handle: FileHandle, code: String) {
        changes.offer(FileChange(handle, ChangeType.Addition, codeQuery.set(handle, code).modified))
    }

    // TODO: Handle incrementally
    fun updateFile(handle: FileHandle, code: String) {
        changes.offer(FileChange(handle, ChangeType.Update, codeQuery.set(handle, code).modified))
    }

    fun removeFile(handle: FileHandle) {
        changes.offer(FileChange(handle, ChangeType.Removal, codeQuery.set(handle, "").modified))
    }
}