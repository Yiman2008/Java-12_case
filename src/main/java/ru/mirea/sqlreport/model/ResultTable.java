package ru.mirea.sqlreport.model;

import java.util.List;

public record ResultTable(List<String> headers, List<List<String>> rows) {
    public ResultTable {
        headers = List.copyOf(headers);
        rows = rows.stream().map(List::copyOf).toList();
    }
}
