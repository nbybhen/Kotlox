import kotlin.RuntimeException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Any?> {
    private val globals = Environment()
    private var environment = globals
    private var locals = mutableMapOf<Expr, Int>()

    init {
        globals.define("clock", object: LoxCallable {
            override fun arity(): Int {
                return 0
            }

            override fun call(interpreter: Interpreter, args: List<Any?>): Double {
                return System.currentTimeMillis().toDouble() / 1000.0
            }
        })
    }

    fun interpret(stmts: List<Stmt?>) {
        try {
            for(stmt in stmts) {
                if (stmt != null) {
                    execute(stmt)
                }
            }
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
    }

    private fun execute(stmt: Stmt?) {
        stmt?.accept(this)
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when(expr.operator.type) {
            TokenType.PLUS -> {
                if(left is Double && right is Double) {
                    left + right
                }
                else if(left is String && right is String) {
                    "$left$right"
                }
                else {
                    throw RuntimeError(expr.operator, "Operands must be two numbers or two strings")
                }
            }
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                left - right
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right)
                left * right
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                left / right
            }
            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                left > right
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                left >= right
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right)
                left  < right
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                left <= right
            }
            TokenType.BANG_EQUAL -> !isEqual(left, right)
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            else -> null
        }
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        return when(expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                -right
            }
            TokenType.BANG -> !isTruthy(right)
            else -> null
        }
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        return if (distance != null) {
            environment.getAt(distance, name.lexeme)
        } else {
            globals.get(name)
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        val distance = locals[expr]

        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        }
        else {
            globals.assign(expr.name, value)
        }

        return value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left
        }
        else {
            if (!isTruthy(left)) return left
        }

        return evaluate(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)

        val args = expr.args.map { evaluate(it) }

        if(callee !is LoxCallable) throw RuntimeError(expr.paren, "Can only call functions and classes.")

        if (args.size != callee.arity()) throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${args.size}.")

        return callee.call(this@Interpreter, args)
    }

    private fun evaluate(expr: Expr?) : Any? {
        return expr?.accept(this)
    }

    //
    // Helper Functions
    //

    private fun isTruthy(obj: Any?): Boolean {
        return when(obj) {
            null -> false
            is Boolean -> obj
            else -> true
        }
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        if(a == null && b == null) return true
        if(a == null) return false

        return a.equals(b)
    }

    @OptIn(ExperimentalContracts::class)
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        contract {
            returns() implies (operand is Double)
        }
        if(operand !is Double) throw RuntimeError(operator, "Operand must be a number.")
    }

    @OptIn(ExperimentalContracts::class)
    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        contract {
            returns() implies (left is Double && right is Double)
        }
        if(left !is Double || right !is Double) throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun stringify(obj: Any?): Any {
        return when(obj) {
            null -> "nil"
            is Double -> {
                var text = obj.toString()
                if(text.endsWith(".0")) {
                    text = text.substring(0, text.length - 2)
                }
                text
            }
            else -> obj.toString()
        }
    }

    fun executeBlock(stmts: List<Stmt?>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment

            for (stmt in stmts) {
                execute(stmt)
            }
        } finally {
            this.environment = previous
        }
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression): Any? {
        this.evaluate(stmt.expression)
        return null
    }

    override fun visitPrintStmt(stmt: Stmt.Print): Any? {
        val value = evaluate(stmt.expression)
        println(stringify(value))
        return null
    }

    override fun visitVarStmt(stmt: Stmt.Var): Any? {
        var value: Any? = null
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer)
        }

        environment.define(stmt.name.lexeme, value)
        return null
    }

    override fun visitBlockStmt(stmt: Stmt.Block): Any? {
        executeBlock(stmt.statements, Environment(this.environment))
        return null
    }

    override fun visitIfStmt(stmt: Stmt.If): Any? {
        if (isTruthy(evaluate(stmt.condition))) {
           execute(stmt.thenBranch)
        }
        else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
        return null
    }

    override fun visitWhileStmt(stmt: Stmt.While): Any? {
        while (isTruthy(evaluate(stmt.condition))) execute(stmt.body)
        return null
    }

    override fun visitFunctionStmt(stmt: Stmt.Function): Any? {
        val function = LoxFunction(stmt, environment)
        environment.define(stmt.name.lexeme, function)
        return null
    }

    override fun visitReturnStmt(stmt: Stmt.Return): Any? {
        val value: Any? = if (stmt.value != null) evaluate(stmt.value) else null
        throw Return(value)
    }
}

class RuntimeError(val token: Token, override val message: String?) : RuntimeException()

class Return(val value: Any?): Throwable()