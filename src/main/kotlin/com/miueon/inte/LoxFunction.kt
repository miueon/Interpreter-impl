package com.miueon.inte

class LoxFunction(
        private val declaration:Stmt.Function,
        private val closure:Environment,
        private val isInitializer:Boolean
):LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in declaration.params.indices) {
            environment[declaration.params[i].lexeme] = arguments[i]
        }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            if (isInitializer) return closure.getAt(0, "this")

            // if this never catches any of this Exception, it means the function
            // reached the end of its body without hitting a return statement.
            return returnValue.value
        }

        if (isInitializer) return closure.getAt(0, "this")
        return null
    }

    override fun arity(): Int {
        return declaration.params.size
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }

    fun bind(instance: LoxInstance):LoxFunction {
        val environment = Environment(closure)
        environment["this"] = instance
        return LoxFunction(declaration, environment, isInitializer)
    }
}