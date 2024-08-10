sealed interface Stmt {
    interface Visitor<R> {
        fun visitExpressionStmt(stmt: Expression) : R
        fun visitPrintStmt(stmt: Print) : R
        fun visitVarStmt(stmt: Var): R
    }

    data class Expression(val expression: Expr) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitExpressionStmt(this@Expression)
        }
    }

    data class Print(val expression: Expr): Stmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitPrintStmt(this@Print)
        }
    }

    data class Var(val name: Token, val initializer: Expr?): Stmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitVarStmt(this@Var)
        }

    }

    fun<R> accept(visitor: Visitor<R>): R
}