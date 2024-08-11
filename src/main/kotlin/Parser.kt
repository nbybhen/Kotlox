import error as mainErr

/*
    EBNF GRAMMAR


    program        → statement* EOF ;
    statement      → exprStmt | printStmt | block ;
    block          → "{" declaration* "}" ;
    declaration    → varDecl | statement ;
    varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
    exprStmt       → expression ";" ;
    printStmt      → "print" expression ";" ;

    expression     → assignment ;
    assignment     → IDENTIFIER "=" assignment | equality ;
    equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    term           → factor ( ( "-" | "+" ) factor )* ;
    factor         → unary ( ( "/" | "*" ) unary )* ;
    unary          → ( "!" | "-" ) unary
    | primary ;
    primary        → NUMBER | STRING | "true" | "false" | "nil"
    | "(" expression ")" | IDENTIFIER  ;
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
        try {
            return if (match(TokenType.VAR)) return varDeclaration() else statement()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun varDeclaration(): Stmt {
        val name: Token = consume(TokenType.IDENTIFIER, "Expect variable name")

        var initializer: Expr? = null
        if (match(TokenType.EQUAL)) initializer = expression()

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {
        return when {
            match(TokenType.PRINT) -> printStatement()
            match(TokenType.LEFT_BRACE) -> Stmt.Block(block())
            else -> expressionStatement()
        }
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
        val expr: Expr = equality()

        if (match(TokenType.EQUAL)) {
            val equals: Token = previous()
            val value = assignment()

            if (expr is Expr.Variable) return Expr.Assign(expr.name, value)

            error(equals, "Invalid assignment target.")
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

        return literal()
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
