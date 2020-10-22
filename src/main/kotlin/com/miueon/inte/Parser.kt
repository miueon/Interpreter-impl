package com.miueon.inte

import java.lang.RuntimeException

class Parser(
        val tokens: List<Token>,
        private var current: Int = 0
) {
    //    fun parse(): Expr? {
//        try {
//            return expression()
//        } catch (e: ParseError) {
//            return null
//        }
//    }
    fun parse(): List<Stmt?> {
        val statements: MutableList<Stmt?> = ArrayList()
        while (!isAtEnd()) {  // the star of rules
            statements.add(declaration())
        }
        return statements
    }

    private fun declaration(): Stmt? {
        return try {
            when {
                match(TokenType.FUN) -> function("function")
                match(TokenType.VAR) -> varDeclaration()
                match(TokenType.CLASS) -> classDeclaration()
                else -> statement()
            }
        } catch (error: ParseError) {
            synchronize()
            null
        }
    }

    private fun classDeclaration(): Stmt? {
        val name = consume(TokenType.IDENTIFIER, "Expect a name for a class.")
        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.")

        val methods: MutableList<Stmt.Function> = ArrayList()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }

        var superclass: Expr.Variable? = null
        if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Expect superclass name.")
            superclass = Expr.Variable(previous())
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.")
        return Stmt.Class(name, superclass, methods)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
        var initializer: Expr? = null
        if (match(TokenType.EQUAL)) {
            initializer = expression()
        }

        consume(TokenType.SEMICOLON, "Expect ';' after a variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement() = when {
        match(TokenType.PRINT) -> printStatement()
        match(TokenType.LEFT_BRACE) -> Stmt.Block(block())
        match(TokenType.IF) -> ifStatement()
        match(TokenType.WHILE) -> whileStatement()
        match(TokenType.FOR) -> forStatement()
        match(TokenType.RETURN) -> returnStatement()
        else -> expressionStatement()
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        var value: Expr? = null
        if (!check(TokenType.SEMICOLON)) {
            value = expression()
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")

        var initializer: Stmt? = when {
            match(TokenType.SEMICOLON) -> null
            match(TokenType.VAR) -> varDeclaration()
            else -> expressionStatement()
        }

        var condition: Expr? = null
        if (!check(TokenType.SEMICOLON)) {
            condition = expression()
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition")

        var increment: Expr? = null
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression()
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' to enclose a for loop.")
        var body = statement()
        if (increment != null) {
            body = Stmt.Block(mutableListOf(body, Stmt.Expression(increment)))
        }
        if (condition == null) condition = Expr.Literal(true)
        body = Stmt.While(condition, body)

        if (initializer != null) {
            body = Stmt.Block(mutableListOf(initializer, body))
        }
        return body
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()
        return Stmt.While(condition, body)
    }

    private fun block(): MutableList<Stmt?> {
        val statements: MutableList<Stmt?> = ArrayList()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration())
        }
        consume(TokenType.RIGHT_BRACE, "Expect ')' to end a block.")
        return statements
    }

    private fun printStatement(): Stmt {
        val value = expression() // value here is still a AST, that means it literal
        // value need to evaluate in Interpreter
        consume(TokenType.SEMICOLON, "Expect ';' to determine the end of a statement")
        return Stmt.Print(value)
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'if' condition.")

        val thenBranch = statement()
        var elseBranch: Stmt? = null
        if (match(TokenType.ELSE)) {
            elseBranch = statement()
        }
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' to determine the end of a statement")
        return Stmt.Expression(expr)
    }

    private fun function(kind: String): Stmt.Function {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters: MutableList<Token> = ArrayList()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }

                parameters.add(
                        consume(TokenType.IDENTIFIER, "Expect parameter name.")
                )
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")

        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind to start a func.")
        val body: List<Stmt?> = block()
        return Stmt.Function(name, parameters, body)
    }

    /*

    ------------ new rules for statement ------------------
    program   → statement* EOF ;

    -----  separate varDeclaration and statement------
    declaration → classDecl
                 | funDecl
                 | varDecl
                 | statement ;
    classDecl   → "class" IDENTIFIER ( "<" IDENTIFIER )?
                    "{" function* "}" ;  # without "fun" symbol
    funDecl     → "fun" function ;
    function    → IDENTIFIER "(" parameters? ")" block ;
    parameters  → IDENTIFIER ( "," IDENTIFIER )* ;
    varDecl → "var" IDENTIFIER ( "=" expression )? ";" ;
    --------------------------------------------------

    statement → exprStmt
              | ifStmt
              | printStmt
              | returnStmt
              | whileStmt
              | forStmt
              | block ;
    returnStmt → "return" expression? ";" ;
    forStmt   → "for" "(" ( varDecl | exprStmt | ";" )
                 expression? ";"
                 expression? ")" statement ;
    whileStmt → "while" "(" expression ")" statement ;
    block     → "{" declaration* "}" ;
    ifStmt    → "if" "(" expression ")" statement
               ( "else" statement )? ;
    exprStmt  → expression ";" ;
    printStmt → "print" expression ";" ;
    -------------------------------------------------------

    expression → assignment ;

    assignment → ( call "." )? IDENTIFIER "=" assignment
                 | logic_or ;
    logic_or       → logic_and ( "or" logic_and )* ;
    logic_and      → equality ( "and" equality )* ;
    equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    comparison     → addition ( ( ">" | ">=" | "<" | "<=" ) addition )* ;
    addition       → multiplication ( ( "-" | "+" ) multiplication )* ;
    multiplication → unary ( ( "/" | "*" ) unary )* ;
    unary          → ( "!" | "-" ) unary
                   | call ;
    call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
     // the call expr take optional num of arg

    arguments      → expression ( "," expression )* ;
    primary        → NUMBER | STRING | "false" | "true" | "nil" | "this"
                   | "(" expression ")"
                   | IDENTIFIER
                   | "super" "." IDENTIFIER ;
    -------------------------------------------------------
    Grammar notation	Code representation
    Terminal	        Code to match and consume a token
    Nonterminal     	Call to that rule’s function
    |	                if or switch statement
    * or +	            while or for loop
    ?	                if statement


    * */
    private fun expression(): Expr {
        // l-value <-> r-value
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = or()
        // before create the assignment expression, we first treat the left side
        // as a r-value, e.g. a expression, to figure out that kind assignment target
        // it is. like newPoint(x + 2, 0).y = 3
        // then, if the parser find the '=' Token, treat is as a l-value
        if (match(TokenType.EQUAL)) {
            val eqs = previous()
            val value = assignment()
            // IDENTIFIER "=" assignment
            // | equality ;
            return when (expr) {
                is Expr.Variable -> {
                    val name = expr.name
                    Expr.Assign(name, value)
                }
                is Expr.Get -> {
                    Expr.Set(expr.`object`, expr.name, value)
                }
                else -> throw this.error(eqs, "Invalid assignment target.")
            }
        }
        return expr
    }

    fun or(): Expr {
        var expr = and()
        while (match(TokenType.OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    fun and(): Expr {
        var expr = equality()
        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    // from string to tree: this is the inverse-traverse of tree of LOR type
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
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

        return call()
    }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            expr = when {
                match(TokenType.LEFT_PAREN) -> finishCall(expr)
                match(TokenType.DOT) -> {
                    val name = consume(TokenType.IDENTIFIER, "Expect property " +
                            "name after '.'.")
                    Expr.Get(expr, name)
                }
                else -> break
            }
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments: MutableList<Expr> = ArrayList()
        // if the next token is right_paren, then it means zero-arg.
        // so, the arguments length will be 0
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can't have more then 255 arguments.")
                }
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }
        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' to end a call.")

        return Expr.Call(callee, paren, arguments)
    }

    private fun primary() = when {
        match(TokenType.FALSE) -> Expr.Literal(false)
        match(TokenType.TRUE) -> Expr.Literal(true)
        match(TokenType.NIL) -> Expr.Literal(null)

        match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
        match(TokenType.THIS) -> Expr.This(previous())
        match(TokenType.SUPER) -> {
            val keyword = previous()
            consume(TokenType.DOT, "Expect '.' after 'super'.")
            val method = consume(TokenType.IDENTIFIER, "Expect superclass method name.")
            Expr.Super(keyword, method)
        }
        match(TokenType.LEFT_PAREN) -> {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression")
            Expr.Grouping(expr)
        }
        match(TokenType.IDENTIFIER) -> {
            Expr.Variable(previous())
        }

        else -> throw error(peek(), "Expect expression.")
    }

    private fun consume(type: TokenType, msg: String): Token {
        if (check(type)) return advance()
        throw error(peek(), msg)
    }

    class ParseError : RuntimeException()

    private fun error(token: Token, msg: String): ParseError {
        Lox.error(token, msg)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return

            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR,
                TokenType.FOR, TokenType.IF, TokenType.WHILE,
                TokenType.PRINT, TokenType.RETURN -> return
                else -> {
                    advance()
                    continue
                }
            }
        }
    }

}