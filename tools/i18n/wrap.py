"""Umhüllt Oberflächen-Texte in Kotlin-Dateien mit t(...). Ausgabe: geänderte Dateien + Bericht."""
import glob
import re
import sys

sys.path.insert(0, '.')
from kotlin_lits import scan

ROOT = '/home/claude/turp-github/app/src/main/java/app/turp'
SKIP_FILES = {'InterestCatalog.kt', 'I18n.kt', 'LanguageScreen.kt', 'TurpMessagingService.kt'}
SKIP_TEXT = {'kumi+', 'kumi', 'File:', 'DE', 'Europe/Berlin', '\\n', ' ', '–', 'kumi Team'}
TECH_CALLS = ['whereEqualTo', 'whereArrayContainsAny', 'orderBy', 'collection', 'document', 'getString', 'getLong',
              'getBoolean', 'getInt', 'getStringExtra', 'putExtra', 'putString', 'putInt', 'putBoolean', 'callFunction',
              'getHttpsCallable', 'getSharedPreferences', 'getIdentifier', 'navigate', 'composable', 'parse', 'Color',
              'parseColor', 'ofPattern', 'open', 'startsWith', 'endsWith', 'contains', 'substringAfter', 'substringBefore',
              'split', 'replace', 'Regex', 'child', 'update', 'hasExtra', 'createTempFile', 'setType', 'Intent',
              'Suppress', 's', 'n', 'b', 'list', 'ts', 'forLanguageTag', 'removePrefix', 'SimpleDateFormat',
              'getOrDefault', 'TimeZone', 'getTimeZone', 'ZoneId', 'of', 'Log', 'w', 'd', 'e', 'v', 'animateFloat',
              'animateFloatAsState', 'rememberInfiniteTransition', 'updateTransition', 'Crossfade', 'AnimatedContent',
              'animateColorAsState', 'animateDpAsState', 'tween']


def unescape(s):
    return (s.replace('\\n', '\n').replace('\\"', '"').replace('\\$', '$').replace('\\t', '\t')
            .replace("\\'", "'").replace('\\\\', '\\'))


def plain_text(parts):
    return ''.join(p[1] if p[0] == 'text' else '' for p in parts)


def technical(parts):
    t = unescape(plain_text(parts))
    if t in SKIP_TEXT or unescape(t).strip() in SKIP_TEXT:
        return True
    if not re.search(r'[A-Za-zÄÖÜäöüß]', t):
        return True
    has_space = ' ' in t.strip()
    if not has_space:
        if '/' in t or '?' in t or '=' in t or '.json' in t or '.jpg' in t or '.m4a' in t:
            return True
        if re.fullmatch(r'[a-z0-9_.:/\-*#%]+', t):
            return True
        if re.fullmatch(r'[a-z]+(?:[A-Z][a-z0-9]*)+', t):
            return True
        if re.fullmatch(r'[A-Z0-9_]+', t):
            return True
    if t.startswith('#') or t.startswith(('http', 'android.', 'com.', 'app.')) or '://' in t:
        return True
    if re.fullmatch(r'[dMyHhmsS.:/\- ]+', t):
        return True
    return False


def context_skip(code, start):
    ls = code.rfind('\n', 0, start) + 1
    prefix = code[ls:start]
    if 'const val' in prefix or prefix.lstrip().startswith('@'):
        return 'const'
    if re.search(r'Log\.[a-z]\(', prefix) or 'logger' in prefix:
        return 'log'
    if re.search(r'(?<![A-Za-z0-9_])t\(\s*$', prefix) or re.search(r'tIn\([^()]*,\s*$', prefix):
        return 'already'
    m = re.search(r'([A-Za-z_][A-Za-z0-9_]*)\s*\(\s*(?:[^()]*,\s*)?$', prefix)
    if m and m.group(1) in TECH_CALLS:
        return 'call:' + m.group(1)
    if re.search(r'label\s*=\s*$', prefix):
        return 'label'
    return None


def render_t(parts, transform):
    text = []
    args = []
    for kind, val in parts:
        if kind == 'text':
            text.append(val.replace('{', '\\{').replace('}', '\\}') if False else val)
        else:
            text.append('{%d}' % len(args))
            args.append(transform(val))
    lit = '"' + ''.join(text) + '"'
    return 't(' + lit + (', ' + ', '.join(args) if args else '') + ')'


report = []


def transform(code, fname='', depth=0):
    lits = scan(code)
    # Zusammenhängende "a" + "b" Ketten zu einem Literal verbinden
    groups = []
    for lit in lits:
        if groups:
            prev = groups[-1]
            between = code[prev[-1][1]:lit[0]]
            if re.fullmatch(r'\s*\+\s*', between):
                prev.append(lit)
                continue
        groups.append([lit])
    out = code
    for g in reversed(groups):
        start, end = g[0][0], g[-1][1]
        parts = []
        for _, _, p in g:
            parts.extend(p)
        # benachbarte Textteile zusammenführen
        merged = []
        for kind, val in parts:
            if merged and merged[-1][0] == 'text' and kind == 'text':
                merged[-1] = ('text', merged[-1][1] + val)
            else:
                merged.append((kind, val))
        skip = context_skip(code, start)
        if skip or technical(merged):
            # Ausdrücke in Templates trotzdem bearbeiten (z. B. ${if (x) "Tag" else "Tage"})
            if any(k == 'expr' for k, _ in merged) and not skip == 'already':
                new = '"' + ''.join(v if k == 'text' else '${' + transform(v, fname, depth + 1) + '}' for k, v in merged) + '"'
                if len(g) == 1 and new != code[start:end]:
                    out = out[:start] + new + out[end:]
            continue
        new = render_t(merged, lambda e: transform(e, fname, depth + 1))
        report.append((fname, unescape(plain_text(merged))[:60], new[:90]))
        out = out[:start] + new + out[end:]
    return out


def main():
    global report
    changed = []
    for f in sorted(glob.glob(ROOT + '/**/*.kt', recursive=True)):
        name = f.split('/')[-1]
        if name in SKIP_FILES:
            continue
        src = open(f).read()
        new = transform(src, name)
        if new != src:
            pkg = re.search(r'^package ([\w.]+)', new, re.M).group(1)
            if pkg != 'app.turp.data' and 'import app.turp.data.t\n' not in new:
                new = re.sub(r'^(package [\w.]+\n)', r'\1\nimport app.turp.data.t\n', new, count=1, flags=re.M)
            if '--dry' not in sys.argv:
                open(f, 'w').write(new)
            changed.append(name)
    print('files', len(changed), 'wrapped', len(report))
    with open('wrap_report.txt', 'w') as fh:
        for r in report:
            fh.write(' | '.join(r) + '\n')


if __name__ == "__main__":
    main()
