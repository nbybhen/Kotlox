import kotlin.system.exitProcess
import java.io.File

fun error(line: Int, message: String) {
    report(line, "", message)
}

fun report(line: Int, where: String, message: String) {
    System.err.println("[line $line] Error $where: $message")
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
    val tokens = scanner.scanTokens()

    for(token in tokens) {
        println("$token")
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
}