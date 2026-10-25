import glob, json, re, sys
sys.path.insert(0, '.')
from kotlin_lits import parse_string
from wrap import unescape
ROOT = '/home/claude/turp-github'
PROPER = set("""Minecraft|Taylor Swift|One Piece|Fortnite|Billie Eilish|Naruto|Roblox|BTS|Demon Slayer|Valorant|Blackpink|Jujutsu Kaisen|League of Legends|Stray Kids|Attack on Titan|Genshin Impact|Ariana Grande|My Hero Academia|GTA|Sabrina Carpenter|Haikyuu|Call of Duty|Bad Bunny|Spy x Family|Clash Royale|Travis Scott|Studio Ghibli|Brawl Stars|Drake|Dragon Ball|Pokémon|The Weeknd|Harry Potter|Mario Kart|Dua Lipa|Marvel|Zelda|Olivia Rodrigo|Star Wars|Among Us|Apache 207|Disney|Ski Aggu|Stranger Things|EA FC|Wednesday|Rocket League|Overwatch|Squid Game|Stardew Valley|Animal Crossing|FNAF|LGBTQ+|K-Pop|BTS""".split('|'))
import os
out = []
seen = set()
# Bisherige Reihenfolge behalten (Übersetzungen hängen an der Position), Neues hinten anhängen
_prev = ROOT + '/app/src/main/assets/i18n/source.json'
if os.path.exists(_prev):
    for _s in json.load(open(_prev)):
        seen.add(_s)
        out.append((_s, 'bestehend'))


def add(s, ctx):
    if s and s not in seen:
        seen.add(s)
        out.append((s, ctx))


for f in sorted(glob.glob(ROOT + '/app/src/main/java/**/*.kt', recursive=True)):
    name = f.split('/')[-1]
    src = open(f).read()
    for m in re.finditer(r'(?<![A-Za-z0-9_])(?:t\(|tIn\([^,()]+,\s*)"', src):
        i = m.end() - 1
        parts, _ = parse_string(src, i)
        if any(k == 'expr' for k, _ in parts):
            print('EXPR LEFT', name, parts, file=sys.stderr)
        add(unescape(''.join(v for k, v in parts if k == 'text')), name)
cat = open(ROOT + '/app/src/main/java/app/turp/ui/components/InterestCatalog.kt').read()
for c in re.findall(r'^\s+[A-Z]+\("([^"]+)", ', cat, re.M):
    add(c, 'Interessen-Kategorie')
for l in re.findall(r'InterestDef\("[^"]*", "([^"]*)",', cat):
    if l not in PROPER:
        add(l, 'Interesse (Hobby/Thema)')
mn = open(ROOT + '/app/src/main/java/app/turp/ui/screens/MainNav.kt').read()
for l in re.findall(r'\n    (?:Lounge|Discover|Requests|Chats|Profile)\("([^"]+)"', mn):
    add(l, 'Tab')
ut = open(ROOT + '/app/src/main/java/app/turp/ui/components/Util.kt').read()
blk = ut[ut.index('val REPORT_REASONS'):]
for l in re.findall(r'"([^"]+)"', blk[:blk.index(')\n')]):
    add(l, 'Meldegrund')
# Server-Meldungen
js = open(ROOT + '/functions/index.js').read()
for m in re.finditer(r'HttpsError\(\s*"[a-z-]+",\s*(`[^`]*`|"[^"]*")', js):
    lit = m.group(1)[1:-1]
    k = [0]
    def rep(_):
        r = '{%d}' % k[0]; k[0] += 1; return r
    lit = re.sub(r'\$\{[^}]*\}', rep, lit)
    add(lit, 'Server-Fehlermeldung')
PUSH = ["Neuer Brief ✉️", "{name} hat dir einen Brief geschrieben. Lies ihn und entscheide, ob ihr Freunde werdet.", "Ihr seid jetzt Freunde 🎉", "{name} hat deine Anfrage erwidert. Sagt Hallo!", "Neue Freundschaftsanfrage 👋",
        "{name} möchte mit dir befreundet sein.", "Anfrage angenommen 🎉", "{name} hat deine Anfrage angenommen. Sagt Hallo!",
        "Happy Birthday! 🎂", "Du bist jetzt 18 und wechselst in die Erwachsenen-Community von kumi."]
for p in PUSH:
    add(p, 'Push-Benachrichtigung')
json.dump(PUSH + ["📷 Foto", "📷 Foto (blockiert)", "🎤 Sprachnachricht"], open('push_keys.json', 'w'), ensure_ascii=False)
json.dump([s for s, _ in out], open(ROOT + '/app/src/main/assets/i18n/source.json', 'w'), ensure_ascii=False, indent=0)
json.dump(out, open('source_ctx.json', 'w'), ensure_ascii=False, indent=0)
print(len(out))
