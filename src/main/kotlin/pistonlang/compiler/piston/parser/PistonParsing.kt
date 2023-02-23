package pistonlang.compiler.piston.parser

import pistonlang.compiler.common.parser.GreenNode
import pistonlang.compiler.common.parser.Parser

private typealias P = Parser<PistonType>
private typealias Ty = PistonType
private typealias ClosedDelim = Boolean

enum class Precedence(val type: Ty) {
    None(Ty.eof),
    Or(Ty.orExpression),
    And(Ty.andExpression),
    Equals(Ty.equalsExpression),
    Relation(Ty.relationExpression),
    Plus(Ty.plusExpression),
    Times(Ty.timesExpression),
}

object PistonParsing {
    private fun P.consume(type: Ty) = at(type).also { if (it) push() }

    private fun P.expect(type: Ty) = consume(type) || makeError()

    private fun P.isRecoveryType() = when (currType) {
        Ty.valKw, Ty.varKw, Ty.eof, Ty.rBrace, Ty.classKw, Ty.traitKw, Ty.defKw -> true
        else -> false
    }

    private fun P.makeError(): Boolean {
        if (isRecoveryType()) return false
        pushToNode(Ty.error)
        return true
    }

    private fun P.handleNullableType() {
        while (at(Ty.qMark)) nestLast(Ty.nullableType) {
            push()      // qMark
        }
    }

    private tailrec fun P.parsePathTypeTail() {
        if (!at(Ty.dot)) return

        push()      // dot
        expectPathSegment()
        parsePathTypeTail()
    }

    private fun P.handleTypePath() = createNode(Ty.typePath) {
        handlePathSegment()
        parsePathTypeTail()
    }

    private fun P.handleNestedType() = createNode(Ty.nestedType) {
        push()  // lParen
        expectTypeInstance()
        expect(Ty.rParen)
    }

    private fun P.expectTypeInstance(): Boolean {
        when (currType) {
            Ty.identifier -> handleTypePath()
            Ty.lParen -> handleNestedType()
            else -> return makeError()
        }
        handleNullableType()
        return true
    }

    private fun P.expectTypeBound() = createNode(Ty.typeBound) {
        if (!expect(Ty.identifier)) return@createNode

        expect(Ty.subtype) && expectTypeInstance()
    }

    private fun P.expectTypeArg() = createNode(Ty.typeArg) {
        when (currType) {
            Ty.subtype, Ty.supertype -> push()
            else -> {}
        }

        expectTypeInstance()
    }

    private fun P.handleTypeBoundsList(): ClosedDelim =
        handleList(Ty.rBracket) { expectTypeBound() }

    private fun P.handleTypeArgsList(): ClosedDelim =
        handleList(Ty.rBracket) { expectTypeArg() }

    private inline fun P.handleList(endDelim: Ty, crossinline fn: () -> Boolean): ClosedDelim {
        while (true) {
            if (at(endDelim)) return true

            if (!fn()) return false

            when (currType) {
                endDelim -> return true
                Ty.comma -> push()
                else -> {}
            }
        }
    }

    private fun P.handleTypeGuard(): Boolean {
        var res = true
        createNode(Ty.typeGuard) {
            push()      // whereKw
            res = handleTypeBoundsList()
        }
        return res
    }

    private tailrec fun P.handleTypeParamsList(): ClosedDelim {
        when (currType) {
            Ty.rBracket -> return true
            Ty.whereKw -> return handleTypeGuard()
            else -> {}
        }

        if (!expect(Ty.identifier)) return false

        when (currType) {
            Ty.rBracket -> return true
            Ty.whereKw -> return handleTypeGuard()
            Ty.comma -> push()
            else -> {}
        }

        return handleTypeParamsList()
    }

    private fun P.parseTypeParams() {
        if (!at(Ty.lBracket)) return

        createNode(Ty.typeParams) {
            push()      // lBracket
            if (handleTypeParamsList())
                push()  // rBracket
        }
    }

    private fun P.parseTypeArgs() {
        if (!at(Ty.lBracket)) return

        createNode(Ty.typeArgs) {
            push()      // lBracket
            if (handleTypeArgsList())
                push()  // rBracket
        }
    }

    private tailrec fun P.handleIntersectingTypes() {
        if (!at(Ty.and)) return

        push()      // and
        expectTypeInstance()

        handleIntersectingTypes()
    }

    private fun P.expectIntersectionType() = createNode(Ty.intersectionType) {
        if (!expectTypeInstance()) return@createNode
        handleIntersectingTypes()
    }

    private fun P.parseSupertypes() {
        if (!at(Ty.supertype)) return

        createNode(Ty.supertypes) {
            push()  // supertype
            expectIntersectionType()
        }
    }

    private fun P.handlePathSegment() = createNode(Ty.pathSegment) {
        push()  // identifier
        parseTypeArgs()
    }

    private fun P.parseStatement() = when (currType) {
        Ty.classKw -> handleClassDef()
        Ty.traitKw -> handleTraitDef()
        Ty.valKw, Ty.varKw -> handlePropertyDef()
        Ty.defKw -> handleFunctionDef()
        else -> false
    }

    private fun P.parseTypeAnnotation() = createNode(Ty.typeAnnotation) {
        consume(Ty.colon).also { if (it) expectTypeInstance() }
    }

    private fun P.expectParam() = createNode(Ty.functionParam) {
        if (!consume(Ty.identifier)) {
            makeError()
            return@createNode
        }

        parseTypeAnnotation()
    }

    private fun P.handleParamsList(): ClosedDelim =
        handleList(Ty.rParen) { expectParam() }

    private fun P.parseParams() {
        if (!at(Ty.lParen)) return

        createNode(Ty.functionParams) {
            push()      // lParen
            if (handleParamsList())
                push()  // rParen
        }
    }

    private fun P.expectStatement() = parseStatement() || makeError()

    private fun P.handleDeclarationStatements(): ClosedDelim =
        handleList(Ty.rBrace) { expectStatement() }

    private fun P.parseStatementBody() {
        if (!at(Ty.lBrace)) return

        createNode(Ty.statementBlock) {
            push()      // lBrace
            if (handleDeclarationStatements())
                push()  // rBrace
        }
    }

    private fun P.handleIdentifierExpression() = createNode(Ty.identifierExpression) {
        handlePathSegment()
    }

    private fun P.handleNestedExpression() = createNode(Ty.nestedExpression) {
        push()      // lParen
        expectExpression()
        expect(Ty.rParen)
    }

    private fun P.handleUnaryExpression() = createNode(Ty.unaryExpression) {
        push()      // plus/minus
        expectTerm()
    }

    private fun P.expectTerm() = parseTerm() || makeError()

    private fun P.handleAccessExpression() = nestLast(Ty.accessExpression) {
        push()      // dot
        expectPathSegment()
    }

    private fun P.handleArgsList(): ClosedDelim =
        handleList(Ty.rParen) { expectExpression() }

    private fun P.handleCallExpression() = nestLast(Ty.callExpression) {
        push()      // lParen
        if (handleArgsList())
            push()  // rParen
    }

    private fun P.expectPathSegment(): Boolean =
        if (at(Ty.identifier)) {
            handlePathSegment()
            true
        } else {
            makeError()
        }

    private tailrec fun P.handlePostfix() {
        when (currType) {
            Ty.dot -> handleAccessExpression()
            Ty.lParen ->
                if (startWithNewline) return
                else handleCallExpression()

            else -> return
        }

        handlePostfix()
    }

    private fun P.parseTerm(): Boolean {
        when (currType) {
            Ty.identifier -> handleIdentifierExpression()
            Ty.lParen -> handleNestedExpression()
            Ty.superKw -> pushToNode(Ty.superExpression)
            Ty.thisKw -> pushToNode(Ty.thisExpression)
            Ty.intLiteral, Ty.floatLiteral,
            Ty.trueKw, Ty.falseKw,
            Ty.charLiteral, Ty.stringLiteral -> pushToNode(Ty.literalExpression)

            Ty.plus, Ty.minus -> handleUnaryExpression()
            else -> return false
        }

        handlePostfix()

        return true
    }

    private fun P.currPrecedence() = when (currType) {
        Ty.star, Ty.slash -> Precedence.Times
        Ty.plus, Ty.minus -> Precedence.Plus
        Ty.less, Ty.lessEq, Ty.greater, Ty.greaterEq -> Precedence.Relation
        Ty.eqEq, Ty.eMarkEq -> Precedence.Equals
        Ty.andAnd -> Precedence.And
        Ty.orOr -> Precedence.Or
        else -> Precedence.None
    }

    private fun P.handleBinaryExpression(pre: Precedence): Unit = nestLast(pre.type) {
        do {
            push()      // operator
            expectTerm()
            if (currPrecedence() > pre)
                handleBinaryExpression(currPrecedence())
        } while (currPrecedence() == pre)
    }

    private fun P.handleAssignment() = nestLast(Ty.assignExpression) {
        push()      // eq
        expectExpression()
    }

    private fun P.handleTernary() = nestLast(Ty.ternaryExpression) {
        push()      // qMark
        expectExpression()
        if (consume(Ty.colon)) {
            expectExpression()
        } else {
            makeError()
        }
    }

    private fun P.handleBinaryExpression() {
        when (currType) {
            Ty.eq -> handleAssignment()
            Ty.qMark -> handleTernary()
            else -> while (currPrecedence() != Precedence.None)
                handleBinaryExpression(currPrecedence())
        }
    }

    private fun P.expectExpression(): Boolean =
        if (parseTerm()) {
            handleBinaryExpression()
            true
        } else makeError()

    private fun P.parseExpressionBody() {
        if (!at(Ty.eq)) return

        createNode(Ty.expressionBody) {
            push()      // eq
            expectExpression()
        }
    }

    private fun P.handleClassDef() = createNode(Ty.classDef) {
        push()      // classKw
        if (!expect(Ty.identifier)) return@createNode
        parseTypeParams()
        parseParams()
        parseSupertypes()
        parseStatementBody()
    }

    private fun P.handlePropertyDef() = createNode(Ty.propertyDef) {
        push()      // valKw/varKw
        if (!expect(Ty.identifier)) return@createNode
        parseTypeAnnotation()
        parseExpressionBody()
    }

    private fun P.handleFunctionDef() = createNode(Ty.functionDef) {
        push()      // defKw
        if (!expect(Ty.identifier)) return@createNode
        parseTypeParams()
        parseParams()
        parseTypeAnnotation()
        parseExpressionBody()
    }

    private fun P.handleTraitDef() = createNode(Ty.traitDef) {
        push()      // traitKw
        if (!expect(Ty.identifier)) return@createNode
        parseTypeParams()
        parseSupertypes()
        parseStatementBody()
    }

    private tailrec fun P.handleImportPathTail() {
        if (consume(Ty.dot)) expect(Ty.identifier)
        else return

        handleImportPathTail()
    }

    private fun P.handleImportPath() = createNode(Ty.importPath) {
        push()      // identifier
        handleImportPathTail()
    }

    private fun P.expectImportSegment(): Boolean =
        if (at(Ty.identifier)) createNode(Ty.importSegment) {
            handleImportPath()
            if (consume(Ty.colon))
                expectImportGroup()
        } else makeError()

    private fun P.handleImportSegment(): ClosedDelim =
        handleList(Ty.rBrace) { expectImportSegment() }

    private fun P.expectImportGroup() = createNode(Ty.importGroup) {
        if (!consume(Ty.lBrace)) {
            makeError()
            return@createNode
        }

        if (handleImportSegment())
            push()  // rBrace
    }

    private fun P.parseImport() {
        if (!at(Ty.importKw)) return

        createNode(Ty.import) {
            push()  // importKw
            expectImportGroup()
        }
    }

    private fun P.handleFile() {
        parseImport()
        while (currType != Ty.eof) {
            if (parseStatement()) consume(Ty.comma)
            else pushToNode(Ty.error)
        }
    }

    fun parseFile(parser: P): GreenNode<PistonType> {
        parser.handleFile()
        return parser.finish()
    }
}