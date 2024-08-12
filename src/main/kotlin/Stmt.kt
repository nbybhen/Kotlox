sealed interface Stmt {
    interface Visitor<R> {
        fun visitExpressionStmt(stmt: Expression) : R
        fun visitPrintStmt(stmt: Print) : R
        fun visitVarStmt(stmt: Var): R
        fun visitBlockStmt(stmt: Block): R
        fun visitIfStmt(stmt: If): R
        fun visitWhileStmt(stmt: While): R
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

    data class Block(val statements: List<Stmt?>): Stmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitBlockStmt(this@Block)
        }
    }

    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?): Stmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitIfStmt(this@If)
        }
    }

    data class While(val condition: Expr, val body: Stmt?): Stmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitWhileStmt(this@While)
        }
    }

    fun<R> accept(visitor: Visitor<R>): R
}