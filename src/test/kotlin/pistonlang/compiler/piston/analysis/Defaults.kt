package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.main.CompilerInstance
import pistonlang.compiler.common.main.FileInputQueries
import pistonlang.compiler.common.main.GeneralQueries
import pistonlang.compiler.common.main.PathInterners
import pistonlang.compiler.common.queries.QueryVersionData
import pistonlang.compiler.piston.parser.PistonLexer
import pistonlang.compiler.piston.parser.PistonParsing

val defaultHandler = { versionData: QueryVersionData,
                       inputs: FileInputQueries,
                       queries: GeneralQueries,
                       interners: PathInterners ->
    PistonLanguageHandler(::PistonLexer, PistonParsing::parseFile, versionData, inputs, queries, interners)
}

fun defaultInstance() = CompilerInstance(QueryVersionData(), listOf(defaultHandler))