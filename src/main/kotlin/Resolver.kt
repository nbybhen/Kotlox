private enum class FunctionType {
    NONE,
    FUNCTION
}

class Resolver(val interpreter: Interpreter): Stmt.Visitor<Any?>, Expr.Visitor<Any?> {
    private val scopes: ArrayDeque<MutableMap<String, Boolean>> = ArrayDeque()
    private var currentFunction: FunctionType = FunctionType.NONE

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        resolve(expr.left)
        resolve(expr.right)
        return null
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        resolve(expr.right)
        return null
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return null
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        resolve(expr.expression)
        return null
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        if (!scopes.isEmpty() && scopes.last()[expr.name.lexeme] == false) {
            error(expr.name, "Can't read local variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)
        return null
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        scopes.reversed().forEachIndexed { index, mutableMap ->
            if (mutableMap.containsKey(name.lexeme)) {
                interpreter.resolve(expr, index)
                return
            }
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
        return null
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        resolve(expr.left)
        resolve(expr.right)
        return null
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        resolve(expr.callee)

        for (arg in expr.args) {
            resolve(arg)
        }

        return null
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression): Any? {
        resolve(stmt.expression)
        return null
    }

    override fun visitPrintStmt(stmt: Stmt.Print): Any? {
        resolve(stmt.expression)
        return null
    }

    override fun visitVarStmt(stmt: Stmt.Var): Any? {
        declare(stmt.name)
        stmt.initializer?.let {
            resolve(it)
        }
        define(stmt.name)
        return null
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return

        val scope: MutableMap<String, Boolean> = scopes.last()

        if (scope.containsKey(name.lexeme)) error(name, "Already a variable with this name in this scope.")

        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.last()[name.lexeme] = true
    }

    override fun visitBlockStmt(stmt: Stmt.Block): Any? {
        beginScope()
        resolve(stmt.statements)
        endScope()
        return null
    }

    private fun beginScope() {
        scopes.addLast(mutableMapOf())
    }

    private fun endScope() {
        scopes.removeLast()
    }

    fun resolve(stmts: List<Stmt?>) {
        for (stmt in stmts) {
            resolve(stmt)
        }
    }

    fun resolve(stmt: Stmt?) {
        stmt?.accept(this@Resolver)
    }

    fun resolve(expr: Expr?) {
        expr?.accept(this@Resolver)
    }

    override fun visitIfStmt(stmt: Stmt.If): Any? {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)

        stmt.elseBranch?.let {
            resolve(stmt.elseBranch)
        }

        return null
    }

    override fun visitWhileStmt(stmt: Stmt.While): Any? {
        resolve(stmt.condition)
        resolve(stmt.body)
        return null
    }

    override fun visitFunctionStmt(stmt: Stmt.Function): Any? {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
        return null
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }

        resolve(function.body)
        endScope()
        currentFunction = enclosingFunction
    }

    override fun visitReturnStmt(stmt: Stmt.Return): Any? {
        if (currentFunction == FunctionType.NONE) {
            error(stmt.keyword, "Can't return from top-level code.")
        }

        stmt.value?.let {
            resolve(stmt.value)
        }
        return null
    }

}