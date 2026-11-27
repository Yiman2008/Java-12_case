package ru.mirea.sqlreport.model;

import java.util.ArrayList;
import java.util.List;

public record ResultTable(List<String> headers, List<List<String>> rows) {
    public ResultTable {
        headers = List.copyOf(headers);
        List<List<String>> copiedRows = new ArrayList<>();
        for (List<String> row : rows) {
            copiedRows.add(List.copyOf(row));
        }
        rows = List.copyOf(copiedRows);
    }
}
