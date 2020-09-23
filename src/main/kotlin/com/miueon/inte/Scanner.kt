package com.miueon.inte



class Scanner(
    private val source: String
) {
    private val tokens: MutableList<Token> = ArrayList()

    private var start: Int = 0 // start Index of a lexeme
    private var current: Int = 0 // current unconsumed character
    private var line: Int = 1

    companion object {
        private val keywords:MutableMap<String, TokenType> = HashMap()
        init {
            keywords["and"] = TokenType.AND
            keywords["class"] = TokenType.CLASS
            keywords["else"] = TokenType.ELSE
            keywords["false"] = TokenType.FALSE
            keywords["true"] = TokenType.TRUE
            keywords["for"] = TokenType.FOR
            keywords["fun"] = TokenType.FUN
            keywords["if"] = TokenType.IF
            keywords["while"] = TokenType.WHILE
            keywords["nil"] = TokenType.NIL
            keywords["or"] = TokenType.OR
            keywords["print"] =TokenType.PRINT
            keywords["return"] = TokenType.RETURN
            keywords["super"] = TokenType.SUPER
            keywords["this"] = TokenType.THIS
            keywords["var"] = TokenType.VAR
        }
    }
    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    private fun isAtEnd() = current >= source.length
    private fun isDigit(c: Char) = c in '0'..'9'
    private val match = fun(expected: Char): Boolean {
        if (isAtEnd()) {
            return false
        }
        if (source[current] != expected) {
            return false
        }
        current++
        return true
    }
    private val peek = fun(): Char {
        if (isAtEnd()) {
            return 0.toChar()
        }
        return source[current]
    }
    private val peekNext = fun(): Char {
        if (current + 1 >= source.length) {
            return 0.toChar()
        }
        return source[current + 1]
    }
    private val number = fun() {
        while (isDigit(peek())) {
            advance()
        }
        if (peek() == '.' && isDigit(peekNext())) {
            advance()
            while (isDigit(peek())) {
                advance()
            }
        }

        addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
    }

    private fun isAlpha(c: Char) =
        (c in 'a'..'z') or (c in 'A'..'Z') or (c == '_')
    private fun isAlphaNumeric(c: Char) = isAlpha(c) or isDigit(c)

    private fun identifier() {
        while (isAlphaNumeric(peek())) {
            advance()
        }
        // see if the identifier is a reserved word
        val text = source.substring(start, current)
        val type = keywords[text] ?: TokenType.IDENTIFIER
        addToken(type)
    }

    private fun scanToken() {
        var c = advance()

        val string = fun() {
            while ((peek() != '"') and !isAtEnd()) {
                if (peek() == '\n') {
                    line++ // even the new line is inside of a string
                }
                advance()
            }
            if (isAtEnd()) {
                Lox.error(line, "Unterminated string.")
            }
            advance() // for the closing '"'
            val value = source.substring(start + 1, current - 1) // only take the pulp
            addToken(TokenType.STRING, value)
        }
        when (c) {
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
            '!' -> addToken(
                when (match('=')) {
                    true -> TokenType.BANG_EQUAL
                    else -> TokenType.BANG
                }
            )
            '=' -> addToken(
                when (match('=')) {
                    true -> TokenType.EQUAL_EQUAL
                    else -> TokenType.EQUAL
                }
            )
            '<' -> addToken(
                when (match('=')) {
                    true -> TokenType.LESS_EQUAL
                    else -> TokenType.LESS
                }
            )
            '>' -> addToken(
                when (match('=')) {
                    true -> TokenType.GREATER_EQUAL
                    else -> TokenType.GREATER
                }
            )

            '/' -> if (match('/')) {
                // a comment should goes until the end of the line
                while (peek() != '\n' && !isAtEnd()) {
                    // if peek() a '\n', break, and let the other case handle it
                    advance()
                }
            } else {
                addToken(TokenType.SLASH)
            }
            ' ' -> Unit
            '\r' -> Unit
            '\t' -> Unit
            '\n' -> line++
            '"' -> string()

            else ->
                if (isDigit(c)) {
                    number()
                } else if (isAlpha(c)) {
                    identifier()
                } else {
                    Lox.error(line, "Unexpected character.")
                }
        }
    }

    // return the last, advance a step. like pc in assembler
    private fun advance(): Char {
        current++
        return source[current - 1]
    }

    private fun addToken(type: TokenType) {
        addToken(type, null)
    }

    private fun addToken(type: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        // text is including surrounding symbol,
        // literal is the pulp that can store as a real value for meta-language
        tokens.add(Token(type, text, literal, line))
    }
}