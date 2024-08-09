import error as mainErr

sealed interface Expr {
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
    class ParseError : RuntimeException()

    private var current: Int = 0

    fun parse(): Expr? {
        return try {
            expression()
        } catch (e: ParseError) {
            null
        }
    }

    private fun expression(): Expr {
        return equality()
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

        throw error(peek(), message)
    }

}
