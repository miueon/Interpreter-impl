package com.miueon.inte

import java.beans.Expression
import java.lang.RuntimeException
import kotlin.math.exp

class Parser(
    val tokens:List<Token>,
    private var current:Int = 0
) {
    fun parse(): Expr? {
        try {
            return expression()
        } catch (e: ParseError) {
            return null
        }
    }

    /*
    expression     → equality ;
    equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    comparison     → addition ( ( ">" | ">=" | "<" | "<=" ) addition )* ;
    addition       → multiplication ( ( "-" | "+" ) multiplication )* ;
    multiplication → unary ( ( "/" | "*" ) unary )* ;
    unary          → ( "!" | "-" ) unary
                   | primary ;
    primary        → NUMBER | STRING | "false" | "true" | "nil"
                   | "(" expression ")" ;


    Grammar notation	Code representation
    Terminal	        Code to match and consume a token
    Nonterminal     	Call to that rule’s function
    |	                if or switch statement
    * or +	            while or for loop
    ?	                if statement
    * */
    private fun expression(): Expr {
        return equality()
    }
    // from string to tree: this is the inverse-traverse of tree of LOR type
    private fun match(vararg types: TokenType):Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType):Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }
    private fun advance():Token {
        if (!isAtEnd()) {
            current++
        }
        return previous()
    }
    private fun isAtEnd() = peek().type == TokenType.EOF
    private fun peek() = tokens[current]
    private fun previous() = tokens[current - 1]

    private fun equality(): Expr {
        // the left recursion is not suitable for recursive descent
        // cause it immediately call itself which calls itself again
        var expr = comparison()
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            // the current always point to the next Token
            val operator = previous()
            val right = comparison()
            // if expr like Parsing a == b == c == d == e.
            // eventually it would formed a left biased syntax tree
            // => cause every time it loop over, the last expr became the operand of
            // the new expr
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }
    private fun comparison(): Expr {
        var expr = addition()
        while (match(
                TokenType.GREATER,
                TokenType.GREATER_EQUAL,
                TokenType.LESS_EQUAL,
                TokenType.LESS
            )
        ) {
            val operator = previous()
            val right = addition()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }
    private fun addition(): Expr {
        var expr = multiplication()
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = multiplication()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }
    private fun multiplication(): Expr {
        var expr = unary()
        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return primary()
    }
    private fun primary() = when {
        match(TokenType.FALSE) -> Expr.Literal(false)
        match(TokenType.TRUE) -> Expr.Literal(true)
        match(TokenType.NIL) -> Expr.Literal(null)

        match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
        match(TokenType.LEFT_PAREN) -> {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression")
            Expr.Grouping(expr)
        }
        else -> throw error(peek(), "Expect expression.")
    }
    private fun consume(type: TokenType, msg: String):Token {
        if (check(type)) return advance()
        throw error(peek(), msg)
    }

    class ParseError : RuntimeException()
    private fun error(token: Token, msg:String): ParseError {
        Lox.error(token, msg)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return

            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR,
                    TokenType.FOR,TokenType.IF,TokenType.WHILE,
                    TokenType.PRINT,TokenType.RETURN -> return
                else -> {
                    advance()
                    continue
                }
            }
        }
    }

}