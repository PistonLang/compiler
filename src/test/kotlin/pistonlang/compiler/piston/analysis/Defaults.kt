package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.main.*
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing

val defaultHandler = { versionData: QueryVersionData,
                       inputs: FileInputQueries,
                       queries: MainQueries,
                       interners: MainInterners ->
    PistonLanguageHandler(::PistonLexer, PistonParsing::parseFile, versionData, inputs, queries, interners)
}

fun defaultInstance() = CompilerInstance(QueryVersionData())