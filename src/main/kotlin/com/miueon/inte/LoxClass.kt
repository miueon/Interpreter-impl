package com.miueon.inte

class LoxClass(
        val name: String,
        val superclass:LoxClass?,
        private val methods:MutableMap<String, LoxFunction>
) : LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)
        return instance
    }

    override fun arity(): Int {
        val initializer = findMethod("init") ?: return 0
        return initializer.arity()
    }

    override fun toString(): String {
        return name
    }

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) {
            return methods[name]
        }

        if (superclass != null) {
            return superclass.findMethod(name)
        }
        return null
    }
}

class LoxInstance(
        private val klass: LoxClass,
        private val fields: MutableMap<String, Any?> = HashMap()
) {
    operator fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }
        val method = klass.findMethod(name.lexeme)
        if (method != null) {
            return method.bind(this)
        }
        throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
    }

    operator fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }

    override fun toString(): String {
        return klass.name + " instance"
    }
}