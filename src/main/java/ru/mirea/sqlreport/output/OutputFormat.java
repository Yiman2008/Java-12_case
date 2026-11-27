package ru.mirea.sqlreport.output;

import ru.mirea.sqlreport.model.ResultTable;

import java.util.List;

public enum OutputFormat {
    TABLE(OutputFormat::table),
    CSV(OutputFormat::csv),
    HTML(OutputFormat::html),
    MARKDOWN(OutputFormat::markdown);

    private final ReportFormatter formatter;

    OutputFormat(ReportFormatter formatter) {
        this.formatter = formatter;
    }

    public ReportFormatter formatter() {
        return formatter;
    }

    private static String table(ResultTable table) {
        int[] widths = new int[table.headers().size()];
        for (int index = 0; index < widths.length; index++) {
            widths[index] = singleLine(table.headers().get(index)).length();
        }
        for (List<String> row : table.rows()) {
            for (int index = 0; index < widths.length; index++) {
                int cellWidth = singleLine(row.get(index)).length();
                widths[index] = Math.max(widths[index], cellWidth);
            }
        }
        StringBuilder result = new StringBuilder();
        appendTableRow(result, table.headers(), widths);
        for (List<String> row : table.rows()) {
            appendTableRow(result, row, widths);
        }
        return result.toString();
    }

    private static void appendTableRow(StringBuilder output, List<String> row, int[] widths) {
        for (int index = 0; index < widths.length; index++) {
            if (index > 0) {
                output.append(" | ");
            }
            String value = singleLine(row.get(index));
            output.append(value);
            if (index + 1 < widths.length) {
                output.append(" ".repeat(widths[index] - value.length()));
            }
        }
        output.append('\n');
    }

    private static String csv(ResultTable table) {
        StringBuilder result = new StringBuilder();
        appendCsvRow(result, table.headers());
        for (List<String> row : table.rows()) {
            appendCsvRow(result, row);
        }
        return result.toString();
    }

    private static void appendCsvRow(StringBuilder output, List<String> row) {
        String separator = "";
        for (String value : row) {
            output.append(separator).append(csvCell(value));
            separator = ",";
        }
        output.append("\r\n");
    }

    private static String csvCell(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String html(ResultTable table) {
        StringBuilder result = new StringBuilder("<!doctype html>\n<html lang=\"ru\"><meta charset=\"utf-8\">"
                + "<title>SQLReport</title>\n<table>\n<thead>");
        appendHtmlRow(result, "th", table.headers());
        result.append("</thead>\n<tbody>\n");
        for (List<String> row : table.rows()) {
            appendHtmlRow(result, "td", row);
        }
        return result.append("</tbody>\n</table>\n</html>\n").toString();
    }

    private static void appendHtmlRow(StringBuilder output, String tag, List<String> row) {
        output.append("<tr>");
        for (String value : row) {
            output.append('<').append(tag).append('>').append(escapeHtml(value))
                    .append("</").append(tag).append('>');
        }
        output.append("</tr>\n");
    }

    private static String markdown(ResultTable table) {
        StringBuilder result = new StringBuilder();
        appendMarkdownRow(result, table.headers());
        result.append("|").append(" --- |".repeat(table.headers().size())).append('\n');
        for (List<String> row : table.rows()) {
            appendMarkdownRow(result, row);
        }
        return result.toString();
    }

    private static void appendMarkdownRow(StringBuilder output, List<String> row) {
        output.append("| ");
        String separator = "";
        for (String value : row) {
            String cell = escapeHtml(singleLine(value));
            cell = cell.replace("\\", "\\\\").replace("|", "\\|");
            output.append(separator).append(cell);
            separator = " | ";
        }
        output.append(" |\n");
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String singleLine(String value) {
        return value.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\r");
    }
}
