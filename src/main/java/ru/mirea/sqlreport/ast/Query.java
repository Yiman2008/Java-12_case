package ru.mirea.sqlreport.ast;

import java.util.List;

public record Query(String source, List<SelectItem> columns, Predicate filter,
                    List<String> groupBy, Ordering orderBy, int limit) {
    public Query {
        columns = List.copyOf(columns);
        groupBy = List.copyOf(groupBy);
    }

    public enum Aggregate { COUNT, SUM, AVG, MIN, MAX }

    public record SelectItem(String column, Aggregate aggregate, String label) {}

    public record Ordering(String label, boolean descending) {}
}
