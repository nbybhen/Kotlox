private enum class FunctionType {
    NONE,
    METHOD,
    FUNCTION,
    INITIALIZER
}

private enum class ClassType {
    NONE,
    CLASS
}

private var currentClass = ClassType.NONE

class Resolver(private val interpreter: Interpreter): Stmt.Visitor<Unit>, Expr.Visitor<Unit> {
    private val scopes: ArrayDeque<MutableMap<String, Boolean>> = ArrayDeque()
    private var currentFunction: FunctionType = FunctionType.NONE

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) = resolve(expr.right)

    override fun visitLiteralExpr(expr: Expr.Literal) {}

    override fun visitGroupingExpr(expr: Expr.Grouping) = resolve(expr.expression)

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.isEmpty() && scopes.last()[expr.name.lexeme] == false) {
            error(expr.name, "Can't read local variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        scopes.reversed().forEachIndexed { index, mutableMap ->
            if (mutableMap.containsKey(name.lexeme)) {
                interpreter.resolve(expr, index)
                return
            }
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)

        for (arg in expr.args) {
            resolve(arg)
        }
    }

    override fun visitGetExpr(get: Expr.Get) {
        resolve(get.obj)
    }

    override fun visitSetExpr(set: Expr.Set) {
        resolve(set.value)
        resolve(set.obj)
    }

    override fun visitThisExpr(arg: Expr.This) {
        if (currentClass == ClassType.NONE) {
            error(arg.keyword, "Can't use 'this' outside of class.")
            return
        }
        resolveLocal(arg, arg.keyword)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) = resolve(stmt.expression)

    override fun visitPrintStmt(stmt: Stmt.Print) = resolve(stmt.expression)

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        stmt.initializer?.let {
            resolve(it)
        }
        define(stmt.name)
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

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
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

    private fun resolve(stmt: Stmt?) {
        stmt?.accept(this@Resolver)
    }

    fun resolve(expr: Expr?) {
        expr?.accept(this@Resolver)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)

        stmt.elseBranch?.let {
            resolve(stmt.elseBranch)
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
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

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            error(stmt.keyword, "Can't return from top-level code.")
        }

        stmt.value?.let {
            if (currentFunction == FunctionType.INITIALIZER) {
                error(stmt.keyword, "Can't return a value from an initializer.")
            }
            resolve(stmt.value)
        }
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name)
        define(stmt.name)

        beginScope()
        scopes.last()["this"] = true

        for (method in stmt.methods) {
            var declaration = FunctionType.METHOD
            if (method.name.lexeme == "init") {
                declaration = FunctionType.INITIALIZER
            }
            resolveFunction(method, declaration)
        }

        endScope()

        currentClass = enclosingClass
    }
}