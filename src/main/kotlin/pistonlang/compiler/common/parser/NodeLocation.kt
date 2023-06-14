package pistonlang.compiler.common.parser

data class NodeLocation<Type: SyntaxType>(val pos: IntRange, val type: Type)

typealias RelativeNodeLoc<Type> = NodeLocation<Type>

typealias AbsoluteNodeLoc<Type> = NodeLocation<Type>