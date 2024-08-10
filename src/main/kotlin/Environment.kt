class Environment {
    private val values: MutableMap<String, Any?> = mutableMapOf()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) return values.get(name.lexeme)
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}