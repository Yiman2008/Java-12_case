# KR1 — простой проект Hello, World!

| Файл | Назначение |
|---|---|
| `Hello.java` | объявление класса `Hello` со статическим методом `void printHelloWorld(PrintStream)` |
| `HelloImpl.java` | определение (реализация) метода: выводит `"Hello, World!\n"` в указанный поток |
| `Main.java` | главная функция `main()`, вызывающая `Hello.printHelloWorld(System.out)` |

В Java имя файла с публичным классом должно совпадать с именем класса, поэтому вместо
`hello.java`, `hello_impl.java`, `main.java` из примера задания используются `Hello.java`,
`HelloImpl.java`, `Main.java`.

## Сборка и запуск

```bash
cd KR1
javac -d out *.java
java -cp out Main
# Hello, World!
```
