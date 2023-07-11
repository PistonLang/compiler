package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.main.GeneralQueries
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing

fun defaultInstance() = CompilerInstance(QueryVersionData())
val defaultHandler = { versionData: QueryVersionData, queries: GeneralQueries ->
    PistonLanguageHandler(::PistonLexer, PistonParsing::parseFile, versionData, queries)
}