class Scanner(private val source: String) {
    private val tokens: MutableList<Token> = mutableListOf()
    private var start: Int = 0
    private var current: Int = 0
    private var line: Int = 1

    companion object {
        private val keywords: Map<String, TokenType> = mapOf(
            "and" to TokenType.AND,
            "class" to TokenType.CLASS,
            "else" to TokenType.ELSE,
            "false" to TokenType.FALSE,
            "for" to TokenType.FOR,
            "fun" to TokenType.FUN,
            "if" to TokenType.IF,
            "nil" to TokenType.NIL,
            "or" to TokenType.OR,
            "print" to TokenType.PRINT,
            "return" to TokenType.RETURN,
            "super" to TokenType.SUPER,
            "this" to TokenType.THIS,
            "true" to TokenType.TRUE,
            "var" to TokenType.VAR,
            "while" to TokenType.WHILE
        )
    }

    fun scanTokens(): List<Token> {
        while(!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.EOF, "", null, line))

        return tokens.toList()
    }

    private fun match(expected: Char): Boolean {
        if(isAtEnd() || source[current] != expected) {
            return false
        }

        current++
        return true
    }

    private fun scanToken() {
        val c: Char = advance()

        when(c) {
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)
            '!' -> addToken(if(match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if(match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if(match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if(match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '/' -> {
                if(match('/')) {
                    while(!isAtEnd() && peek() != '\n') {
                        advance()
                    }
                }
                else {
                    addToken(TokenType.SLASH)
                }
            }
            ' ', '\r', '\t' -> {}
            '\n' -> line++
            '"' -> createString()
            in '0'..'9' -> createNumber()
            in 'a'..'z', in 'A'..'Z' -> createIdentifier()
            else -> error(line, "Unexpected character")
        }
    }

    //
    // Helper Functions
    //

    private fun Char.isAlphaNum(): Boolean {
        return this in 'a'..'z' || this in 'A'..'Z' || this == '_' || this in '0'..'9'
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }

    private fun createNumber() {
        while(isDigit(peek())) {
            advance()
        }

        // Checks for decimal
        if (peek() == '.' && isDigit(peekNext())) {
            // Consumes the '.'
            advance()

            while(isDigit(peek())) {
                advance()
            }
        }

        addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
    }

    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun createIdentifier() {
        while (peek().isAlphaNum()) {
            advance()
        }

        val text: String = source.substring(start, current)
        var type: TokenType? = keywords[text]

        if(type == null) {
            type = TokenType.IDENTIFIER
        }

        addToken(type)
    }

    private fun peekNext(): Char {
        if(current + 1 >= source.length) {
            return '\u0000'
        }
        return source[current + 1]
    }

    private fun createString() {
        while(peek() != '"' && !isAtEnd()) {
            if(peek() == '\n') {
                line++
            }
            advance()
        }

        if(isAtEnd()) {
            error(line, "Unexpected character")
            return
        }

        // Captures the closing "
        advance()

        // Removes the " from the ends
        val value: String = source.substring(start + 1, current - 1)
        addToken(TokenType.STRING, value)
    }

    private fun advance(): Char {
        return source[current++]
    }

    private fun peek(): Char {
        if(isAtEnd()) {
            return '\u0000'
        }
        return source[current]
    }

    private fun addToken(type: TokenType) {
        addToken(type, null)
    }

    private fun addToken(type: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }
}