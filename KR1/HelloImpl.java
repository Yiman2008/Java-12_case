import java.io.PrintStream;

/** Определение (реализация) метода printHelloWorld. */
final class HelloImpl {
    private HelloImpl() {
    }

    static void printHelloWorld(PrintStream out) {
        out.print("Hello, World!\n");
    }
}
