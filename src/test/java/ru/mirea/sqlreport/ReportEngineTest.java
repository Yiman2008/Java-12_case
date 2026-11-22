package ru.mirea.sqlreport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.sqlreport.model.ResultTable;
import ru.mirea.sqlreport.output.OutputFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportEngineTest {
    @TempDir
    Path temp;

    @Test
    void filtersGroupsAndSortsTheDemo() throws IOException {
        ResultTable table = new ReportEngine().execute("""
                SELECT name, AVG(grade) AS avg_grade, COUNT(*) AS n FROM examples/students.csv
                WHERE grade >= 3 GROUP BY name ORDER BY avg_grade DESC LIMIT 2;
                """);
        assertEquals(List.of("name", "avg_grade", "n"), table.headers());
        assertEquals(List.of(List.of("Anna", "4.5", "2"), List.of("Boris", "4", "2")), table.rows());
    }

    @Test
    void handlesQuotedCsvAndNestedPredicates() throws IOException {
        String source = csv("name,grade\r\n\"A, \"\"B\"\"\nC\",10\r\nD,2\r\n");
        ResultTable table = new ReportEngine().execute(
                "SELECT name FROM " + source + " WHERE (grade >= 10 OR grade = 1) AND name <> 'D'");
        assertEquals("name\r\n\"A, \"\"B\"\"\nC\"\r\n", OutputFormat.CSV.formatter().format(table));
    }

    @Test
    void rejectsAnUnknownColumnEvenForAnEmptyInput() throws IOException {
        String source = csv("name,grade\n");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new ReportEngine()
                .execute("SELECT name FROM " + source + " WHERE missing = 0"))
                .getMessage().contains("missing"));
    }

    @Test
    void countsEmptyInputAndLeavesAverageEmpty() throws IOException {
        String source = csv("name,grade\n");
        assertEquals(List.of(List.of("0", "")), new ReportEngine()
                .execute("SELECT COUNT(*), AVG(grade) FROM " + source).rows());
    }

    @Test
    void escapesHtmlCells() {
        ResultTable table = new ResultTable(List.of("x"), List.of(List.of("<b>&")));
        assertTrue(OutputFormat.HTML.formatter().format(table).contains("&lt;b&gt;&amp;"));
    }

    private String csv(String content) throws IOException {
        Path file = Files.writeString(temp.resolve("students.csv"), content);
        return "\"" + file + "\"";
    }
}
