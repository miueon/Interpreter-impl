package com.miueon.inte

import com.miueon.tool.defineAst
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.exp

private enum class FunctionType {
    NONE,
    FUNCTION,
    METHOD,
    INITIALIZER
}
private enum class ClassType {
    NONE,
    CLASS,
    SUBCLASS // to check a whether a super call is valid inside a method
}

class Resolver(
        private val interpreter: Interpreter
) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    // the scopes here is like a call stack,
    // Each element in the stack is a Map representing a single block scope
    // only used for local block scopes
    // When resolving a variable,
    //if we can’t find it in the stack of local scopes, we assume it must be global.
    private val scopes: Stack<MutableMap<String, Boolean>> = Stack()
    private var currentFunction: FunctionType = FunctionType.NONE
    private var currentClass:ClassType = ClassType.NONE

    override fun visitBinaryExpr(expr: Expr.Binary): Unit? {
        resolve(expr.left)
        resolve(expr.right)
        return Unit
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Unit? {
        resolve(expr.expression)
        return Unit
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Unit? {
        return Unit
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Unit? {
        resolve(expr.right)
        return Unit
    }

    override fun visitVariableExpr(expr: Expr.Variable): Unit? {
        if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme] != null) {
            if (scopes.peek()[expr.name.lexeme] == false) {
                Lox.error(expr.name, "Can't read local variable in its own initializer.")
            }
        }
        resolveLocal(expr, expr.name)
        return Unit
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.size - 1 downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    override fun visitCallExpr(expr: Expr.Call): Unit? {
        resolve(expr.callee)

        expr.arguments.forEach {
            resolve(it)
        }
        return Unit
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Unit? {
        resolve(expr.right)
        resolve(expr.left)
        return Unit
    }

    override fun visitAssignExpr(expr: Expr.Assign): Unit? {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
        return Unit
    }

    override fun visitBlockStmt(stmt: Stmt.Block): Unit? {
        beginScope()
        resolve(stmt.statements)
        endScope()
        return Unit
    }

    override fun visitVarStmt(stmt: Stmt.Var): Unit? {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }

        define(stmt.name)
        return Unit
    }

    // if the scopes is empty, assume the var is in the global scope
    private fun declare(name: Token) {
        if (scopes.isEmpty()) {
            return
        }
        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already variable with this name in this scope.")
        }
        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) {
            return
        }
        scopes.peek()[name.lexeme] = true
    }

    private fun beginScope() {
        scopes.push(HashMap())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    fun resolve(statements: List<Stmt?>) {
        statements.forEach {
            resolve(it)
        }
    }

    private fun resolve(stmt: Stmt?) {
        stmt?.accept(this)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression): Unit? {
        resolve(stmt.expression)
        return Unit
    }

    override fun visitFunctionStmt(stmt: Stmt.Function): Unit? {
        declare(stmt.name)
        define(stmt.name)
        // define the func name eagerly makes function recursively call possible
        resolveFunction(stmt, FunctionType.FUNCTION)
        return Unit
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        // piggyback the type stack on JVM
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        function.params.forEach {
            declare(it)
            define(it)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
    }

    override fun visitIfStmt(stmt: Stmt.If): Unit? {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch)
        }
        return Unit
    }

    override fun visitPrintStmt(stmt: Stmt.Print): Unit? {
        resolve(stmt.expression)
        return Unit
    }

    override fun visitReturnStmt(stmt: Stmt.Return): Unit? {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.")
        }

        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer.")
            }

            resolve(stmt.value)
        }
        return Unit
    }


    override fun visitWhileStmt(stmt: Stmt.While): Unit? {
        resolve(stmt.condition)
        resolve(stmt.body)
        return Unit
    }

    override fun visitGetExpr(expr: Expr.Get): Unit? {
        resolve(expr.`object`)
        return Unit
    }

    override fun visitSetExpr(expr: Expr.Set): Unit? {
        resolve(expr.value)
        resolve(expr.`object`)
        return Unit
    }

    override fun visitThisExpr(expr: Expr.This): Unit? {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'This' outside of a class.")
            return Unit
        }

        resolveLocal(expr, expr.keyword)
        return null
    }

    override fun visitClassStmt(stmt: Stmt.Class): Unit? {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name)
        define(stmt.name)

        if (stmt.superclass != null) {
            if (stmt.name.lexeme == stmt.superclass.name.lexeme) {
                Lox.error(stmt.superclass.name, "A class can't inherit from itself.")
            }
            currentClass = ClassType.SUBCLASS
            resolve(stmt.superclass)
            // enclosing the scope, create a new environment for super class
            beginScope()
            scopes.peek()["super"] = true
        }

        // Decl this
        beginScope()
        scopes.peek()["this"] = true
        stmt.methods.forEach {
            var declaration = FunctionType.METHOD
            if (it.name.lexeme == "init") {
                declaration = FunctionType.INITIALIZER
            }
            resolveFunction(it, declaration)
        }
        endScope()

        if (stmt.superclass != null) {
            endScope()
        }
        currentClass = enclosingClass
        return Unit
    }

    override fun visitSuperExpr(expr: Expr.Super): Unit? {
        when (currentClass) {
            ClassType.NONE -> Lox.error(expr.keyword,
                    "Can't use 'super' outside of a class.")
            ClassType.CLASS -> Lox.error(expr.keyword,
            "Can't call 'super' in a class without superclass.")
        }

        resolveLocal(expr, expr.keyword)
        return Unit
    }
}