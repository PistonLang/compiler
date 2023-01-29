package pistonlang.compiler.palm.parser

import pistonlang.compiler.common.parser.SyntaxType

enum class PalmType(
    override val ignorable: Boolean = false,
    override val trailing: Boolean = true,
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
    getKw,
    setKw,
    valKw,
    varKw,
    defKw,
    classKw,
    traitKw,
    trueKw,
    falseKw,

    identifier,

    comment(ignorable = true, trailing = true),
    whitespace(ignorable = true, trailing = false),
    newline(ignorable = true, trailing = false, isNewline = true),

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

    arrow,
    and,
    subtype,
    superType,

    ident,
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
    getterDef,
    setterDef,
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
    superTypes;
}