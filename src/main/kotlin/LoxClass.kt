class LoxClass(val name: String, val superclass: LoxClass?, val methods: MutableMap<String, LoxFunction>): LoxCallable {
    override fun arity(): Int {
        val initializer = findMethod("init")
        initializer?.let { return it.arity() }
        return 0
    }

    override fun call(interpreter: Interpreter, args: List<Any?>): Any {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, args)
        return instance
    }

    fun findMethod(name: String): LoxFunction? {
        return methods[name] ?: superclass?.findMethod(name)
    }

    override fun toString(): String {
        return name
    }
}