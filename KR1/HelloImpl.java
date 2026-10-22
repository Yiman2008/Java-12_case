import java.io.PrintStream;

final class HelloImpl {
    private HelloImpl() {
    }

    static void printHelloWorld(PrintStream out) {
        out.print("Hello, World!\n");
    }
}
