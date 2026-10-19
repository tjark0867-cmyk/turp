"""Liest den Dienstkonto-Schlüssel aus dem Secret, repariert typische
Kopierfehler (Leerzeichen, Code-Blöcke, echte Zeilenumbrüche) und speichert
ihn als Datei. Gibt nie den Schlüssel selbst aus."""
import json
import os
import re
import sys

raw = os.environ.get("SA", "")
out = sys.argv[1]
text = raw.strip().lstrip("﻿")
text = re.sub(r"^```[a-zA-Z]*\s*", "", text)
text = re.sub(r"\s*```$", "", text).strip()
info = f"Länge {len(raw)} Zeichen, erstes Zeichen {raw.strip()[:1]!r}, letztes Zeichen {raw.strip()[-1:]!r}"

# Beim Kopieren am Handy fehlt oft die erste oder letzte Klammer
if text.startswith('"'):
    text = "{" + text
if text.startswith("{") and not text.endswith("}"):
    text = text.rstrip(",") + "}"

start, end = text.find("{"), text.rfind("}")
if start == -1 or end <= start:
    print(f"::error title=Secret ungültig::FIREBASE_SERVICE_ACCOUNT enthält kein JSON ({info}). "
          "Bitte den kompletten Inhalt der JSON-Datei einfügen – er beginnt mit { und endet mit }.")
    sys.exit(1)

try:
    data = json.loads(text[start:end + 1], strict=False)
except Exception as e:  # noqa: BLE001
    print(f"::error title=Secret unvollständig::Der Inhalt ist kein vollständiges JSON ({info}; {e}). "
          "Vermutlich wurde beim Kopieren etwas abgeschnitten.")
    sys.exit(1)

missing = [k for k in ("type", "project_id", "private_key", "client_email") if not data.get(k)]
if missing:
    print(f"::error title=Secret unvollständig::Es fehlen die Felder {', '.join(missing)} ({info}).")
    sys.exit(1)

key = data["private_key"]
if "\\n" in key and "\n" not in key:
    data["private_key"] = key.replace("\\n", "\n")

with open(out, "w", encoding="utf-8") as f:
    json.dump(data, f)

print(f"Schlüssel in Ordnung – Projekt {data['project_id']}, Konto {data['client_email'].split('@')[0]}")
gh_out = os.environ.get("GITHUB_OUTPUT")
if gh_out:
    with open(gh_out, "a", encoding="utf-8") as f:
        f.write(f"project={data['project_id']}\n")
