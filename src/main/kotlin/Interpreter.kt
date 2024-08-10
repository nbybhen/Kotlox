import kotlin.RuntimeException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Any?> {
    var environment = Environment()
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
        return environment.get(expr.name)
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        environment.assign(expr.name, value)
        return value
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

    private fun executeBlock(stmts: List<Stmt?>, environment: Environment) {
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
}

class RuntimeError(val token: Token, override val message: String?) : RuntimeException()