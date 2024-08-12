sealed interface Stmt {
    interface Visitor<R> {
        fun visitExpressionStmt(stmt: Expression) : R
        fun visitPrintStmt(stmt: Print) : R
        fun visitVarStmt(stmt: Var): R
        fun visitBlockStmt(stmt: Block): R
        fun visitIfStmt(stmt: If): R
        fun visitWhileStmt(stmt: While): R
        fun visitFunctionStmt(stmt: Function): R
        fun visitReturnStmt(stmt: Return): R
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

    data class Function(val name: Token, val params: List<Token>, val body: List<Stmt?>): Stmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitFunctionStmt(this@Function)
        }
    }

    data class Return(val keyword: Token, val value: Expr?): Stmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitReturnStmt(this@Return)
        }

    }

    fun<R> accept(visitor: Visitor<R>): R
}