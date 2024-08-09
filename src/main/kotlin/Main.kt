import kotlin.system.exitProcess
import java.io.File

var hasError = false;

fun error(line: Int, message: String) {
    report(line, "", message)
}

fun error(token: Token, message: String) {
    if(token.type == TokenType.EOF) {
        report(token.line, "at end", message)
    }
    else {
        report(token.line, " at '${token.lexeme}'" ,message)
    }
}

fun report(line: Int, where: String, message: String) {
    System.err.println("[line $line] Error $where: $message")
    hasError = true
}

// Starts language REPL
fun runPrompt() {
    while(true) {
        print("> ")
        val line = readlnOrNull() ?: break
        run(line)
    }
}

// Runs language via .klox file (chosen in Intellij configuration)
fun runFile(path: String) {
    val inputStream = File(path).inputStream()
    val inputString = inputStream.bufferedReader().use { it.readText() }
    run(inputString)
}


fun run(source: String) {
    val scanner = Scanner(source)
    val tokens: List<Token> = scanner.scanTokens()

    val parser = Parser(tokens)
    val expr: Expr? = parser.parse()

    if(hasError) return;

    if(expr != null) {
        println(AstPrinter().print(expr))
    }
}

fun main(args: Array<String>) {
    println("Args: ${args.joinToString(" | ")}")
    when (args.size) {
        0 -> runPrompt()
        1 -> runFile(args[0])
        else -> {
            println("Usage: klox [script]")
            exitProcess(64)
        }
    }
    val expression = Expr.Binary(Expr.Unary(Token(TokenType.MINUS, "-", null, 1), Expr.Literal(123)),
        Token(TokenType.STAR, "*", null, 1),
        Expr.Grouping(Expr.Literal(45.67)))
    println(AstPrinter().print(expression))
}