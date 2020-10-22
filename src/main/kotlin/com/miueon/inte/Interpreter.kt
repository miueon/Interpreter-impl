package com.miueon.inte

import java.lang.RuntimeException
import kotlin.math.exp
fun Environment.getAt(distance: Int, name: String): Any? {
    return ancestor(distance)?.values?.get(name)
}

private fun Environment.ancestor(distance: Int): Environment? {
    var environment:Environment? = this
    for (i in 0 until distance) {
        environment = environment?.enclosing
    }
    return environment
}
fun Environment.assignAt(distance: Int, name: Token, value: Any?) {
    ancestor(distance)?.values?.set(name.lexeme, value)
}

class Interpreter(
        val globals: Environment = Environment(),
        private var environment: Environment = globals,
        private val locals:MutableMap<Expr, Int> = HashMap()
) : Expr.Visitor<Any>, Stmt.Visitor<Unit> {
    //    fun interpret(expression: Expr?) {
//        try {
//            val value = evaluate(expression)
//            println(stringify(value))
//        } catch (error: RuntimeError) {
//            Lox.runtimeError(error)
//        }
//    }

    init {

        globals["clock"] = object :LoxCallable{
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                return System.currentTimeMillis()/ 1000.0
            }

            override fun arity(): Int {
                return 0
            }

            override fun toString(): String {
                return "<native fn clock>"
            }
        }
    }

    fun resolve(expr: Expr, depth: Int) {
        // this method let interpreter knows how many scopes between
        // the current scope and the scope where the variable is defined
        // We store the resolution information in locals.
        // # Or, we can store the info in the ast tree node.
        // so we can use it when the variable or assignment expression is later executed
        locals[expr] = depth
    }

    fun interpret(statements: List<Stmt?>) {
        try {
            for (statement in statements) {
                execute(statement ?: continue)
            }
        } catch (error: RuntimeError) {
            Lox.runtimeError(error)
        }
    }
    private fun execute(stmt: Stmt?) {
        stmt?.accept(this)
    }
    fun executeBlock(statements: List<Stmt?>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment // temporary change the environment
            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    private fun stringify(obj: Any?): String {
        if (obj == null) return "nil"
        if (obj is Double) {
            var text = obj.toString()
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length -2)
            }
            return text
        }
        return obj.toString()
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)

        val arguments:MutableList<Any?> = ArrayList()
        expr.arguments.forEach{
            arguments.add(evaluate(it))
        }
        if (callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call function and classed.")
        }
        val function:LoxCallable = callee
        if (arguments.size != function.arity()) {
            throw RuntimeError(expr.paren, "Expected ${function.arity()} arguments." +
                    "but got ${arguments.size}.")
        }
        return function.call(this, arguments)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function): Unit? {
        // for actual function declarations, isInitializer is distinguished from inner one
        val function = LoxFunction(stmt, environment, false)
        environment[stmt.name.lexeme] = function
        return Unit
    }

    override fun visitReturnStmt(stmt: Stmt.Return): Unit? {
        var value:Any? = null
        if (stmt.value != null) {
            value = evaluate(stmt.value)
        }
        // use Exception as unwind way to jump out from inner most to call()
        throw Return(value)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression): Unit? {
        evaluate(stmt.expression)
        return Unit
    }

    override fun visitPrintStmt(stmt: Stmt.Print): Unit? {
        val value = evaluate(stmt.expression)
        println(stringify(value))
        return Unit
    }

    override fun visitBlockStmt(stmt: Stmt.Block): Unit? {
        executeBlock(stmt.statements, Environment(environment))
        return Unit
    }

    override fun visitVarStmt(stmt: Stmt.Var): Unit? {
        var value:Any? = null
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer)
        }
        environment[stmt.name.lexeme] = value
        return Unit
    }

    // the interpreter evaluate a ast value in post-traverse
    override fun visitBinaryExpr(expr: Expr.Binary): Any {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)
        when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double - right as Double
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double / right as Double
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double * right as Double
            }
            TokenType.PLUS -> {
                if (left is String && right is String) {
                    return "${left}${right}"
                }
                if (left is Double && right is Double) {
                    return left + right
                }
            }
            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double > right as Double
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double >= right as Double
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) < right as Double
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) <= right as Double
            }
            TokenType.BANG_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return !isEqual(left, right)
            }
            TokenType.EQUAL_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return isEqual(left, right)
            }
        }

        return 0
    }
    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) {
            return
        }
        throw RuntimeError(operator, "Operands must be all numbers.")
    }

    private fun isEqual(left: Any?, right: Any?): Boolean {
        if (left == null && right == null) {
            return true
        }
        if (left == null) {
            return false
        }
        return left == right
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any {
        return evaluate(expr.expression)!!
    }

    private fun evaluate(expr: Expr?): Any? {
        return expr?.accept(this)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any {
        return expr.value!!
    }

    private fun isTruthy(`object`: Any?): Boolean {
        if (`object` == null) {
            return false
        }
        if (`object` is Boolean) {
            return `object`
        }
        return true
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any {
        val right = evaluate(expr.right)
        when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                return -(right as Double)
            }
            TokenType.BANG -> {

                return !isTruthy(right)
            }
        }
        return 0 // won't be reach here
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        // return environment[expr.name]
        // now, by static analysis, the interpreter could know the exactly scope
        // and jump to that
        return lookUpVariable(expr.name, expr)
    }


    private fun lookUpVariable(name: Token, expr: Expr):Any? {
        val distance = locals[expr]
        return if (distance != null) {
            environment.getAt(distance, name.lexeme)
        } else {
            globals[name]
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        //environment.assign(expr.name, value)
        val distance = locals[expr]
        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }
        return value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left
        } else {
            if (!isTruthy(left)) return left
        }
        return evaluate(expr.right)
    }

    override fun visitIfStmt(stmt: Stmt.If): Unit? {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
        return Unit
    }

    override fun visitWhileStmt(stmt: Stmt.While): Unit? {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
        return Unit
    }

    override fun visitGetExpr(expr: Expr.Get): Any? {
        val obj = evaluate(expr.`object`)
        if (obj is LoxInstance) {
            return obj[expr.name]
        }
        throw RuntimeError(expr.name, "Only instance have properties.")
    }

    override fun visitSetExpr(expr: Expr.Set): Any? {
        val obj = evaluate(expr.`object`)
        if (obj !is LoxInstance) {
            throw RuntimeError(expr.name, "Only instances have fileds.")
        }
        val value = evaluate(expr.value)
        obj[expr.name] = value
        return value
    }

    override fun visitThisExpr(expr: Expr.This): Any? {
        return lookUpVariable(expr.keyword, expr)
    }

    override fun visitClassStmt(stmt: Stmt.Class): Unit? {
        var superclass:Any? = null
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass)
            if (superclass !is LoxClass) {
                throw RuntimeError(stmt.superclass.name, "Superclass must be a class.")
            }
        }

        environment[stmt.name.lexeme] = null
        if (stmt.superclass != null) {
            environment = Environment(environment)
            environment["super"] = superclass
        }

        //val klass = LoxClass(stmt.name.lexeme)
        val methods:MutableMap<String, LoxFunction> = HashMap()
        stmt.methods.forEach{
            val function = LoxFunction(it, environment,
                    it.name.lexeme == "init")
            methods[it.name.lexeme] = function
        }
        // val klass = LoxClass(stmt.name.lexeme, methods)
        val klass = LoxClass(stmt.name.lexeme, superclass as LoxClass?, methods)

        if (superclass != null) {
            environment = environment.enclosing!!
        }

        environment.assign(stmt.name, klass)
        return Unit
    }

    override fun visitSuperExpr(expr: Expr.Super): Any? {
        val distance = locals[expr]
        val superclass = environment.getAt(distance!!, "super") as LoxClass
        val obj = environment.getAt(distance -1, "this") as LoxInstance
        val method = superclass.findMethod(expr.method.lexeme) ?:
                throw RuntimeError(expr.method, "Undefined property " +
                        "'${expr.method.lexeme}'.")
        return method.bind(obj)
    }
}

// instead of using raw java exception, we defined a Error Class that can
// show the error msg in console and won't exist
// And this class track the Token that identifies where in the user's code
// the runtime error came from
class RuntimeError(val token: Token, val msg: String) : RuntimeException(msg)

class Return(val value:Any?) : RuntimeException(null, null, false, false)