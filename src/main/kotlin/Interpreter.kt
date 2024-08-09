import kotlin.RuntimeException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Interpreter : Expr.Visitor<Any?> {
    fun interpret(expression: Expr?) {
        try {
            val value = evaluate(expression)
            println(stringify(value))
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
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

}

class RuntimeError(val token: Token, override val message: String?) : RuntimeException()