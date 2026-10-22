"""UML class diagram for SQLReport, generated with Graphviz."""
import subprocess, html

E = html.escape


def cls(name, attrs=(), ops=(), stereo=None, abstract=False, compact=False):
    head = ""
    if stereo:
        head += f'<FONT POINT-SIZE="10"><I>«{E(stereo)}»</I></FONT><BR/>'
    nm = f"<I>{E(name)}</I>" if abstract else E(name)
    head += f"<B>{nm}</B>"
    rows = [f'<TR><TD BGCOLOR="#e9eef6" CELLPADDING="4">{head}</TD></TR>']
    if not compact:
        a = "<BR/>".join(E(x) for x in attrs) if attrs else " "
        o = "<BR/>".join(E(x) for x in ops) if ops else " "
        rows.append(f'<TR><TD ALIGN="LEFT" BALIGN="LEFT" CELLPADDING="4">{a}</TD></TR>')
        rows.append(f'<TR><TD ALIGN="LEFT" BALIGN="LEFT" CELLPADDING="4">{o}</TD></TR>')
    return '<<TABLE BORDER="1" CELLBORDER="1" CELLSPACING="0" COLOR="#222222">' + "".join(rows) + "</TABLE>>"


N = {
    # app
    "ReportCli": cls("ReportCli", ["- engine: ReportEngine", "- format: OutputFormat"],
                     ["+ main(args: String[]): void", "- runRepl(): void", "- runScript(path: Path): void"]),
    "ReportEngine": cls("ReportEngine", ["- catalog: TableCatalog", "- functions: FunctionRegistry", "- planner: QueryPlanner"],
                        ["+ execute(sql: String): ResultTable", "+ describe(file: String): Schema",
                         "+ registerFunction(name: String, f: Supplier)"], stereo="facade"),
    # parsing
    "Lexer": cls("Lexer", ["- source: String", "- pos: int", "- line: int", "- column: int"],
                 ["+ tokenize(sql: String): List<Token>", "- nextToken(): Token"]),
    "Token": cls("Token", ["+ type: TokenType", "+ text: String", "+ line: int", "+ column: int"], []),
    "TokenType": cls("TokenType", ["SELECT, FROM, WHERE, JOIN, ON,", "GROUP_BY, HAVING, ORDER_BY, LIMIT,",
                                   "IDENT, NUMBER, STRING, OPERATOR, EOF"], [], stereo="enumeration"),
    "Parser": cls("Parser", ["- tokens: List<Token>", "- pos: int"],
                  ["+ parse(tokens): SelectStatement", "- parseSelectList(): List<SelectItem>",
                   "- parseExpression(): Expression", "- expect(type: TokenType): Token"]),
    "QueryException": cls("QueryException", ["# line: int", "# column: int"], ["+ getMessage(): String"]),
    "SyntaxException": cls("SyntaxException", compact=True),
    "SemanticException": cls("SemanticException", compact=True),
    # AST
    "SelectStatement": cls("SelectStatement",
                           ["+ items: List<SelectItem>", "+ from: TableRef", "+ joins: List<JoinClause>",
                            "+ where: Expression", "+ groupBy: List<Expression>", "+ having: Expression",
                            "+ orderBy: List<OrderItem>", "+ limit: Integer"], []),
    "Expression": cls("Expression", [], ["+ evaluate(row: Row): Value", "+ accept(v: ExprVisitor)"],
                      stereo="interface"),
    "ColumnRef": cls("ColumnRef", compact=True),
    "Literal": cls("Literal", compact=True),
    "BinaryOp": cls("BinaryOp", compact=True),
    "FunctionCall": cls("FunctionCall", compact=True),
    # data
    "TableCatalog": cls("TableCatalog", ["- cache: Map<Path, Table>", "- baseDir: Path"],
                        ["+ resolve(name: String): Table"]),
    "CsvReader": cls("CsvReader", ["- delimiter: char", "- charset: Charset", "- hasHeader: boolean"],
                     ["+ read(path: Path): Table", "- inferTypes(raw): Schema"]),
    "Table": cls("Table", ["+ name: String", "+ schema: Schema", "+ rows: List<Row>"], []),
    "Schema": cls("Schema", ["- columns: List<Column>"], ["+ indexOf(name: String): int"]),
    "Column": cls("Column", ["+ name: String", "+ type: DataType"], []),
    "DataType": cls("DataType", ["INTEGER, DECIMAL, STRING,", "BOOLEAN, DATE, NULL"], [], stereo="enumeration"),
    "Row": cls("Row", ["- values: Value[]"], ["+ get(i: int): Value"]),
    "Value": cls("Value", ["- type: DataType", "- raw: Object"], ["+ compareTo(o: Value): int", "+ asNumber(): BigDecimal"]),
    # execution
    "QueryPlanner": cls("QueryPlanner", [], ["+ plan(s: SelectStatement, c: TableCatalog): Operator",
                                             "- validate(s, schema): void"]),
    "Operator": cls("Operator", [], ["+ open(): void", "+ next(): Row", "+ close(): void", "+ schema(): Schema"],
                    stereo="interface"),
    "ScanOperator": cls("ScanOperator", compact=True),
    "FilterOperator": cls("FilterOperator", compact=True),
    "JoinOperator": cls("JoinOperator", compact=True),
    "AggregateOperator": cls("AggregateOperator", ["- groupBy: List<Expression>", "- groups: Map<Key, Accs>"], []),
    "SortOperator": cls("SortOperator", compact=True),
    "ProjectOperator": cls("ProjectOperator", compact=True),
    "LimitOperator": cls("LimitOperator", compact=True),
    "AggregateFunction": cls("AggregateFunction", [], ["+ accumulate(v: Value): void", "+ result(): Value"],
                             stereo="interface"),
    "FunctionRegistry": cls("FunctionRegistry", ["- factories: Map<String, Supplier>"],
                            ["+ register(name, f): void", "+ create(name): AggregateFunction"]),
    "Count": cls("Count", compact=True), "Sum": cls("Sum", compact=True), "Avg": cls("Avg", compact=True),
    "Min": cls("Min", compact=True), "Max": cls("Max", compact=True),
    # output
    "ResultTable": cls("ResultTable", ["+ schema: Schema", "+ rows: List<Row>"], ["+ rowCount(): int"]),
    "ReportFormatter": cls("ReportFormatter", [], ["+ format(r: ResultTable): String"], stereo="interface"),
    "ConsoleTableFormatter": cls("ConsoleTableFormatter", compact=True),
    "CsvFormatter": cls("CsvFormatter", compact=True),
    "HtmlFormatter": cls("HtmlFormatter", compact=True),
    "MarkdownFormatter": cls("MarkdownFormatter", compact=True),
}

clusters = {
    "app": ("ru.edu.sqlreport (приложение)", ["ReportCli", "ReportEngine"]),
    "parser": ("parser", ["Lexer", "Token", "TokenType", "Parser", "QueryException", "SyntaxException", "SemanticException"]),
    "ast": ("ast", ["SelectStatement", "Expression", "ColumnRef", "Literal", "BinaryOp", "FunctionCall"]),
    "data": ("data", ["TableCatalog", "CsvReader", "Table", "Schema", "Column", "DataType", "Row", "Value"]),
    "exec": ("execution", ["QueryPlanner", "Operator", "ScanOperator", "FilterOperator", "JoinOperator",
                           "AggregateOperator", "SortOperator", "ProjectOperator", "LimitOperator",
                           "AggregateFunction", "FunctionRegistry", "Count", "Sum", "Avg", "Min", "Max"]),
    "out": ("output", ["ResultTable", "ReportFormatter", "ConsoleTableFormatter", "CsvFormatter",
                       "HtmlFormatter", "MarkdownFormatter"]),
}

edges = []
def assoc(a, b, hl="", tl=""): edges.append(f'{a} -> {b} [arrowhead=vee, headlabel="{hl}", taillabel="{tl}"];')
def comp(whole, part, hl=""): edges.append(f'{whole} -> {part} [dir=both, arrowtail=diamond, arrowhead=none, headlabel="{hl}"];')
def aggr(whole, part, hl=""): edges.append(f'{whole} -> {part} [dir=both, arrowtail=odiamond, arrowhead=none, headlabel="{hl}"];')
def real(child, iface): edges.append(f'{child} -> {iface} [arrowhead=onormal, style=dashed];')
def gen(child, parent): edges.append(f'{child} -> {parent} [arrowhead=onormal];')
def dep(a, b, l=""): edges.append(f'{a} -> {b} [arrowhead=vee, style=dashed, label="«{l}»", fontsize=10];' if l else f'{a} -> {b} [arrowhead=vee, style=dashed];')

assoc("ReportCli", "ReportEngine", "1")
assoc("ReportCli", "ReportFormatter", "1")
comp("ReportEngine", "TableCatalog", "1")
comp("ReportEngine", "FunctionRegistry", "1")
comp("ReportEngine", "QueryPlanner", "1")
dep("ReportEngine", "Lexer", "create")
dep("ReportEngine", "Parser", "create")
dep("ReportEngine", "ResultTable", "create")
dep("Lexer", "Token", "create")
assoc("Token", "TokenType", "1")
dep("Parser", "SelectStatement", "create")
dep("Parser", "SyntaxException", "throw")
gen("SyntaxException", "QueryException")
gen("SemanticException", "QueryException")
comp("SelectStatement", "Expression", "0..*")
for c in ["ColumnRef", "Literal", "BinaryOp", "FunctionCall"]: real(c, "Expression")
aggr("BinaryOp", "Expression", "2")
dep("QueryPlanner", "Operator", "create")
dep("QueryPlanner", "SemanticException", "throw")
assoc("QueryPlanner", "TableCatalog", "1")
for c in ["ScanOperator", "FilterOperator", "JoinOperator", "AggregateOperator", "SortOperator",
          "ProjectOperator", "LimitOperator"]: real(c, "Operator")
aggr("FilterOperator", "Operator", "1")
assoc("ScanOperator", "Table", "1")
comp("AggregateOperator", "AggregateFunction", "1..*")
dep("AggregateOperator", "FunctionRegistry")
for c in ["Count", "Sum", "Avg", "Min", "Max"]: real(c, "AggregateFunction")
aggr("TableCatalog", "Table", "0..*")
dep("TableCatalog", "CsvReader", "use")
dep("CsvReader", "Table", "create")
comp("Table", "Schema", "1")
comp("Table", "Row", "0..*")
comp("Schema", "Column", "1..*")
assoc("Column", "DataType", "1")
comp("Row", "Value", "1..*")
assoc("ResultTable", "Schema", "1")
comp("ResultTable", "Row", "0..*")
for c in ["ConsoleTableFormatter", "CsvFormatter", "HtmlFormatter", "MarkdownFormatter"]: real(c, "ReportFormatter")
dep("ReportFormatter", "ResultTable", "use")

import re
def build(name, keys, refs, rankdir="TB"):
    present=set(refs)
    for k in keys: present.update(clusters[k][1])
    dot = ['digraph G {', f'graph [rankdir={rankdir}, fontname="Liberation Sans", fontsize=12, nodesep=0.3, ranksep=0.5, newrank=true, pad=0.2, bgcolor=white];',
       'node [shape=plain, fontname="Liberation Sans", fontsize=11];',
       'edge [fontname="Liberation Sans", fontsize=10, color="#222222", arrowsize=0.9, labeldistance=1.6, labelangle=30];']
    for key in keys:
        label, members = clusters[key]
        dot.append(f'subgraph cluster_{key} {{ label=<<B>{E(label)}</B>>; labeljust=l; style="rounded"; color="#9aa3b2"; bgcolor="#fbfbf7";')
        for m in members: dot.append(f'  {m} [label={N[m]}];')
        dot.append('}')
    for r in refs:
        dot.append(f'{r} [label={cls(r, compact=True)}];')
    for e in edges:
        a,b=re.match(r'(\w+) -> (\w+)',e).groups()
        if a in present and b in present: dot.append(e)
    dot.append('}')
    open(f"diagrams/{name}.dot","w").write("\n".join(dot))
    subprocess.run(["dot","-Tpng","-Gdpi=200",f"diagrams/{name}.dot","-o",f"diagrams/{name}.png"],check=True)

build("classes1", ["app","parser","ast","out"], ["QueryPlanner","TableCatalog","FunctionRegistry","Row","Schema"])
build("classes2", ["exec"], ["ReportEngine","SemanticException","TableCatalog","Table"])
build("classes3", ["data"], ["ReportEngine","QueryPlanner","ScanOperator"], rankdir="LR")
print("ok")
