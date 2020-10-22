package com.miueon.inte

interface LoxCallable {
    fun call(interpreter:Interpreter, arguments:List<Any?>):Any?
    fun arity():Int
}