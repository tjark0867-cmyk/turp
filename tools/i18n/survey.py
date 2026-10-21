import glob, re, sys, json
sys.path.insert(0, '.')
from kotlin_lits import scan
ROOT = '/home/claude/turp-github/app/src/main/java/app/turp'
SKIP_FILES = {'InterestCatalog.kt', 'I18n.kt'}
TECH_CALLS = ('whereEqualTo(', 'whereArrayContainsAny(', 'orderBy(', 'collection(', 'document(', 'getString(', 'getLong(',
              'getBoolean(', 'getInt(', 'getStringExtra(', 'putExtra(', 'putString(', 'putInt(', 'putBoolean(', 'callFunction(',
              'getHttpsCallable(', 'Log.w(', 'Log.d(', 'Log.e(', 'Log.i(', 'getSharedPreferences(', 'getIdentifier(',
              'navigate(', 'composable(', 'Uri.parse(', 'Color(', 'parseColor(', 'ofPattern(', 'readArray(', 'open(',
              'startsWith(', 'endsWith(', 'contains(', 'substringAfter(', 'substringBefore(', 'split(', 'replace(', 'Regex(',
              'child(', 'getReference(', 'reference(', 'update(', 'FieldPath', 'arrayOf(', 'setOf(', 'hasExtra(', 'createTempFile(',
              'MediaType', 'setType(', 'type = ', 'Intent(', 'fromParts(', 'getColumnIndex', 'MimeTypeMap', 'Suppress(', 's(', 'n(', 'b(', 'list(', 'ts(', 'i(',
              'ACTION_', 'NotificationChannel(', 'firstOrNull { it.code', 'forLanguageTag(', 'key = ', 'label = ')


def letters(t):
    return re.search(r'[A-Za-zÄÖÜäöüß]', t)


def technical(text):
    t = text
    if not letters(t):
        return True
    if re.fullmatch(r'[a-z0-9_.:/\-*#%]+', t):
        return True
    if re.fullmatch(r'[a-z]+(?:[A-Z][a-z0-9]*)+', t):
        return True
    if re.fullmatch(r'[A-Z0-9_]+', t) and len(t) > 2:
        return True
    if t.startswith(('http', 'android.', 'com.', 'app.', 'image/', 'video/', 'audio/', 'gs://', 'content://')) or '://' in t:
        return True
    if re.fullmatch(r'[dMyHhmsS.:/\- ]+', t):
        return True
    return False


def classify(src, start, parts):
    line_start = src.rfind('\n', 0, start) + 1
    prefix = src[line_start:start]
    if 'const val' in prefix or prefix.strip().startswith('@'):
        return 'skip-const'
    if re.search(r'\bt\(\s*$', prefix) or re.search(r'tIn\([^,]*,\s*$', prefix):
        return 'already'
    text = ''.join(p[1] if p[0] == 'text' else '{}' for p in parts)
    if technical(text.replace('{}', '')) and not re.search(r'[A-ZÄÖÜ][a-zäöüß]+ [a-zäöüß]', text):
        return 'tech'
    tail = prefix[-40:]
    for c in TECH_CALLS:
        if tail.rstrip().endswith(c.rstrip()) or tail.endswith(c):
            return 'techcall:' + c
    # Map-Schlüssel: "x" to …
    after = src[src.find('"', start + 1):start + 400]
    return 'wrap'


res = {}
for f in sorted(glob.glob(ROOT + '/**/*.kt', recursive=True)):
    if f.split('/')[-1] in SKIP_FILES:
        continue
    src = open(f).read()
    for s, e, parts in scan(src):
        kind = classify(src, s, parts)
        text = src[s:e]
        res.setdefault(kind.split(':')[0], []).append((f.split('/')[-1], text, kind))
for k, v in res.items():
    print(k, len(v))
json.dump(res, open('survey.json', 'w'), ensure_ascii=False, indent=0)
