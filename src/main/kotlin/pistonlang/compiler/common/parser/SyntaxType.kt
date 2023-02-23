package pistonlang.compiler.common.parser

interface SyntaxType {
    val name: String
    val ignorable: Boolean
    val isNewline: Boolean
    val isEOF: Boolean
}