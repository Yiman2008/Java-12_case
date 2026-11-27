package ru.mirea.sqlreport.output;

import ru.mirea.sqlreport.model.ResultTable;

@FunctionalInterface
public interface ReportFormatter {
    String format(ResultTable table);
}
