package ru.mirea.sqlreport.execution;

import ru.mirea.sqlreport.ast.Query;
import ru.mirea.sqlreport.model.ResultTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QueryExecutor {
    public ResultTable execute(Query query, ResultTable source) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (String header : source.headers()) {
            indexes.put(header, indexes.size());
        }
        List<Query.SelectItem> select = expand(query.columns(), source.headers());
        List<String> labels = select.stream().map(Query.SelectItem::label).toList();
        boolean groupedQuery = !query.groupBy().isEmpty();
        for (Query.SelectItem item : select) {
            if (item.aggregate() != null) {
                groupedQuery = true;
                break;
            }
        }
        validate(query, select, labels, indexes.keySet(), groupedQuery);

        // WHERE выполняется до группировки и вычисления агрегатов.
        List<List<String>> matchingRows = new ArrayList<>();
        for (List<String> row : source.rows()) {
            if (query.filter() == null || query.filter().test(column -> row.get(indexes.get(column)))) {
                matchingRows.add(row);
            }
        }

        List<List<String>> result = new ArrayList<>();
        if (groupedQuery) {
            for (List<List<String>> group : group(matchingRows, query.groupBy(), indexes)) {
                List<String> resultRow = new ArrayList<>();
                for (Query.SelectItem item : select) {
                    resultRow.add(projectGroup(item, group, indexes));
                }
                result.add(resultRow);
            }
        } else {
            for (List<String> row : matchingRows) {
                List<String> resultRow = new ArrayList<>();
                for (Query.SelectItem item : select) {
                    resultRow.add(row.get(indexes.get(item.column())));
                }
                result.add(resultRow);
            }
        }
        if (query.orderBy() != null) {
            sort(result, labels.indexOf(query.orderBy().label()), query.orderBy().descending());
        }
        return new ResultTable(labels, result.subList(0, Math.min(query.limit(), result.size())));
    }

    private List<Query.SelectItem> expand(List<Query.SelectItem> columns, List<String> headers) {
        if (columns.size() == 1 && wildcard(columns.get(0))) {
            List<Query.SelectItem> expanded = new ArrayList<>();
            for (String header : headers) {
                expanded.add(new Query.SelectItem(header, null, header));
            }
            return expanded;
        }
        for (Query.SelectItem item : columns) {
            if (wildcard(item)) {
                throw new IllegalArgumentException("SELECT * используется без других элементов SELECT");
            }
        }
        return columns;
    }

    private boolean wildcard(Query.SelectItem item) {
        return item.aggregate() == null && item.column() == null;
    }

    private void validate(Query query, List<Query.SelectItem> select, List<String> labels,
                          Set<String> headers, boolean groupedQuery) {
        Set<String> referenced = new LinkedHashSet<>(query.groupBy());
        for (Query.SelectItem item : select) {
            if (item.column() != null) {
                referenced.add(item.column());
            }
            if (groupedQuery && item.aggregate() == null
                    && !query.groupBy().contains(item.column())) {
                throw new IllegalArgumentException("Добавьте столбец в GROUP BY: " + item.column());
            }
        }
        if (query.filter() != null) {
            query.filter().collectColumns(referenced);
        }
        referenced.removeAll(headers);
        if (!referenced.isEmpty()) {
            throw new IllegalArgumentException("Неизвестные столбцы: " + String.join(", ", referenced));
        }
        if (query.orderBy() != null) {
            String label = query.orderBy().label();
            if (labels.indexOf(label) < 0 || labels.indexOf(label) != labels.lastIndexOf(label)) {
                throw new IllegalArgumentException("ORDER BY требует уникальное имя из SELECT: " + label);
            }
        }
    }

    private Iterable<List<List<String>>> group(List<List<String>> rows, List<String> groupBy,
                                                 Map<String, Integer> indexes) {
        if (groupBy.isEmpty()) {
            return List.of(rows);
        }
        Map<List<String>, List<List<String>>> groups = new LinkedHashMap<>();
        for (List<String> row : rows) {
            List<String> key = new ArrayList<>();
            for (String column : groupBy) {
                key.add(row.get(indexes.get(column)));
            }
            List<List<String>> matchingGroup = groups.get(key);
            if (matchingGroup == null) {
                matchingGroup = new ArrayList<>();
                groups.put(key, matchingGroup);
            }
            matchingGroup.add(row);
        }
        return groups.values();
    }

    private String projectGroup(Query.SelectItem item, List<List<String>> group, Map<String, Integer> indexes) {
        if (item.aggregate() == null) {
            return group.get(0).get(indexes.get(item.column()));
        }
        if (item.column() == null) {
            return Integer.toString(group.size());
        }
        int column = indexes.get(item.column());
        List<String> values = new ArrayList<>();
        for (List<String> row : group) {
            String value = row.get(column);
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        if (item.aggregate() == Query.Aggregate.COUNT) {
            return Integer.toString(values.size());
        }
        if (values.isEmpty()) {
            return "";
        }
        if (item.aggregate() == Query.Aggregate.MIN || item.aggregate() == Query.Aggregate.MAX) {
            return findExtreme(values, item.aggregate() == Query.Aggregate.MIN);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (String value : values) {
            BigDecimal number = decimal(value);
            if (number == null) {
                throw new IllegalArgumentException("Агрегат " + item.aggregate() + " требует числовой столбец: "
                        + item.column());
            }
            sum = sum.add(number);
        }
        BigDecimal result = item.aggregate() == Query.Aggregate.AVG
                ? sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP) : sum;
        return result.stripTrailingZeros().toPlainString();
    }

    private String findExtreme(List<String> values, boolean minimum) {
        boolean numeric = true;
        for (String value : values) {
            if (decimal(value) == null) {
                numeric = false;
                break;
            }
        }
        String best = values.get(0);
        for (int index = 1; index < values.size(); index++) {
            String value = values.get(index);
            int comparison = compare(value, best, numeric);
            if ((minimum && comparison < 0) || (!minimum && comparison > 0)) {
                best = value;
            }
        }
        return best;
    }

    private void sort(List<List<String>> rows, int index, boolean descending) {
        boolean numeric = isNumericColumn(rows, index);
        rows.sort((left, right) -> {
            String a = left.get(index);
            String b = right.get(index);
            if (a.isEmpty() || b.isEmpty()) {
                return Boolean.compare(a.isEmpty(), b.isEmpty());
            }
            return descending ? compare(b, a, numeric) : compare(a, b, numeric);
        });
    }

    private boolean isNumericColumn(List<List<String>> rows, int index) {
        for (List<String> row : rows) {
            String value = row.get(index);
            if (!value.isEmpty() && decimal(value) == null) {
                return false;
            }
        }
        return true;
    }

    private int compare(String left, String right, boolean numeric) {
        return numeric ? new BigDecimal(left).compareTo(new BigDecimal(right)) : left.compareTo(right);
    }

    private BigDecimal decimal(String text) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
