package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.LanguageHandler
import pistonlang.compiler.common.files.FileHandle
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.parser.*
import pistonlang.compiler.common.queries.Query
import pistonlang.compiler.common.queries.toQueryValue
import pistonlang.compiler.piston.parser.PistonSyntaxSets
import pistonlang.compiler.piston.parser.PistonType

/**
 * The standard [LanguageHandler] for Piston code
 */
class PistonLanguageHandler(
    lexer: (String) -> Lexer<PistonType>,
    parsing: (Parser<PistonType>) -> GreenNode<PistonType>,
    instance: CompilerInstance,
) : LanguageHandler<PistonType> {
    override val extensions: List<String> = listOf("pi")

    override val ast: Query<FileHandle, GreenNode<PistonType>> = run {
        val parseFn = { key: FileHandle ->
            val codeString = instance.code[key].value
            val parser = Parser(lexer(codeString), PistonType.file)
            parsing(parser)
        }
        Query(instance.versionData, parseFn) { key, old, version ->
            val codeString = instance.code[key]
            if (codeString.modified < version) old.copy(checked = version)
            else parseFn(key).toQueryValue(version)
        }
    }

    override val fileItems: Query<FileHandle, Map<String, List<NodeLocation<PistonType>>>> = run {
        val collectFn = { key: FileHandle ->
            val ast = ast[key]
            ast.value.childIterator
                .asSequence()
                .filter { it.type in PistonSyntaxSets.defs }
                .groupBy(
                    { it.value.firstDirectChild(PistonType.identifier)?.value?.content ?: "" },
                    { NodeLocation(it.offset..(it.offset + it.value.length), it.type) })
        }
        Query(instance.versionData, collectFn) { handle, old, version ->
            val new = collectFn(handle)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }
}