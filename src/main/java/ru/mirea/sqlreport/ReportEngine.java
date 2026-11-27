package ru.mirea.sqlreport;

import ru.mirea.sqlreport.ast.Query;
import ru.mirea.sqlreport.data.CsvReader;
import ru.mirea.sqlreport.execution.QueryExecutor;
import ru.mirea.sqlreport.model.ResultTable;
import ru.mirea.sqlreport.parser.QueryParser;

import java.io.IOException;
import java.nio.file.Path;

public final class ReportEngine {
    private final QueryParser parser = new QueryParser();
    private final CsvReader reader = new CsvReader();
    private final QueryExecutor executor = new QueryExecutor();

    public ResultTable execute(String sql) throws IOException {
        Query query = parser.parse(sql);
        return executor.execute(query, reader.read(Path.of(query.source())));
    }
}
