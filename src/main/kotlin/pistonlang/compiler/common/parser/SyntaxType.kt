package pistonlang.compiler.common.parser

/**
 * The type of a syntax node of a particular grammar
 * It is generally assumed that types implementing this are enums
 * There should be at least one node marked as [isEOF]
 */
interface SyntaxType {
    val name: String
    val ignorable: Boolean
    val isNewline: Boolean
    val isEOF: Boolean
}