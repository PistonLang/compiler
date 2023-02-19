package pistonlang.compiler.piston.parser

import pistonlang.compiler.common.parser.SyntaxType

enum class PistonType(
    override val ignorable: Boolean = false,
    override val isNewline: Boolean = false
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

    eof,
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
    superType,

    pathSegment,

    nestedExpression,
    accessExpression,
    callExpression,
    unaryExpression,
    thisExpression,
    superExpression,
    literalExpression,
    identifierExpression,
    term,
    expression,
    timesExpression,
    plusExpression,
    relationExpression,
    equalsExpression,
    andExpression,
    orExpression,
    ternaryExpression,
    assignExpression,

    functionParam,
    functionParams,

    importGroup,
    importPath,
    importValue,
    importSegment,
    import,

    propertyDef,
    classDef,
    traitDef,
    functionDef,

    statement,
    statementBody,
    StatementBlock,
    file,

    typePath,
    typeBound,
    typeParams,
    typeArg,
    typeArgs,
    whereClause,

    nestedType,
    nullableType,
    typeInstance,
    intersectionType,
    superTypes,

    error,
}