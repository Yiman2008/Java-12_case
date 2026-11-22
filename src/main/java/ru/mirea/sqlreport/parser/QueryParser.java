package ru.mirea.sqlreport.parser;

import ru.mirea.sqlreport.ast.Predicate;
import ru.mirea.sqlreport.ast.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QueryParser {
    private static final Pattern TOKEN = Pattern.compile(
            "\\s+|'(?:[^']|'')*'|\"(?:[^\"]|\"\")*\"|>=|<=|<>|!=|[(),;*=<>]|[^\\s'\"(),;*=<>!]+");
    private static final Pattern NUMBER = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)");
    private static final Map<String, Predicate.Operator> OPERATORS = Map.of(
            "=", Predicate.Operator.EQ, "!=", Predicate.Operator.NE, "<>", Predicate.Operator.NE,
            "<", Predicate.Operator.LT, "<=", Predicate.Operator.LE,
            ">", Predicate.Operator.GT, ">=", Predicate.Operator.GE);

    public Query parse(String sql) {
        return new Cursor(sql).parseQuery();
    }

    private record Token(String text, int offset) {}

    private static final class Cursor {
        private final String sql;
        private final List<Token> tokens = new ArrayList<>();
        private int next;

        Cursor(String sql) {
            this.sql = sql;
            Matcher matcher = TOKEN.matcher(sql);
            int end = 0;
            while (matcher.find()) {
                if (matcher.start() != end) {
                    throw error(end, "неизвестный символ");
                }
                if (!matcher.group().isBlank()) {
                    tokens.add(new Token(matcher.group(), matcher.start()));
                }
                end = matcher.end();
            }
            if (end != sql.length()) {
                throw error(end, "неизвестный символ");
            }
            tokens.add(new Token("", end));
        }

        Query parseQuery() {
            require("SELECT");
            List<Query.SelectItem> columns = new ArrayList<>();
            do {
                columns.add(selectItem());
            } while (take(","));
            require("FROM");
            String source = identifier();
            Predicate filter = take("WHERE") ? disjunction() : null;
            List<String> groups = new ArrayList<>();
            if (take("GROUP")) {
                require("BY");
                do {
                    groups.add(identifier());
                } while (take(","));
            }
            Query.Ordering ordering = null;
            if (take("ORDER")) {
                require("BY");
                String label = identifier();
                boolean descending = take("DESC");
                if (!descending) {
                    take("ASC");
                }
                ordering = new Query.Ordering(label, descending);
            }
            int limit = Integer.MAX_VALUE;
            if (take("LIMIT")) {
                Token token = current();
                try {
                    limit = Integer.parseInt(token.text());
                    if (limit < 0) {
                        throw error(token.offset(), "LIMIT требует неотрицательное целое");
                    }
                } catch (NumberFormatException e) {
                    throw error(token.offset(), "LIMIT требует неотрицательное целое");
                }
                next++;
            }
            take(";");
            if (!current().text().isEmpty()) {
                throw error(current().offset(), "неожиданный токен " + current().text());
            }
            return new Query(source, columns, filter, groups, ordering, limit);
        }

        private Query.SelectItem selectItem() {
            if (take("*")) {
                return new Query.SelectItem(null, null, "*");
            }
            Token start = current();
            String name = identifier();
            if (!take("(")) {
                return new Query.SelectItem(name, null, take("AS") ? identifier() : name);
            }
            Query.Aggregate aggregate;
            try {
                aggregate = Query.Aggregate.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw error(start.offset(), "неизвестная агрегатная функция " + name);
            }
            String column = take("*") ? null : identifier();
            if (column == null && aggregate != Query.Aggregate.COUNT) {
                throw error(start.offset(), "звёздочка допустима только в COUNT(*)");
            }
            require(")");
            String label = take("AS") ? identifier() : name.toLowerCase(Locale.ROOT) + "(" + (column == null ? "*" : column) + ")";
            return new Query.SelectItem(column, aggregate, label);
        }

        private Predicate disjunction() {
            Predicate result = conjunction();
            while (take("OR")) {
                result = new Predicate.Or(result, conjunction());
            }
            return result;
        }

        private Predicate conjunction() {
            Predicate result = condition();
            while (take("AND")) {
                result = new Predicate.And(result, condition());
            }
            return result;
        }

        private Predicate condition() {
            if (take("(")) {
                Predicate nested = disjunction();
                require(")");
                return nested;
            }
            String column = identifier();
            Token operator = current();
            Predicate.Operator comparison = OPERATORS.get(operator.text());
            if (comparison == null) {
                throw error(operator.offset(), "ожидался оператор сравнения");
            }
            next++;
            Token operand = current();
            String literal = operand.text();
            boolean numeric = NUMBER.matcher(literal).matches();
            if (!numeric && !literal.startsWith("'")) {
                throw error(operand.offset(), "ожидалось число или строка в одинарных кавычках");
            }
            next++;
            return new Predicate.Comparison(column, comparison,
                    numeric ? literal : unquote(literal), numeric);
        }

        private String identifier() {
            Token token = current();
            String value = token.text();
            if (value.isEmpty() || "(),;*=<>".contains(value) || OPERATORS.containsKey(value)
                    || value.startsWith("'")) {
                throw error(token.offset(), "ожидалось имя столбца или файла");
            }
            next++;
            return value.startsWith("\"") ? unquote(value) : value;
        }

        private String unquote(String value) {
            String quote = value.substring(0, 1);
            return value.substring(1, value.length() - 1).replace(quote + quote, quote);
        }

        private Token current() {
            return tokens.get(next);
        }

        private boolean take(String expected) {
            if (!current().text().equalsIgnoreCase(expected)) {
                return false;
            }
            next++;
            return true;
        }

        private void require(String expected) {
            if (!take(expected)) {
                throw error(current().offset(), "ожидалось " + expected);
            }
        }

        private IllegalArgumentException error(int offset, String detail) {
            int line = 1;
            int column = 1;
            for (int index = 0; index < offset; index++) {
                if (sql.charAt(index) == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }
            return new IllegalArgumentException("Синтаксическая ошибка (" + line + ":" + column + "): " + detail);
        }
    }
}
