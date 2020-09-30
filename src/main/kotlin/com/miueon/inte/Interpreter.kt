package com.miueon.inte

import java.lang.RuntimeException

class Interpreter : Expr.Visitor<Any> {
    fun interpret(expression: Expr?) {
        try {
            val value = evaluate(expression)
            println(stringify(value))
        } catch (error: RuntimeError) {
            Lox.runtimeError(error)
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
    // the interpreter evaluate a ast value in post-traverse
    override fun visitBinaryExpr(expr: Expr.Binary): Any {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)
        when (expr.operator.type) {
            TokenType.MINUS -> {
                chechNumberOperands(expr.operator, left, right)
                return left as Double - right as Double
            }
            TokenType.SLASH -> {
                chechNumberOperands(expr.operator, left, right)
                return left as Double / right as Double
            }
            TokenType.STAR -> {
                chechNumberOperands(expr.operator, left, right)
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
                chechNumberOperands(expr.operator, left, right)
                return left as Double > right as Double
            }
            TokenType.GREATER_EQUAL -> {
                chechNumberOperands(expr.operator, left, right)
                return left as Double >= right as Double
            }
            TokenType.LESS -> {
                chechNumberOperands(expr.operator, left, right)
                return (left as Double) < right as Double
            }
            TokenType.LESS_EQUAL -> {
                chechNumberOperands(expr.operator, left, right)
                return (left as Double) <= right as Double
            }
            TokenType.BANG_EQUAL -> {
                chechNumberOperands(expr.operator, left, right)
                return !isEqual(left, right)
            }
            TokenType.EQUAL_EQUAL -> {
                chechNumberOperands(expr.operator, left, right)
                return isEqual(left, right)
            }
        }

        return 0
    }
    private fun chechNumberOperands(operator: Token, left: Any?, right: Any?) {
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
                chechNumberOperand(expr.operator, right)
                return -(right as Double)
            }
            TokenType.BANG -> {

                return !isTruthy(right)
            }
        }
        return 0 // won't be reach here
    }

    private fun chechNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

}

// instead of using raw java exception, we defined a Error Class that can
// show the error msg in console and won't exist
// And this class track the Token that identifies where in the user's code
// the runtime error came from
class RuntimeError(val token: Token, val msg: String) : RuntimeException(msg)