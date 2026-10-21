"""Findet Kotlin-String-Literale (mit Templates, verschachtelten Ausdrücken) und Kommentaren."""


def scan(src):
    """Liefert Liste von (start, end, parts) für "..."-Literale außerhalb von Kommentaren.
    parts: Liste von ('text', s) / ('expr', code) Elementen."""
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if src.startswith('//', i):
            j = src.find('\n', i)
            i = n if j < 0 else j
            continue
        if src.startswith('/*', i):
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
            continue
        if src.startswith('"""', i):
            j = src.find('"""', i + 3)
            i = n if j < 0 else j + 3
            continue
        if c == "'":
            # Zeichen-Literal
            if src[i + 1] == '\\':
                j = src.find("'", i + 2)
            else:
                j = i + 2
            i = j + 1
            continue
        if c == '"':
            start = i
            parts, end = parse_string(src, i)
            out.append((start, end, parts))
            i = end
            continue
        i += 1
    return out


def parse_string(src, i):
    """src[i] == '"'. Gibt (parts, index nach schließendem ") zurück."""
    assert src[i] == '"'
    i += 1
    parts = []
    buf = []
    while True:
        c = src[i]
        if c == '\\':
            buf.append(src[i:i + 2])
            i += 2
            continue
        if c == '"':
            if buf:
                parts.append(('text', ''.join(buf)))
            return parts, i + 1
        if c == '$' and src[i + 1] == '{':
            if buf:
                parts.append(('text', ''.join(buf)))
                buf = []
            j = skip_expr(src, i + 2)
            parts.append(('expr', src[i + 2:j]))
            i = j + 1
            continue
        if c == '$' and (src[i + 1].isalpha() or src[i + 1] == '_'):
            if buf:
                parts.append(('text', ''.join(buf)))
                buf = []
            j = i + 1
            while j < len(src) and (src[j].isalnum() or src[j] == '_'):
                j += 1
            parts.append(('expr', src[i + 1:j]))
            i = j
            continue
        buf.append(c)
        i += 1


def skip_expr(src, i):
    """Ab i (nach '${'): Index der passenden '}'."""
    depth = 1
    while True:
        c = src[i]
        if c == '"':
            _, i = parse_string(src, i)
            continue
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
