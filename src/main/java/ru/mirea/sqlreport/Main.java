package ru.mirea.sqlreport;

import picocli.CommandLine;

import ru.mirea.sqlreport.cli.ReportCommand;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        ReportCommand command = new ReportCommand();
        CommandLine cli = new CommandLine(command).setCaseInsensitiveEnumValuesAllowed(true);
        if (args.length == 0) {
            cli.usage(System.out);
            return;
        }
        System.exit(cli.execute(args));
    }
}
