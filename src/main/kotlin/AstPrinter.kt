class AstPrinter : Expr.Visitor<String> {
    fun print(expr: Expr): String {
        return expr.accept(this);
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        var builder: StringBuilder = StringBuilder()
        builder.append("($name")
        for(expr in exprs) {
            builder.append(" ").append(expr.accept(this))
        }
        builder.append(")")

        return builder.toString()
    }
    override fun visitBinaryExpr(expr: Expr.Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        if(expr.value == null) {
            return "nil"
        }
        return expr.value.toString()
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitVariableExpr(expr: Expr.Variable): String {
        TODO("Not yet implemented")
    }

    override fun visitAssignExpr(expr: Expr.Assign): String {
        TODO("Not yet implemented")
    }

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        TODO("Not yet implemented")
    }

    override fun visitCallExpr(expr: Expr.Call): String {
        TODO("Not yet implemented")
    }

    override fun visitGetExpr(get: Expr.Get): String {
        TODO("Not yet implemented")
    }

    override fun visitSetExpr(set: Expr.Set): String {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(arg: Expr.This): String {
        TODO("Not yet implemented")
    }

}