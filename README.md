# turp – Freunde finden auf der ganzen Welt

Android-App (Kotlin + Jetpack Compose) mit Firebase als Server.

**Was die App kann**

- Registrierung mit E-Mail, Geburtsdatum (ab 13, später nicht änderbar) und Land
- **Jugendschutz:** zwei strikt getrennte Communities (13–17 und 18+), vom Server erzwungen.
  Unter 16 muss ein Elternteil per E-Mail zustimmen. Mit 18 wechselt man automatisch die Gruppe.
- Entdecken mit Swipe-Karten: rechts = Freundschaftsanfrage, links = weiter, Filter nach Land und Alter
- Anfragen annehmen/ablehnen, bei gegenseitigem Interesse sofort befreundet
- Echtzeit-Chat zwischen Freunden (Text und Fotos, Gelesen-Status), Push-Benachrichtigungen
- Socials (Instagram, Snapchat, TikTok) nur für Freunde sichtbar – bei Jugendlichen standardmäßig aus
- Profil mit bis zu 6 Fotos, 1 kurzem Video, Bio, Interessen-Stickern und frei wählbarer Profilfarbe
- Gems: Anfragen kosten Gems, verdient werden sie durch täglichen Check-in (mit Serie), Einladungscode und neue Freundschaften
- Lounge: globaler Gruppenchat je Altersgruppe mit Slow-Mode
- Automatische Prüfung aller Fotos und Videos (Nacktbilder/Gewalt werden blockiert), Wortfilter
- Melden und Blockieren überall, Admin-Bereich für Meldungen (Jugend-Meldungen zuerst), Konto löschen
- turp+ und Gem-Shop sind vorbereitet (Käufe noch nicht aktiv)

---

## Was du brauchst

- Ein Google-Konto
- Einen Computer mit Browser (Windows, Mac oder Linux)
- Für Variante A beim APK-Bau: [Android Studio](https://developer.android.com/studio) (kostenlos)

Firebase braucht für die Server-Funktionen den **Blaze-Tarif** (Bezahlung nach Nutzung, Kreditkarte nötig).
Es gibt ein großzügiges Gratis-Kontingent – für Tests und kleine Nutzerzahlen zahlst du in der Regel nichts.
Richte trotzdem gleich eine **Budget-Warnung** ein (z. B. 5 €).

---

## Schritt 1: Firebase-Projekt einrichten (ca. 15 Minuten)

1. Öffne <https://console.firebase.google.com> → **Projekt hinzufügen** → Name z. B. `turp`.
2. **Tarif auf Blaze umstellen:** Zahnrad → *Nutzung und Abrechnung* → *Details & Einstellungen* → Blaze.
   Danach eine Budget-Benachrichtigung anlegen.
3. **Authentication** → *Loslegen* → Anbieter **E-Mail/Passwort** aktivieren.
4. **Firestore Database** → *Datenbank erstellen* → Standort **`europe-west3 (Frankfurt)`** → Produktionsmodus.
   ⚠️ Der Standort muss genau `europe-west3` sein, sonst funktionieren die automatischen Server-Funktionen nicht.
5. **Storage** → *Loslegen* → ebenfalls Standort `europe-west3`.
6. **Android-App registrieren:** Zahnrad → *Projekteinstellungen* → *App hinzufügen* → Android
   → Paketname **`app.turp`** → **`google-services.json` herunterladen** (brauchst du in Schritt 3).
7. **Bild- und Videoprüfung einschalten** (gleiches Projekt auswählen, jeweils *Aktivieren*):
   - <https://console.cloud.google.com/apis/library/vision.googleapis.com>
   - <https://console.cloud.google.com/apis/library/videointelligence.googleapis.com>
8. **E-Mail-Versand für die Elternzustimmung:** In Firebase links *Extensions* → **Trigger Email from Firestore** installieren.
   - Collection: `mail`
   - SMTP-Verbindung, z. B. mit Gmail und einem [App-Passwort](https://myaccount.google.com/apppasswords):
     `smtps://deine.adresse@gmail.com@smtp.gmail.com:465` und das App-Passwort im Passwortfeld
   - Standard-Absender: deine Adresse

Deine **Projekt-ID** findest du unter *Projekteinstellungen → Allgemein*.

---

## Schritt 2: Server hochladen (Functions, Regeln, Indizes)

Am einfachsten im Browser mit **Google Cloud Shell** – du musst nichts installieren.

1. Öffne <https://shell.cloud.google.com> (mit demselben Google-Konto).
2. Oben rechts **⋮ → Hochladen** → `turp.zip` auswählen.
3. Diese Befehle nacheinander eingeben (`DEINE-PROJEKT-ID` und die E-Mail ersetzen):

```bash
unzip turp.zip -d turp-projekt && cd turp-projekt/turp
echo "ADMIN_EMAILS=deine@email.de" > functions/.env
npm install --prefix functions
npx firebase-tools login --no-localhost
npx firebase-tools deploy --project DEINE-PROJEKT-ID
```

- Beim Login zeigt die Cloud Shell einen Link: öffnen, anmelden, Code zurück in die Shell kopieren.
- Fragen beim ersten Deploy (APIs aktivieren usw.) mit **y** bestätigen. Das dauert 5–10 Minuten.
- Meldet der allererste Deploy einen Fehler mit *Eventarc* oder *permission*, warte 3–5 Minuten und starte den letzten Befehl noch einmal. Das ist bei neuen Projekten normal.
- Die Such-Indizes für „Entdecken“ werden danach noch ein paar Minuten im Hintergrund gebaut.

---

## Schritt 3: APK bauen

### Variante A – Android Studio

1. `turp.zip` entpacken und die **`google-services.json`** in den Ordner **`turp/app/`** kopieren.
2. Android Studio → **Open** → den Ordner `turp` auswählen. Beim ersten Öffnen lädt Android Studio alles Nötige (5–10 Minuten).
3. Menü **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. Unten rechts erscheint *locate* → dort liegt **`app-debug.apk`**.
5. APK aufs Handy übertragen (USB, Google Drive, Mail …), antippen und installieren
   (beim ersten Mal „Installation aus unbekannten Quellen“ erlauben).

### Variante B – GitHub baut die APK für dich

1. Bei <https://github.com> ein **privates** Repository `turp` anlegen.
2. *uploading an existing file* → den **Inhalt** des entpackten `turp`-Ordners hineinziehen und speichern.
   Wichtig: Der versteckte Ordner `.github` muss mit hochgeladen werden
   (Mac: im Finder `Cmd + Shift + .` drücken, Windows: Explorer → *Ansicht* → *Ausgeblendete Elemente*).
   **Die `google-services.json` nicht hochladen.**
3. Im Repository **Settings → Secrets and variables → Actions → New repository secret**:
   Name `GOOGLE_SERVICES_JSON`, Wert = kompletter Inhalt deiner `google-services.json`.
4. **Actions → „APK bauen“ → Run workflow.** Nach ca. 10 Minuten beim fertigen Lauf unter
   *Artifacts* **`turp-apk`** herunterladen – darin liegt die APK.

---

## Schritt 4: Admin werden

1. Vor dem Deploy in `functions/.env` deine E-Mail bei `ADMIN_EMAILS` eintragen (siehe Schritt 2).
2. In der App mit genau dieser E-Mail registrieren und die Bestätigungs-Mail anklicken.
3. In der App **Profil → ganz unten „Admin? Zugang prüfen“** tippen. Danach erscheint der **Admin-Bereich** mit allen offenen Meldungen.

Profile, die von 3 oder mehr verschiedenen Personen gemeldet werden, verschwinden automatisch aus „Entdecken“, bis ein Admin entscheidet.

---

## So testest du

- Zwei Konten anlegen (z. B. Handy und Android-Emulator), **beide in derselben Altersgruppe** und beide mit mindestens einem Foto. Dann sehen sie sich gegenseitig unter „Entdecken“.
- Elternzustimmung testen: Geburtsdatum unter 16 wählen und eine zweite eigene E-Mail als Eltern-Adresse angeben.
- Ein Erwachsenen-Konto und ein Jugend-Konto sehen sich nie – das ist Absicht.

---

## Anpassen

- **Spielregeln** (Start-Gems, Kosten pro Anfrage, Check-in-Belohnung, Slow-Mode …): oben in `functions/index.js`
- **Wortfilter:** `functions/wordfilter.js`
- **Farben und Schrift:** `app/src/main/java/app/turp/ui/theme/Theme.kt`
- **Interessen-Sticker:** `ALL_INTERESTS` in `app/src/main/java/app/turp/ui/components/Util.kt`
- Nach Änderungen am Server: `npx firebase-tools deploy --only functions --project DEINE-PROJEKT-ID`

---

## Bevor du die App veröffentlichst

Das sind Hinweise, keine Rechtsberatung:

- **Datenschutzerklärung, Nutzungsbedingungen und Impressum** schreiben und in der App verlinken – im Onboarding steht bisher nur ein Platzhalter-Text. Der Play Store verlangt einen Link zur Datenschutzerklärung.
- In der **Play Console** die Zielgruppe (ab 13) ehrlich angeben. Für Apps mit Jugendlichen und Nutzerinhalten gelten strengere Richtlinien (Melden, Blockieren und Moderation sind eingebaut).
- Das Alter geben Nutzer selbst an. Die Trennung der Gruppen senkt das Risiko stark, ersetzt aber nicht, dass ihr Meldungen regelmäßig prüft.
- Für den Play Store brauchst du eine **signierte Release-Version**: Android Studio → *Build → Generate Signed App Bundle / APK*.

---

## Aufbau des Projekts

```
turp/
├─ app/                         Android-App
│  └─ src/main/java/app/turp/
│     ├─ data/                  Firebase-Zugriff, Modelle, Session
│     ├─ push/                  Push-Benachrichtigungen
│     └─ ui/                    Bildschirme, Komponenten, Theme
├─ functions/                   Server-Logik (Cloud Functions, Node.js 22)
├─ firestore.rules              Sicherheitsregeln der Datenbank
├─ storage.rules                Sicherheitsregeln für Fotos/Videos
├─ firestore.indexes.json       Such-Indizes
└─ .github/workflows/           automatischer APK-Bau auf GitHub
```

**Grenzen dieser ersten Version:** Login nur per E-Mail/Passwort, Käufe (turp+, Gems) noch nicht aktiv.
