package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing

fun defaultInstance() = CompilerInstance(QueryVersionData())
fun defaultHandler(instance: CompilerInstance) =
    PistonLanguageHandler(::PistonLexer, PistonParsing::parseFile, instance)