enum class TokenType {
    // SC Tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,
    BANG, EQUAL, GREATER, LESS,

    // DC Tokens
    GREATER_EQUAL, EQUAL_EQUAL, BANG_EQUAL, LESS_EQUAL,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE, EOF
}

data class Token(val type: TokenType, val lexeme: String, val literal: Any?, val line: Int)