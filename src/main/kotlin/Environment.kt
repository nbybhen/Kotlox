class Environment(val enclosing: Environment? = null) {
    private val values: MutableMap<String, Any?> = mutableMapOf()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? {
        return values.getOrElse(name.lexeme) {
            enclosing?.get(name) ?: throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }

    fun ancestor(distance: Int): Environment {
        var environment = this@Environment
        for (i in 0..<distance) {
            environment = environment.enclosing!!
        }

        return environment
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values[name]
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance).values[name.lexeme] = value
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        enclosing?.assign(name, value) ?: throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}