package ru.mirea.sqlreport.ast;

import java.math.BigDecimal;
import java.util.Set;
import java.util.function.Function;

public sealed interface Predicate permits Predicate.Comparison, Predicate.And, Predicate.Or {
    boolean test(Function<String, String> row);
    void collectColumns(Set<String> columns);

    record Comparison(String column, Operator operator, String literal, boolean numeric)
            implements Predicate {
        @Override
        public boolean test(Function<String, String> row) {
            String value = row.apply(column);
            if (value.isEmpty()) {
                return false;
            }
            int result;
            if (numeric) {
                try {
                    result = new BigDecimal(value).compareTo(new BigDecimal(literal));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Ожидалось число в столбце " + column, e);
                }
            } else {
                result = value.compareTo(literal);
            }
            return operator.matches(result);
        }

        @Override
        public void collectColumns(Set<String> columns) {
            columns.add(column);
        }
    }

    record And(Predicate left, Predicate right) implements Predicate {
        @Override
        public boolean test(Function<String, String> row) {
            return left.test(row) && right.test(row);
        }

        @Override
        public void collectColumns(Set<String> columns) {
            left.collectColumns(columns);
            right.collectColumns(columns);
        }
    }

    record Or(Predicate left, Predicate right) implements Predicate {
        @Override
        public boolean test(Function<String, String> row) {
            return left.test(row) || right.test(row);
        }

        @Override
        public void collectColumns(Set<String> columns) {
            left.collectColumns(columns);
            right.collectColumns(columns);
        }
    }

    enum Operator {
        EQ, NE, LT, LE, GT, GE;

        boolean matches(int result) {
            return switch (this) {
                case EQ -> result == 0;
                case NE -> result != 0;
                case LT -> result < 0;
                case LE -> result <= 0;
                case GT -> result > 0;
                case GE -> result >= 0;
            };
        }
    }
}
