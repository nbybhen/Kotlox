class Scanner(val source: String = "") {
    var tokens: MutableList<Token> = mutableListOf();
    private var start: Int = 0;
    private var current: Int = 0;
    private var line: Int = 1;
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


    fun scanTokens(): MutableList<Token> {
        while(!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(Token(TokenType.EOF, "", null, line));

        return tokens;
    }

    private fun match(expected: Char): Boolean {
        if(isAtEnd()) {
            return false;
        }

        if(source[current] != expected) {
            return false;
        }

        current++;
        return true;
    }

    private fun scanToken() {
        val c: Char = advance();

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
                        advance();
                    }
                }
                else {
                    addToken(TokenType.SLASH);
                }
            }
            ' ', '\r', '\t' -> {}
            '\n' -> line++
            '"' -> string()
            else -> {
                if(isDigit(c)) {
                    number();
                }
                else if(c.isLetter()) {
                    identifier();
                }
                else {
                    // TODO: Setup Lox error
                }
            }
        }
    }

    //
    // Helper Functions
    //
    private fun isAtEnd(): Boolean {
        return current >= source.length
    }

    private fun number() {
        while(isDigit(peek())) {
            advance();
        }

        // Checks for decimal
        if (peek() == '.' && isDigit(peekNext())) {
            // Consumes the '.'
            advance();

            while(isDigit(peek())) {
                advance();
            }
        }

        addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
    }

    private fun isDigit(c: Char): Boolean {
        return c >= '0' && c <= '9';
    }

    private fun identifier() {
        while (peek().isLetterOrDigit()) {
            advance();
        }

        val text: String = source.substring(start, current);
        var type: TokenType? = keywords[text];

        if(type == null) {
            type = TokenType.IDENTIFIER;
        }

        addToken(type);
    }

    private fun peekNext(): Char {
        if(current + 1 >= source.length) {
            return '\u0000';
        }
        return source[current + 1];
    }

    private fun string() {
        while(peek() != '"' && !isAtEnd()) {
            if(peek() == '\n') {
                line++;
            }
            advance();
        }

        if(isAtEnd()) {
            // TODO: "Unterminated string" Lox error
            return;
        }

        // Captures the closing "
        advance();

        // Removes the " from the ends
        val value: String = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private fun advance(): Char {
        return source[current++];
    }

    private fun peek(): Char {
        if(isAtEnd()) {
            return '\u0000';
        }
        return source[current];
    }

    private fun addToken(type: TokenType) {
        addToken(type, null);
    }

    private fun addToken(type: TokenType, literal: Any?) {
        var text = source.substring(start, current);
        tokens.add(Token(type, text, literal, line));
    }
}