import error as main_err

interface Expr {
    interface Visitor<R> {
        fun visitBinaryExpr(expr: Binary) : R
        fun visitUnaryExpr(expr: Unary) : R
        fun visitLiteralExpr(expr: Literal) : R
        fun visitGroupingExpr(expr: Grouping) : R
    }

    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr {
        override fun<R> accept(visitor: Visitor<R>): R {
            return visitor.visitBinaryExpr(this)
        }
    }

    data class Unary(val operator: Token, val right: Expr): Expr {
        override fun<R> accept(visitor: Visitor<R>): R {
            return visitor.visitUnaryExpr(this)
        }
    }

    data class Grouping(val expression: Expr) : Expr {
        override fun<R> accept(visitor: Visitor<R>): R {
            return visitor.visitGroupingExpr(this)
        }
    }

    data class Literal(val value: Any?) : Expr {
        override fun<R> accept(visitor: Visitor<R>): R {
            return visitor.visitLiteralExpr(this)
        }
    }

    fun<R> accept(visitor: Visitor<R>): R
}

/*
    EBNF GRAMMAR

    expression     → equality ;
    equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    term           → factor ( ( "-" | "+" ) factor )* ;
    factor         → unary ( ( "/" | "*" ) unary )* ;
    unary          → ( "!" | "-" ) unary
    | primary ;
    primary        → NUMBER | STRING | "true" | "false" | "nil"
    | "(" expression ")" ;
*/

class Parser(private val tokens: List<Token>) {
    class ParseError : RuntimeException() {}

    private var current: Int = 0

    fun parse(): Expr? {
        try {
            return expression()
        }
        catch (e: ParseError) {
            return null
        }
    }

    private fun expression(): Expr {
        return equality()
    }

    private fun equality(): Expr {
        var expr: Expr = comparison()

        while(match(listOf(TokenType.BANG, TokenType.BANG_EQUAL))) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr: Expr = term()
        while(match(listOf(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL))) {
            val operator: Token = previous()
            val right: Expr = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun term(): Expr {
        var expr: Expr = factor()

        while(match(listOf(TokenType.MINUS, TokenType.PLUS))) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun factor(): Expr {
        var expr: Expr = unary()

        while(match(listOf(TokenType.STAR, TokenType.SLASH))) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if(match(listOf(TokenType.BANG, TokenType.MINUS))) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return literal()
    }

    private fun literal(): Expr {
        if(match(listOf(TokenType.FALSE))) return Expr.Literal(false)
        if(match(listOf(TokenType.TRUE))) return Expr.Literal(true)
        if(match(listOf(TokenType.NIL))) return Expr.Literal(null)

        if(match(listOf(TokenType.NUMBER, TokenType.STRING))) return Expr.Literal(previous().literal)

        if(match(listOf(TokenType.LEFT_PAREN))) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression")
            return Expr.Grouping(expr)
        }

        throw error(peek(), "Expected expression")
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

    private fun match(types: List<TokenType>): Boolean {
        for(type in types) {
            if(check(type)) {
                advance()
                return true
            }
        }

        return false
    }

    private fun error(token: Token, message: String): ParseError {
        main_err(token, message)
        return ParseError()
    }

    private fun consume(type: TokenType, message: String): Token {
        if(check(type)) return advance()

        throw error(peek(), message)
    }

}
