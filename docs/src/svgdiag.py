"""Hand-built SVG for the use case and sequence diagrams; rendered to PNG via Chromium."""
import math, html, sys
from playwright.sync_api import sync_playwright

FONT = "Liberation Sans, DejaVu Sans, Arial, sans-serif"
E = html.escape


def tw(s, size=13):
    return len(s) * size * 0.56


# ---------------------------------------------------------------- use case
def use_case():
    W, H = 1060, 760
    out = []
    a = out.append
    a(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" font-family="{FONT}">')
    a('''<defs>
<marker id="open" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse">
<path d="M0,0 L10,5 L0,10" fill="none" stroke="#222" stroke-width="1.3"/></marker>
<marker id="tri" viewBox="0 0 12 12" refX="12" refY="6" markerWidth="12" markerHeight="12" orient="auto">
<path d="M0,0 L12,6 L0,12 z" fill="#fff" stroke="#222" stroke-width="1.2"/></marker>
</defs>''')
    a(f'<rect width="{W}" height="{H}" fill="#fff"/>')
    # system boundary
    bx, by, bw, bh = 250, 20, 560, 720
    a(f'<rect x="{bx}" y="{by}" width="{bw}" height="{bh}" fill="#fbfbf7" stroke="#222" stroke-width="1.5"/>')
    a(f'<text x="{bx+bw-16}" y="{by+28}" text-anchor="end" font-size="15" font-weight="bold">Система «SQLReport»</text>')

    uc = {
        "func": ("Зарегистрировать\nагрегатную функцию", 430, 75),
        "api": ("Выполнить запрос\nчерез Java API", 430, 175),
        "run": ("Выполнить SQL-запрос\nк CSV-файлам", 430, 285),
        "script": ("Выполнить скрипт\nзапросов из файла", 430, 390),
        "schema": ("Просмотреть схему\nCSV-файла (DESCRIBE)", 430, 495),
        "help": ("Получить справку\nпо синтаксису", 430, 595),
        "fmt": ("Выбрать формат вывода\n(таблица/CSV/HTML/MD)", 430, 690),
        "parse": ("Разобрать и проверить\nзапрос", 685, 185),
        "err": ("Показать сообщение\nоб ошибке", 685, 300),
        "load": ("Загрузить\nCSV-файл", 685, 495),
        "save": ("Сохранить отчёт\nв файл", 685, 690),
    }
    geom = {}
    for k, (t, x, y) in uc.items():
        lines = t.split("\n")
        rx = max(tw(l) for l in lines) / 2 + 26
        ry = 30
        geom[k] = (x, y, rx, ry)
        a(f'<ellipse cx="{x}" cy="{y}" rx="{rx}" ry="{ry}" fill="#fff" stroke="#222" stroke-width="1.4"/>')
        for i, l in enumerate(lines):
            yy = y + (i - (len(lines) - 1) / 2) * 16 + 4.5
            a(f'<text x="{x}" y="{yy}" text-anchor="middle" font-size="13">{E(l)}</text>')

    def actor(x, y, name, stereo=None):
        a(f'<g stroke="#222" stroke-width="1.6" fill="none">'
          f'<circle cx="{x}" cy="{y-38}" r="11" fill="#fff"/>'
          f'<line x1="{x}" y1="{y-27}" x2="{x}" y2="{y+5}"/>'
          f'<line x1="{x-18}" y1="{y-16}" x2="{x+18}" y2="{y-16}"/>'
          f'<line x1="{x}" y1="{y+5}" x2="{x-14}" y2="{y+28}"/>'
          f'<line x1="{x}" y1="{y+5}" x2="{x+14}" y2="{y+28}"/></g>')
        ly = y + 46
        if stereo:
            a(f'<text x="{x}" y="{ly}" text-anchor="middle" font-size="12" font-style="italic">«{E(stereo)}»</text>')
            ly += 16
        for i, l in enumerate(name.split("\n")):
            a(f'<text x="{x}" y="{ly + i*16}" text-anchor="middle" font-size="13" font-weight="bold">{E(l)}</text>')
        return (x, y - 10)

    actors = {
        "an": actor(110, 520, "Аналитик\n(пользователь CLI)"),
        "dev": actor(110, 140, "Разработчик-\nинтегратор"),
        "fs": actor(950, 600, "Файловая\nсистема", "system"),
    }

    def bpt(k, tx, ty):
        x, y, rx, ry = geom[k]
        dx, dy = tx - x, ty - y
        t = 1 / math.sqrt((dx / rx) ** 2 + (dy / ry) ** 2)
        return x + dx * t, y + dy * t

    def assoc(act, k, toward=None):
        ax, ay = actors[act]
        x, y, rx, ry = geom[k]
        px, py = bpt(k, ax, ay)
        off = 22 if ax < x else -22
        a(f'<line x1="{ax+off}" y1="{ay}" x2="{px}" y2="{py}" stroke="#222" stroke-width="1.3"/>')

    for k in ["script", "run", "schema", "help", "fmt"]:
        assoc("an", k)
    for k in ["api", "func"]:
        assoc("dev", k)
    for k in ["load", "save"]:
        assoc("fs", k)

    def dep(src, dst, label):
        sx, sy, _, _ = geom[src]
        dx, dy, _, _ = geom[dst]
        p1 = bpt(src, dx, dy)
        p2 = bpt(dst, sx, sy)
        a(f'<line x1="{p1[0]:.1f}" y1="{p1[1]:.1f}" x2="{p2[0]:.1f}" y2="{p2[1]:.1f}" stroke="#222" '
          f'stroke-width="1.2" stroke-dasharray="6,4" marker-end="url(#open)"/>')
        mx, my = (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2
        w = tw(label, 11) + 6
        a(f'<rect x="{mx-w/2}" y="{my-9}" width="{w}" height="14" fill="#fbfbf7"/>')
        a(f'<text x="{mx}" y="{my+2}" text-anchor="middle" font-size="11" font-style="italic">{E(label)}</text>')

    dep("script", "run", "«include»")
    dep("run", "parse", "«include»")
    dep("run", "load", "«include»")
    dep("schema", "load", "«include»")
    dep("err", "run", "«extend»")
    dep("save", "fmt", "«extend»")
    dep("api", "run", "«include»")
    a('</svg>')
    return "\n".join(out), W, H


# ---------------------------------------------------------------- sequence
def sequence():
    parts = [
        ("actor", "Аналитик"),
        ("obj", ":ReportCli"),
        ("obj", ":ReportEngine"),
        ("obj", ":Lexer"),
        ("obj", ":Parser"),
        ("obj", ":QueryPlanner"),
        ("obj", ":TableCatalog"),
        ("obj", ":CsvReader"),
        ("obj", "root:Operator"),
        ("obj", ":ConsoleTableFormatter"),
    ]
    # message list: (from, to, text, kind)  kind: call | ret | self | create
    # fragments: ('frag', label, guard, start_marker) / ('end',) / ('else', guard)
    M = [
        (0, 1, "ввод запроса (Enter)", "call"),
        (1, 2, "execute(sql)", "call"),
        (2, 3, "tokenize(sql)", "call"),
        (3, 2, "tokens: List<Token>", "ret"),
        (2, 4, "parse(tokens)", "call"),
        ("frag", "alt", "[синтаксическая ошибка]"),
        (4, 2, "throw SyntaxException(строка, столбец)", "ret"),
        (2, 1, "throw SyntaxException", "ret"),
        (1, 0, "сообщение об ошибке с позицией", "ret"),
        ("else", "[запрос корректен]"),
        (4, 2, "stmt: SelectStatement", "ret"),
        (2, 5, "plan(stmt, catalog)", "call"),
        (5, 6, "resolve(\"students.csv\")", "call"),
        ("frag", "opt", "[таблицы нет в кеше]"),
        (6, 7, "read(path, options)", "call"),
        (7, 7, "inferTypes()", "self"),
        (7, 6, "table: Table", "ret"),
        ("end",),
        (6, 5, "table: Table", "ret"),
        (5, 5, "validate(stmt, schema)", "self"),
        (5, 2, "root: Operator (Scan→Filter→Aggregate→Sort→Project)", "ret"),
        (2, 8, "open()", "call"),
        ("frag", "loop", "[пока next() ≠ null]"),
        (2, 8, "next()", "call"),
        (8, 2, "row: Row", "ret"),
        (2, 2, "result.add(row)", "self"),
        ("end",),
        (2, 8, "close()", "call"),
        (2, 1, "result: ResultTable", "ret"),
        (1, 9, "format(result)", "call"),
        (9, 1, "text: String", "ret"),
        (1, 0, "таблица отчёта в консоли", "ret"),
        ("end",),
    ]
    gap = 122
    x0 = 70
    xs = [x0 + i * gap for i in range(len(parts))]
    W = xs[-1] + 100
    top = 30
    head_h = 34
    y = top + head_h + 40
    step = 34
    items = []  # computed
    stack = []
    frames = []
    for m in M:
        if m[0] == "frag":
            stack.append({"label": m[1], "guard": m[2], "y0": y - 6, "elses": []})
            y += 40
        elif m[0] == "else":
            stack[-1]["elses"].append((y - 4, m[1]))
            y += 38
        elif m[0] == "end":
            f = stack.pop()
            f["y1"] = y - 10
            f["depth"] = len(stack)
            frames.append(f)
            y += 14
        else:
            items.append((m, y))
            y += step + (14 if m[3] == "self" else 0)
    H = y + 40
    out = []
    a = out.append
    a(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" font-family="{FONT}">')
    a('''<defs>
<marker id="fill" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="9" markerHeight="9" orient="auto">
<path d="M0,0 L10,5 L0,10 z" fill="#222"/></marker>
<marker id="open" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="9" markerHeight="9" orient="auto">
<path d="M0,0 L10,5 L0,10" fill="none" stroke="#222" stroke-width="1.3"/></marker></defs>''')
    a(f'<rect width="{W}" height="{H}" fill="#fff"/>')
    # frames first (behind)
    for f in sorted(frames, key=lambda f: f["depth"]):
        d = f["depth"]
        fx0 = 22 + d * 8
        fx1 = W - 14 - d * 8
        if f["label"] in ("opt",):
            fx0 = xs[5] - 30
            fx1 = xs[7] + 70
        if f["label"] == "loop":
            fx0 = xs[2] - 40
            fx1 = xs[8] + 64
        fill = "none"
        a(f'<rect x="{fx0}" y="{f["y0"]}" width="{fx1-fx0}" height="{f["y1"]-f["y0"]}" fill="{fill}" stroke="#555" stroke-width="1.1"/>')
        lw = tw(f["label"], 12) + 14
        a(f'<path d="M{fx0},{f["y0"]} h{lw} v12 l-7,7 h{-lw+7} z" fill="#e8ebf2" stroke="#555" stroke-width="1"/>')
        a(f'<text x="{fx0+5}" y="{f["y0"]+14}" font-size="12" font-weight="bold">{E(f["label"])}</text>')
        a(f'<text x="{fx0+lw+8}" y="{f["y0"]+14}" font-size="12" font-style="italic">{E(f["guard"])}</text>')
        for ey, g in f["elses"]:
            a(f'<line x1="{fx0}" y1="{ey}" x2="{fx1}" y2="{ey}" stroke="#555" stroke-dasharray="7,4"/>')
            a(f'<text x="{fx0+8}" y="{ey+15}" font-size="12" font-style="italic">{E(g)}</text>')
    # lifelines + heads
    for i, (kind, name) in enumerate(parts):
        x = xs[i]
        a(f'<line x1="{x}" y1="{top+head_h}" x2="{x}" y2="{H-14}" stroke="#777" stroke-dasharray="5,4"/>')
        if kind == "actor":
            a(f'<g stroke="#222" stroke-width="1.5" fill="#fff"><circle cx="{x}" cy="{top-4}" r="8"/>'
              f'<line x1="{x}" y1="{top+4}" x2="{x}" y2="{top+20}"/><line x1="{x-12}" y1="{top+10}" x2="{x+12}" y2="{top+10}"/>'
              f'<line x1="{x}" y1="{top+20}" x2="{x-9}" y2="{top+32}"/><line x1="{x}" y1="{top+20}" x2="{x+9}" y2="{top+32}"/></g>')
            a(f'<text x="{x}" y="{top+head_h+12}" text-anchor="middle" font-size="12" font-weight="bold">{E(name)}</text>')
        else:
            w = max(tw(name, 12) + 14, 88)
            a(f'<rect x="{x-w/2}" y="{top}" width="{w}" height="{head_h}" fill="#eef2f8" stroke="#222" stroke-width="1.3"/>')
            a(f'<text x="{x}" y="{top+21}" text-anchor="middle" font-size="12" text-decoration="underline">{E(name)}</text>')
    # messages
    for (fr, to, text, kind), yy in items:
        x1, x2 = xs[fr], xs[to]
        if kind == "self":
            a(f'<path d="M{x1+5},{yy} h30 v16 h-25" fill="none" stroke="#222" stroke-width="1.2" marker-end="url(#fill)"/>')
            a(f'<text x="{x1+40}" y="{yy+12}" font-size="12">{E(text)}</text>')
            continue
        sgn = 1 if x2 > x1 else -1
        dash = ' stroke-dasharray="6,4"' if kind == "ret" else ""
        mk = "open" if kind == "ret" else "fill"
        a(f'<line x1="{x1 + sgn*5}" y1="{yy}" x2="{x2 - sgn*5}" y2="{yy}" stroke="#222" stroke-width="1.2"{dash} marker-end="url(#{mk})"/>')
        mx = (x1 + x2) / 2
        w = tw(text, 12)
        # keep label inside if it's long
        a(f'<rect x="{mx-w/2-3}" y="{yy-17}" width="{w+6}" height="14" fill="#fff" fill-opacity="0.85"/>')
        a(f'<text x="{mx}" y="{yy-6}" text-anchor="middle" font-size="12">{E(text)}</text>')
    a('</svg>')
    return "\n".join(out), W, H


def render(svg, w, h, path, scale=2):
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page(viewport={"width": int(w), "height": int(h)}, device_scale_factor=scale)
        pg.set_content(f'<html><body style="margin:0">{svg}</body></html>')
        pg.screenshot(path=path, clip={"x": 0, "y": 0, "width": w, "height": h})
        b.close()


if __name__ == "__main__":
    s, w, h = use_case()
    render(s, w, h, "diagrams/use_case.png")
    s, w, h = sequence()
    render(s, w, h, "diagrams/sequence.png")
    print("ok")
