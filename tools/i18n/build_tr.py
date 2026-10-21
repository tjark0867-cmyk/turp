"""Baut assets/i18n/<lang>.json aus tr/<lang>_*.txt (Zeilen: index<TAB>Übersetzung) und prüft Platzhalter."""
import glob, json, re, sys
ROOT = '/home/claude/turp-github'
src = json.load(open(ROOT + '/app/src/main/assets/i18n/source.json'))
push_keys = json.load(open('push_keys.json'))
langs = sorted({f.split('/')[-1].split('_')[0] for f in glob.glob('tr/*.txt')})
push = {}
ok = True
for lang in langs:
    arr = [''] * len(src)
    for f in sorted(glob.glob(f'tr/{lang}_*.txt')):
        for ln, line in enumerate(open(f, encoding='utf-8'), 1):
            line = line.rstrip('\n')
            if not line.strip():
                continue
            if '|' not in line:
                print(lang, f, ln, 'kein |:', line[:60]); ok = False; continue
            i, txt = line.split('|', 1)
            i = int(i)
            arr[i] = txt.replace('\\n', '\n')
    missing = [i for i, v in enumerate(arr) if not v]
    probs = []
    for i, (s, v) in enumerate(zip(src, arr)):
        if not v:
            continue
        for ph in set(re.findall(r'\{\w+\}|%0?\d*d', s)):
            if v.count(ph) != s.count(ph):
                probs.append((i, ph, v[:50]))
    print(f'{lang}: {len(src) - len(missing)}/{len(src)} übersetzt, fehlend: {missing[:12]}{"…" if len(missing) > 12 else ""}, Platzhalter-Fehler: {probs[:5]}')
    if probs:
        ok = False
    json.dump(arr, open(f'{ROOT}/app/src/main/assets/i18n/{lang}.json', 'w'), ensure_ascii=False, indent=0)
    push[lang] = {k: arr[src.index(k)] for k in push_keys if k in src and arr[src.index(k)]}
json.dump(push, open(ROOT + '/functions/push_i18n.json', 'w'), ensure_ascii=False, indent=1)
print('ok' if ok else 'FEHLER')
