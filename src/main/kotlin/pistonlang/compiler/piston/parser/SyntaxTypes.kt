package pistonlang.compiler.piston.parser

import pistonlang.compiler.common.parser.SyntaxType
import pistonlang.compiler.common.parser.syntaxSet

enum class PistonType(
    override val ignorable: Boolean = false,
    override val isNewline: Boolean = false,
    override val isEOF: Boolean = false
) : SyntaxType {
    intLiteral,
    floatLiteral,
    charLiteral,
    stringLiteral,

    thisKw,
    superKw,
    nullKw,
    whereKw,
    importKw,
    valKw,
    varKw,
    defKw,
    classKw,
    traitKw,
    trueKw,
    falseKw,

    identifier,

    comment(ignorable = true),
    whitespace(ignorable = true),
    newline(ignorable = true, isNewline = true),

    eof(isEOF = true),
    unknown,

    eq,
    eqEq,
    eMarkEq,
    less,
    greater,
    lessEq,
    greaterEq,
    andAnd,
    orOr,
    plus,
    minus,
    star,
    slash,

    dot,
    comma,
    qMark,
    colon,
    lParen,
    rParen,
    lBracket,
    rBracket,
    lBrace,
    rBrace,

    and,
    subtype,
    supertype,

    pathSegment,

    nestedExpression,
    accessExpression,
    callExpression,
    unaryExpression,
    thisExpression,
    superExpression,
    literalExpression,
    identifierExpression,
    timesExpression,
    plusExpression,
    relationExpression,
    equalsExpression,
    andExpression,
    orExpression,
    ternaryExpression,
    assignExpression,

    typeAnnotation,
    functionParam,
    functionParams,
    expressionBody,

    importGroup,
    importPath,
    importSegment,
    import,

    propertyDef,
    classDef,
    traitDef,
    functionDef,

    statementBlock,
    file,

    typePath,
    typeBound,
    typeParams,
    typeArg,
    typeArgs,
    typeGuard,

    nestedType,
    nullableType,
    intersectionType,
    supertypes,

    error;
}

object PistonSyntaxSets {
    val defs = syntaxSet(
        PistonType.classDef, PistonType.traitDef, PistonType.functionDef, PistonType.propertyDef,
    )
}