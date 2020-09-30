package com.miueon.inte

import kotlin.text.StringBuilder
// this class show the value of Visitor pattern

// first: we need a Expression representation. And that is the Expr Classes(Unary, binary..
// second: the Expr classes should have the same op,
//         this means they keep the same "type" as the abstract Expr
// third: the original oop style keeping the same "type" means
//          whenever you want to add a new method to a specific sub-class like binary
//         you should add it across all the Expr class family, and that would be chores

// fourth: the way to save we from the hell is using Visitor pattern:
//              1. add only one method that accept a Visitor{ a interface } to the
//                      abstract class
//              2. inside Visitor interface, we can add a list of function that can handle
//                       with different sub-class { depends on different purpose,
//                         the interface and the function can be a generic one.

// conclusion: 1. we have a Tree-like classes ,
class AstPrinter : Expr.Visitor<String> {
    fun print(expr: Expr): String {
        return expr.accept(this)!!
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        if (expr.value == null) {
            return "nil"
        }
        return expr.value.toString()
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val thisOut = this
        return with(StringBuilder()) {
            append("(${name}")
            for (expr in exprs) {
                append(" ${expr.accept(thisOut)}")
            }
            append(")")
        }.toString()
    }
}

fun main() {
    val expression = Expr.Binary(
        Expr.Unary(
            Token(TokenType.MINUS, "-", null, 1),
            Expr.Literal(123)
        ),
        Token(TokenType.STAR, "*", null, 1),
        Expr.Grouping(
            Expr.Literal(45.67)
        )
    )
    println(AstPrinter().print(expression))
}

