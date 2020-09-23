package com.miueon.inte

class Token(
    val type:TokenType,
    val lexeme:String, // raw string by grouping
    val literal:Any?, // the literal value ....
    val line:Int // indicate where error happens
) {
    override fun toString(): String {
        return "${type} ${lexeme} ${literal}"
    }
}