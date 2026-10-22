import java.io.PrintStream;

/**
 * Объявление класса Hello со статическим методом printHelloWorld(PrintStream).
 * Сама реализация вынесена в отдельный файл HelloImpl.java.
 */
public class Hello {
    /** Выводит в указанный поток строку "Hello, World!\n". */
    public static void printHelloWorld(PrintStream out) {
        HelloImpl.printHelloWorld(out);
    }
}
