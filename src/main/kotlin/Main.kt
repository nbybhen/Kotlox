import kotlin.system.exitProcess

fun runPrompt() {
    // TODO
}

fun runFile(file: String) {
    // TODO
}

fun main(args: Array<String>) {
    println("Args: ${args}");
    when (args.size) {
        0 -> runPrompt()
        1 -> runFile(args[0])
        else -> {
            println("Usage: klox [script]");
            exitProcess(64);
        }
    }
}