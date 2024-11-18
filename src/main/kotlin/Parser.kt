import error as mainErr


/*
    EBNF GRAMMAR


    program        → statement* EOF ;

    statement      → ifStmt | exprStmt | printStmt | block | whileStmt | forStmt | returnStmt ;
    returnStmt     → "return" expression? ";" ;
    whileStmt      → "while" "(" expression ")" statement ;
    forStmt        → "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
    ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
    exprStmt       → expression ";" ;
    printStmt      → "print" expression ";" ;
    block          → "{" declaration* "}" ;

    declaration    → classDecl | funDecl | varDecl | statement ;
    classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )? "{" function* "}" ;
    parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
    funDecl        → "fun" function ;
    function       → IDENTIFIER "(" parameters? ")" block ;
    varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;

    arguments      → expression ( "," expression )* ;
    expression     → assignment ;
    assignment     → ( call "." )? IDENTIFIER "=" assignment | logic_or ;
    logic_or       → logic_and ( "or" logic_and )* ;
    logic_and      → equality ( "and" equality )* ;
    equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    term           → factor ( ( "-" | "+" ) factor )* ;
    factor         → unary ( ( "/" | "*" ) unary )* ;
    unary          → ( "!" | "-" ) unary | call ;
    call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
    literal        → "true" | "false" | "nil" | "this"
               | NUMBER | STRING | IDENTIFIER | "(" expression ")"
               | "super" "." IDENTIFIER ;
*/

class Parser(private val tokens: List<Token>) {
    class ParseError : RuntimeException()

    private var current: Int = 0

    fun parse(): List<Stmt?> {
        val stmts = mutableListOf<Stmt?>()
        while(!isAtEnd()) {
            stmts.add(declaration())
        }
        return stmts
    }

    private fun declaration(): Stmt? {
        return try {
            when {
                match(TokenType.VAR) -> varDeclaration()
                match(TokenType.CLASS) -> classDeclaration()
                match(TokenType.FUN) -> function("function")
                else -> statement()
            }
        } catch (error: ParseError) {
            synchronize()
            null
        }
    }

    private fun classDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expected class name.")
        var superclass: Expr.Variable? = null

        if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Expect superclass name.")
            superclass = Expr.Variable(previous())
        }

        consume(TokenType.LEFT_BRACE, "Expected '{' before class body.")

        val methods: MutableList<Stmt.Function> = mutableListOf()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' after class body.")

        return Stmt.Class(name, superclass, methods)
    }

    private fun function(kind: String): Stmt.Function {
        val name: Token = consume(TokenType.IDENTIFIER, "Expected $kind name.")
        consume(TokenType.LEFT_PAREN, "Expected '(' after $kind name.")

        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }

                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")

        consume(TokenType.LEFT_BRACE, "Expected '{' before $kind body.")
        val body = block()

        return Stmt.Function(name, parameters, body)
    }

    private fun varDeclaration(): Stmt.Var {
        val name: Token = consume(TokenType.IDENTIFIER, "Expect variable name")

        var initializer: Expr? = null
        if (match(TokenType.EQUAL)) initializer = expression()

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {
        return when {
            match(TokenType.IF) -> ifStatement()
            match(TokenType.PRINT) -> printStatement()
            match(TokenType.LEFT_BRACE) -> Stmt.Block(block())
            match(TokenType.WHILE) -> whileStatement()
            match(TokenType.FOR) -> forStatement()
            match(TokenType.RETURN) -> returnStatement()
            else -> expressionStatement()
        }
    }

    private fun returnStatement(): Stmt.Return {
        val keyword = previous()
        val value: Expr? = if (!check(TokenType.SEMICOLON)) expression() else null

        consume(TokenType.SEMICOLON, "Expected ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'for'.")

        val initializer: Stmt?
        if (match(TokenType.SEMICOLON)) {
            initializer = null
        }
        else if(match(TokenType.VAR)) {
            initializer = varDeclaration()
        }
        else {
            initializer = expressionStatement()
        }

        var condition: Expr? = if (!check(TokenType.SEMICOLON)) expression() else null
        consume(TokenType.SEMICOLON, "Expect ';' after condition.")

        val increment: Expr? = if(!check(TokenType.RIGHT_PAREN)) expression() else null
        consume(TokenType.RIGHT_PAREN, "Expected ')' after for clauses.")

        var body: Stmt = statement()

        if (increment != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }

        if (condition == null) condition = Expr.Literal(true)
        body = Stmt.While(condition, body)

        if (initializer != null) body = Stmt.Block(listOf(initializer, body))

        return body
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'")
        val condition: Expr = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")
        val body: Stmt = statement()

        return Stmt.While(condition, body)
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after if.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")

        val thenBranch = statement()
        var elseBranch: Stmt? = null
        if (match(TokenType.ELSE)) elseBranch = statement()

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun expressionStatement(): Stmt {
        val expr: Expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun block(): List<Stmt?> {
        val stmts = mutableListOf<Stmt?>()

        while (!check(TokenType.RIGHT_BRACE)) {
            stmts.add(declaration())
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        return stmts
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr: Expr = or()

        if (match(TokenType.EQUAL)) {
            val equals: Token = previous()
            val value = assignment()

            if (expr is Expr.Variable) return Expr.Assign(expr.name, value)
            if (expr is Expr.Get) return Expr.Set(expr.obj, expr.name, value)

            error(equals, "Invalid assignment target.")
        }

        return expr
    }

    private fun or(): Expr {
        var expr: Expr = and()

        while (match(TokenType.OR)) {
            val or = previous()
            val right: Expr = and()
            expr = Expr.Logical(expr, or, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr: Expr = equality()

        while (match(TokenType.AND)) {
            val and = previous()
            val right: Expr = equality()
            expr = Expr.Logical(expr, and, right)
        }

        return expr
    }

    private fun equality(): Expr {
        var expr: Expr = comparison()

        while(match(TokenType.BANG, TokenType.BANG_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr: Expr = term()
        while(match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator: Token = previous()
            val right: Expr = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun term(): Expr {
        var expr: Expr = factor()

        while(match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun factor(): Expr {
        var expr: Expr = unary()

        while(match(TokenType.STAR, TokenType.SLASH)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if(match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return call()
    }

    private fun call(): Expr {
        var expr = literal()

        while (true) {
            when {
                match(TokenType.LEFT_PAREN) -> expr = finishCall(expr)
                match(TokenType.DOT) -> {
                    val name = consume(TokenType.IDENTIFIER, "Expected property name after '.'")
                    expr = Expr.Get(expr, name)
                }
                else -> break
            }
        }

        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val args = mutableListOf<Expr>()

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (args.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments.")
                }
                args.add(expression())
            } while (match(TokenType.COMMA))
        }

        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")

        return Expr.Call(callee, paren, args)
    }

    private fun literal(): Expr {
        return when {
            match(TokenType.TRUE) -> Expr.Literal(true)
            match(TokenType.FALSE) -> Expr.Literal(false)
            match(TokenType.NIL) -> Expr.Literal(null)
            match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
            match(TokenType.LEFT_PAREN) -> {
                val expr = expression()
                consume(TokenType.RIGHT_PAREN, "Expected ')' after expression")
                Expr.Grouping(expr)
            }
            match(TokenType.IDENTIFIER) -> Expr.Variable(previous())
            match(TokenType.THIS) -> Expr.This(previous())
            match(TokenType.SUPER) -> {
                val keyword = previous()
                consume(TokenType.DOT, "Expected '.' after 'super'.")
                val method = consume(TokenType.IDENTIFIER, "Expect superclass method name.")
                Expr.Super(keyword, method)
            }
            else -> throw error(peek(), "Expected expression")
        }
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if(previous().type == TokenType.SEMICOLON) return

            when(peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF,
                TokenType.WHILE, TokenType.PRINT ,TokenType.RETURN -> return
                else -> advance()
            }
        }
    }

    //
    // Helper Functions
    //

    private fun peek(): Token {
        return tokens[current]
    }

    // Checks if the next token to be processed is of the specified type
    private fun check(type: TokenType): Boolean {
        return if (isAtEnd()) false else peek().type == type
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }

    private fun previous(): Token {
        return tokens[current-1]
    }

    private fun advance(): Token {
        if(!isAtEnd()) {
            current++
        }
        return previous()
    }

    private fun match(vararg types: TokenType): Boolean {
        for(type in types) {
            if(check(type)) {
                advance()
                return true
            }
        }

        return false
    }

    private fun error(token: Token, message: String): ParseError {
        mainErr(token, message)
        return ParseError()
    }

    private fun consume(type: TokenType, message: String): Token {
        if(check(type)) return advance()

        throw error(previous(), message)
    }

}
