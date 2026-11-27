package ru.mirea.sqlreport.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import ru.mirea.sqlreport.ReportEngine;
import ru.mirea.sqlreport.model.ResultTable;
import ru.mirea.sqlreport.output.OutputFormat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

@Command(name = "sqlreport", mixinStandardHelpOptions = true, version = "SQLReport 0.3.0",
        description = "Генератор отчётов по CSV-файлам")
public final class ReportCommand implements Callable<Integer> {
    @Spec
    private CommandSpec spec;

    @Option(names = {"-q", "--query"}, required = true, description = "SQL-подобный запрос")
    private String query;

    @Option(names = "--format", defaultValue = "TABLE",
            description = "Формат отчёта: ${COMPLETION-CANDIDATES}")
    private OutputFormat format;

    @Option(names = "--out", description = "Путь для сохранения нового файла в UTF-8")
    private Path output;

    @Override
    public Integer call() {
        try {
            ResultTable result = new ReportEngine().execute(query);
            String report = format.formatter().format(result);
            if (output == null) {
                spec.commandLine().getOut().print(report);
                spec.commandLine().getOut().flush();
            } else {
                Files.writeString(output, report, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            return 0;
        } catch (IOException | IllegalArgumentException e) {
            spec.commandLine().getErr().println("Ошибка: " + e.getMessage());
            return 1;
        }
    }
}
