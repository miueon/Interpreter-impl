package com.miueon.inte

class Environment(val enclosing:Environment? = null) {

    val values:MutableMap<String, Any?> = HashMap()

    operator fun set(name: String, value: Any?) {
        // if the user re-declare a variable, then it will cover the old one
        values[name] = value
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }
        enclosing?.let {
            it.assign(name,value)
            return
        }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    operator fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }
        enclosing?.let {
            return it[name]
        }
        throw RuntimeError(name, "Undefined variable: '${name.lexeme}'.")
        // defer to the runtime allow refer a variable that not yet defined

    }


    /*
    * The problem is that using a variable isn’t the same as referring to it.
    * You can refer to a variable in a chunk of code
    *  without immediately evaluating it if that chunk of code
    *  is wrapped inside a function.
    *
    *  If we make it a static error to mention a variable
    *  before it’s been declared,
    *  it becomes much harder to define recursive functions.
    *
    * */
}