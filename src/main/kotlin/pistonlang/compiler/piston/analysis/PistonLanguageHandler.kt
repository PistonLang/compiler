package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.handles.*
import pistonlang.compiler.common.language.LanguageHandler
import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.parser.Lexer
import pistonlang.compiler.common.parser.Parser
import pistonlang.compiler.common.parser.nodes.*
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


    override val fileItems: Query<FileHandle, Map<String, ItemList<PistonType>>> = run {
        val collectFn = { key: FileHandle ->
            val res = mutableMapOf<String, MutableItemList<PistonType>>()

            ast[key].value.childIterator
                .asSequence()
                .filter { child -> child.type in PistonSyntaxSets.defs }
                .forEach { child -> defNodeToReference(child, key, res) }

            res.mapValues { (_, list) -> list.toImmutable() }
        }
        Query(instance.versionData, collectFn) { handle, old, version ->
            val new = collectFn(handle)
            if (new == old.value) old.copy(checked = version) else new.toQueryValue(version)
        }
    }

    private fun defNodeToReference(
        child: GreenChild<PistonType>,
        key: FileHandle,
        res: MutableMap<String, MutableItemList<PistonType>>
    ) {
        val node = child.value
        var name: String = node.firstDirectChild(PistonType.identifier)?.content ?: ""
        val type: ItemType = when (node.type) {
            PistonType.traitDef ->
                ItemType.Trait

            PistonType.classDef ->
                if (node.firstDirectChild(PistonType.functionParams) != null)
                    ItemType.MultiInstanceClass
                else
                    ItemType.SingletonClass

            PistonType.propertyDef ->
                if (node.firstDirectChild(PistonType.valKw) != null)
                    ItemType.Val
                else
                    ItemType.Var

            PistonType.functionDef -> when {
                node.firstDirectChild(PistonType.functionParams) == null ->
                    ItemType.Getter

                name.length > 2 && name.endsWith("_=") -> {
                    name = name.dropLast(2)
                    ItemType.Setter
                }

                else -> ItemType.Function
            }

            else -> error("Something went wrong with the syntax set")
        }

        val loc = child.parentRelativeLocation
        val ref = ItemReference(type, this, loc, key)
        res.getOrPut(name) { MutableItemList() }.add(ref)
    }
}