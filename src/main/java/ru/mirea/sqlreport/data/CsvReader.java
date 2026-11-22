package ru.mirea.sqlreport.data;

import ru.mirea.sqlreport.model.ResultTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class CsvReader {
    private enum State { START, UNQUOTED, QUOTED, AFTER_QUOTE }

    public ResultTable read(Path path) throws IOException {
        String text = Files.readString(path);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<List<String>> records = parse(text);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("В CSV отсутствует заголовок: " + path);
        }
        List<String> headers = records.get(0);
        if (headers.stream().anyMatch(String::isBlank) || new HashSet<>(headers).size() != headers.size()) {
            throw new IllegalArgumentException("Имена столбцов CSV должны быть непустыми и уникальными");
        }
        for (int index = 1; index < records.size(); index++) {
            if (records.get(index).size() != headers.size()) {
                throw new IllegalArgumentException("Неверное число полей в записи CSV " + (index + 1));
            }
        }
        return new ResultTable(headers, records.subList(1, records.size()));
    }

    private List<List<String>> parse(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        State state = State.START;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (state == State.QUOTED) {
                if (character == '"') {
                    state = State.AFTER_QUOTE;
                } else {
                    field.append(character);
                }
                continue;
            }
            if (state == State.AFTER_QUOTE && character == '"') {
                field.append('"');
                state = State.QUOTED;
                continue;
            }
            if (character == ',' || character == '\r' || character == '\n') {
                record.add(field.toString());
                field.setLength(0);
                state = State.START;
                if (character != ',') {
                    records.add(record);
                    record = new ArrayList<>();
                    if (character == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                        index++;
                    }
                }
                continue;
            }
            if (character == '"' && state == State.START) {
                state = State.QUOTED;
                continue;
            }
            if (character == '"' || state == State.AFTER_QUOTE) {
                throw new IllegalArgumentException("Некорректные кавычки в записи CSV " + (records.size() + 1));
            }
            field.append(character);
            state = State.UNQUOTED;
        }
        if (state == State.QUOTED) {
            throw new IllegalArgumentException("Незакрытые кавычки в записи CSV " + (records.size() + 1));
        }
        if (state != State.START || !record.isEmpty()) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }
}
