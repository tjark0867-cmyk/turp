"use strict";

/**
 * kumi – Server-Logik (Firebase Cloud Functions, 2. Generation)
 *
 * Alles, was nicht manipuliert werden darf, passiert hier:
 * Alter & Altersgruppe, Gems, Freundschaftsanfragen, Elternzustimmung,
 * Moderation von Bildern/Texten, Sperren und Löschen von Konten.
 */

const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const { setGlobalOptions } = require("firebase-functions/v2");
const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineString, defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");

const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");
const { getStorage, getDownloadURL } = require("firebase-admin/storage");
const { getMessaging } = require("firebase-admin/messaging");

const { clean } = require("./wordfilter");
// Erlaubte Interessen (gleiche Liste wie in der App, InterestCatalog.kt)
const ALLOWED_INTERESTS = new Set(require("./interests.json"));
const MAX_INTERESTS = 8;
const { isStoredImageSafe, isVideoSafe, warmUp, accessToken } = require("./moderation");
const billing = require("./billing");

initializeApp();
const db = getFirestore();

// Muss zur Region in app/build.gradle.kts (FUNCTIONS_REGION) und zum
// Standort der Firestore-Datenbank passen.
const REGION = "europe-west3";
setGlobalOptions({ region: REGION, maxInstances: 10 });

const ADMIN_EMAILS = defineString("ADMIN_EMAILS", { default: "" });
// Paketname der App im Play Store (für die Prüfung von Käufen)
const PLAY_PACKAGE = defineString("PLAY_PACKAGE", { default: "app.turp" });
// SMTP-Zugang für die Eltern-E-Mails, z. B. smtps://adresse%40gmail.com:apppasswort@smtp.gmail.com:465
// Wird vom GitHub-Workflow "Server einrichten" angelegt ("none" = noch nicht eingerichtet).
const SMTP_URI = defineSecret("SMTP_URI");

// ---- Spielregeln ---------------------------------------------------------
// Muss zu app/src/main/java/app/turp/data/Models.kt passen
const START_GEMS = 50;
const REQUEST_COST = 10;
const PLUS_REQUEST_COST = 5; // mit kumi+
const CHECKIN_BASE = 10;
const STREAK_BONUS = 2; // pro Tag Serie, höchstens 5 Tage
const FRIEND_BONUS = 5;
// Briefe: jemandem schreiben, bevor er die Anfrage angenommen hat
const LETTER_PACK = 5;
const LETTER_PACK_COST = 500;
const LETTER_MAX_LEN = 500;
const CHECKIN_CYCLE = 7;   // Wochen-Kalender der täglichen Belohnung
const LETTER_DAY = 3;      // an Tag 3 jeder Woche gibt es einen Brief dazu
const INVITE_BONUS_INVITER = 50;
const INVITE_BONUS_NEW = 20;
const SLOW_MODE_MS = 10 * 1000;
const AUTO_HIDE_REPORTS = 3;
const MAX_PHOTOS = 6;
const DEFAULT_COLOR = "#2DE1C2";
const DAY_MS = 24 * 60 * 60 * 1000;

/** Hat das Konto gerade kumi+? */
function isPlus(acc) {
  if (!acc || acc.get("plus") !== true) return false;
  const until = acc.get("plusUntil");
  return !until || until.toMillis() > Date.now();
}

// ---- Hilfsfunktionen -----------------------------------------------------

function projectId() {
  if (process.env.GCLOUD_PROJECT) return process.env.GCLOUD_PROJECT;
  try {
    return JSON.parse(process.env.FIREBASE_CONFIG || "{}").projectId;
  } catch (e) {
    return "";
  }
}

/** Heutiges Datum in Deutschland als YYYY-MM-DD */
function todayBerlin() {
  return new Date().toLocaleDateString("en-CA", { timeZone: "Europe/Berlin" });
}

function shiftDay(isoDate, days) {
  const d = new Date(`${isoDate}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

function isValidIsoDate(s) {
  if (typeof s !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(s)) return false;
  const d = new Date(`${s}T00:00:00Z`);
  return !isNaN(d.getTime()) && d.toISOString().slice(0, 10) === s;
}

function isLeapYear(y) {
  return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
}

function ageOn(birthDate, onDate) {
  const [by, bm, bd] = birthDate.split("-").map(Number);
  const [ty, tm, td] = onDate.split("-").map(Number);
  let age = ty - by;
  if (tm < bm || (tm === bm && td < bd)) age -= 1;
  return age;
}

function ageGroupFor(age) {
  return age < 18 ? "teen" : "adult";
}

function pairId(a, b) {
  return [a, b].sort().join("_");
}

function str(v, max) {
  return typeof v === "string" ? v.trim().slice(0, max) : "";
}

function isSafeId(id) {
  return typeof id === "string" && id.length > 0 && id.length <= 300 && !id.includes("/");
}

function isEmail(s) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(s);
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function requireAuth(req) {
  if (!req.auth) throw new HttpsError("unauthenticated", "Bitte melde dich an.");
  return req.auth.uid;
}

function requireActive(req) {
  const uid = requireAuth(req);
  const t = req.auth.token;
  if (t.active !== true || (t.ageGroup !== "teen" && t.ageGroup !== "adult")) {
    throw new HttpsError("permission-denied", "Dein Konto ist noch nicht freigeschaltet.");
  }
  return uid;
}

function requireAdmin(req) {
  const uid = requireAuth(req);
  if (req.auth.token.admin !== true) throw new HttpsError("permission-denied", "Nur für Admins.");
  return uid;
}

/** Ergänzt die Custom Claims eines Nutzers, ohne bestehende zu verlieren. */
async function setClaims(uid, patch) {
  const user = await getAuth().getUser(uid);
  await getAuth().setCustomUserClaims(uid, { ...(user.customClaims || {}), ...patch });
}

function memberInfo(userSnap) {
  const d = userSnap.data() || {};
  return { name: d.name || "", photo: (d.photos || [])[0] || "", color: d.color || DEFAULT_COLOR };
}

/** Download-Link einer Datei; legt bei Bedarf einen Zugriffs-Token an. */
async function downloadUrl(file) {
  try {
    return await getDownloadURL(file);
  } catch (e) {
    await file.setMetadata({ metadata: { firebaseStorageDownloadTokens: crypto.randomUUID() } });
    return getDownloadURL(file);
  }
}

function pathFromDownloadUrl(url) {
  try {
    const m = new URL(url).pathname.match(/\/o\/(.+)$/);
    return m ? decodeURIComponent(m[1]) : "";
  } catch (e) {
    return "";
  }
}

// Push-Texte in der Sprache des Empfängers (accounts/{uid}.lang, setzt die App)
const PUSH_I18N = require("./push_i18n.json");
function trPush(lang, text, vars = {}) {
  const table = PUSH_I18N[lang] || {};
  let out = table[text] || text;
  for (const [k, v] of Object.entries(vars)) out = out.split(`{${k}}`).join(String(v));
  return out;
}

async function notify(uid, title, body, data = {}, vars = {}) {
  try {
    const acc = await db.doc(`accounts/${uid}`).get();
    const token = acc.exists ? acc.get("fcmToken") : "";
    if (!token) return;
    const lang = (acc.exists && acc.get("lang")) || "de";
    title = trPush(lang, title, vars);
    body = trPush(lang, body, vars);
    const payload = {};
    for (const [k, v] of Object.entries(data)) payload[k] = String(v);
    await getMessaging().send({
      token,
      notification: { title, body },
      data: payload,
      android: { priority: "high", notification: { channelId: "turp_default" } },
    });
  } catch (e) {
    logger.warn("Push fehlgeschlagen", { uid, error: e.message });
    if (e.code === "messaging/registration-token-not-registered") {
      await db.doc(`accounts/${uid}`).update({ fcmToken: "" }).catch(() => {});
    }
  }
}

const GENDERS = new Set(["male", "female", "other"]);

const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
function randomCode(len = 6) {
  const bytes = crypto.randomBytes(len);
  let s = "";
  for (let i = 0; i < len; i++) s += CODE_ALPHABET[bytes[i] % CODE_ALPHABET.length];
  return s;
}

// ---- Profil anlegen -------------------------------------------------------

exports.createProfile = onCall({ secrets: [SMTP_URI] }, async (req) => {
  const uid = requireAuth(req);
  const data = req.data || {};
  const name = str(data.name, 30);
  const birthDate = str(data.birthDate, 10);
  const country = str(data.country, 2).toUpperCase();
  const parentEmail = str(data.parentEmail, 200).toLowerCase();
  const inviteCode = str(data.inviteCode, 12).toUpperCase();
  // Geschlecht (ältere App-Versionen schicken keins)
  const gender = GENDERS.has(str(data.gender, 10)) ? str(data.gender, 10) : "";

  if (name.length < 2) throw new HttpsError("invalid-argument", "Bitte gib einen Namen mit mindestens 2 Zeichen ein.");
  if (!isValidIsoDate(birthDate)) throw new HttpsError("invalid-argument", "Bitte gib ein gültiges Geburtsdatum ein.");
  if (!/^[A-Z]{2}$/.test(country)) throw new HttpsError("invalid-argument", "Bitte wähle dein Land aus.");

  const age = ageOn(birthDate, todayBerlin());
  if (age < 13) {
    throw new HttpsError("failed-precondition", "Du musst mindestens 13 Jahre alt sein, um kumi zu nutzen.");
  }
  if (age > 110) throw new HttpsError("invalid-argument", "Bitte gib ein gültiges Geburtsdatum ein.");

  // DSGVO (Deutschland): unter 16 braucht es die Zustimmung eines Elternteils
  const needsConsent = age < 16;
  const ownEmail = String(req.auth.token.email || "").toLowerCase();
  if (needsConsent) {
    if (!isEmail(parentEmail)) {
      throw new HttpsError("invalid-argument", "Bitte gib die E-Mail-Adresse eines Elternteils ein.");
    }
    if (parentEmail === ownEmail) {
      throw new HttpsError("invalid-argument", "Die Eltern-E-Mail muss eine andere Adresse sein als deine eigene.");
    }
  }

  const accRef = db.doc(`accounts/${uid}`);
  if ((await accRef.get()).exists) throw new HttpsError("already-exists", "Du hast bereits ein Profil.");

  const ageGroup = ageGroupFor(age);
  const status = needsConsent ? "pending_consent" : "active";

  let invitedBy = "";
  if (/^[A-Z0-9]{4,12}$/.test(inviteCode)) {
    const inv = await db.doc(`inviteCodes/${inviteCode}`).get();
    if (inv.exists && inv.get("uid") !== uid) invitedBy = inv.get("uid");
  }

  let myCode = "";
  for (let i = 0; i < 10 && !myCode; i++) {
    const candidate = randomCode();
    try {
      await db.doc(`inviteCodes/${candidate}`).create({ uid, createdAt: FieldValue.serverTimestamp() });
      myCode = candidate;
    } catch (e) {
      // Code schon vergeben – nächster Versuch
    }
  }

  // Rechte zuerst setzen: Sobald das Konto-Dokument erscheint, lädt die App
  // ihren Login-Token neu und hat dann sofort die passende Altersgruppe.
  await setClaims(uid, { ageGroup, active: status === "active" });

  const batch = db.batch();
  batch.set(accRef, {
    birthDate,
    birthMonthDay: birthDate.slice(5),
    ageGroup,
    status,
    gems: START_GEMS + (invitedBy ? INVITE_BONUS_NEW : 0),
    streak: 0,
    lastCheckIn: "",
    inviteCode: myCode,
    invitedBy,
    parentEmail: needsConsent ? parentEmail : "",
    plus: false,
    fcmToken: "",
    videoPath: "",
    createdAt: FieldValue.serverTimestamp(),
  });
  batch.set(db.doc(`users/${uid}`), {
    name,
    age,
    country,
    gender,
    bio: "",
    interests: [],
    photos: [],
    videoUrl: "",
    color: DEFAULT_COLOR,
    ageGroup,
    active: status === "active",
    hiddenForReview: false,
    random: Math.random(),
    createdAt: FieldValue.serverTimestamp(),
  });
  // Socials: für Minderjährige standardmäßig ausgeblendet
  batch.set(db.doc(`users/${uid}/private/socials`), {
    instagram: "",
    snapchat: "",
    tiktok: "",
    visible: ageGroup === "adult",
  });
  await batch.commit();

  if (invitedBy) {
    await db.doc(`accounts/${invitedBy}`)
      .update({ gems: FieldValue.increment(INVITE_BONUS_INVITER) })
      .catch((e) => logger.warn("Einladungsbonus fehlgeschlagen", e.message));
  }
  if (needsConsent) await sendConsentMail(uid, parentEmail, name);

  return { status, ageGroup };
});

// ---- Elternzustimmung -----------------------------------------------------

function consentUrl(token) {
  return `https://${REGION}-${projectId()}.cloudfunctions.net/parentConsent?token=${token}`;
}

async function sendConsentMail(uid, parentEmail, childName) {
  const old = await db.collection("consents").where("uid", "==", uid).get();
  const token = crypto.randomBytes(24).toString("hex");
  const link = consentUrl(token);
  const safeName = escapeHtml(childName);

  const text = [
    "Hallo,",
    "",
    `${childName} hat sich bei kumi angemeldet und deine E-Mail-Adresse als Elternteil angegeben.`,
    "kumi ist eine App, um neue Freunde zu finden. Weil dein Kind jünger als 16 ist,",
    "brauchen wir deine Zustimmung, bevor das Konto genutzt werden kann.",
    "",
    "So schützt kumi Jugendliche:",
    "- 13- bis 17-Jährige sehen und schreiben nur mit Gleichaltrigen. Erwachsene haben keinen Zugang zu diesem Bereich.",
    "- Chats gibt es nur zwischen Freunden, die eine Anfrage angenommen haben.",
    "- Alle Bilder werden automatisch geprüft, Nacktbilder werden blockiert.",
    "- Jedes Profil und jede Nachricht kann gemeldet und blockiert werden.",
    "",
    `Zustimmen oder ablehnen: ${link}`,
    "",
    "Wenn du diese E-Mail nicht erwartet hast, kannst du sie ignorieren. Ohne Zustimmung bleibt das Konto gesperrt.",
    "",
    "Dein kumi-Team",
  ].join("\n");

  const html = `
  <div style="font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;max-width:560px;margin:auto;color:#1b1d2e">
    <h1 style="color:#0E0F1A;font-size:28px;margin:0 0 4px">kumi</h1>
    <p>Hallo,</p>
    <p><b>${safeName}</b> hat sich bei kumi angemeldet und deine E-Mail-Adresse als Elternteil angegeben.
    kumi ist eine App, um neue Freunde zu finden. Weil dein Kind jünger als 16 ist, brauchen wir deine Zustimmung,
    bevor das Konto genutzt werden kann.</p>
    <p><b>So schützt kumi Jugendliche:</b></p>
    <ul>
      <li>13- bis 17-Jährige sehen und schreiben nur mit Gleichaltrigen. Erwachsene haben keinen Zugang zu diesem Bereich.</li>
      <li>Chats gibt es nur zwischen Freunden, die eine Anfrage angenommen haben.</li>
      <li>Alle Bilder werden automatisch geprüft, Nacktbilder werden blockiert.</li>
      <li>Jedes Profil und jede Nachricht kann gemeldet und blockiert werden.</li>
    </ul>
    <p style="margin:28px 0">
      <a href="${link}" style="background:#2DE1C2;color:#0E0F1A;padding:14px 22px;border-radius:999px;text-decoration:none;font-weight:700">
        Zustimmen oder ablehnen
      </a>
    </p>
    <p style="color:#666;font-size:13px">Wenn du diese E-Mail nicht erwartet hast, kannst du sie ignorieren.
    Ohne Zustimmung bleibt das Konto gesperrt.</p>
  </div>`;

  const batch = db.batch();
  old.forEach((d) => batch.delete(d.ref));
  batch.set(db.doc(`consents/${token}`), { uid, parentEmail, createdAt: FieldValue.serverTimestamp() });
  batch.update(db.doc(`accounts/${uid}`), { parentEmail, lastConsentMailAt: FieldValue.serverTimestamp() });
  await batch.commit();

  return deliverMail(parentEmail, `${childName} möchte kumi nutzen – bitte um deine Zustimmung`, text, html);
}

/** Verschickt eine E-Mail über den SMTP-Zugang. Liefert false, wenn keiner eingerichtet ist. */
async function deliverMail(to, subject, text, html) {
  let uri = "";
  try {
    uri = String(SMTP_URI.value() || "").trim();
  } catch (e) {
    uri = "";
  }
  if (!uri || uri === "none") {
    logger.warn("Kein E-Mail-Versand eingerichtet (SMTP_URI) – Eltern-E-Mail wurde nicht verschickt.");
    return false;
  }
  try {
    const nodemailer = require("nodemailer");
    let from = "kumi";
    try {
      from = `kumi <${decodeURIComponent(new URL(uri).username)}>`;
    } catch (e) {
      // Absender bleibt "kumi"
    }
    await nodemailer.createTransport(uri).sendMail({ from, to, subject, text, html });
    return true;
  } catch (e) {
    logger.error("E-Mail konnte nicht verschickt werden", e.message);
    return false;
  }
}

exports.resendConsent = onCall({ secrets: [SMTP_URI] }, async (req) => {
  const uid = requireAuth(req);
  const accRef = db.doc(`accounts/${uid}`);
  const acc = await accRef.get();
  if (!acc.exists || acc.get("status") !== "pending_consent") {
    throw new HttpsError("failed-precondition", "Für dein Konto ist keine Zustimmung nötig.");
  }
  const last = acc.get("lastConsentMailAt");
  if (last && Date.now() - last.toMillis() < 5 * 60 * 1000) {
    throw new HttpsError("resource-exhausted", "Bitte warte ein paar Minuten, bevor du die E-Mail erneut sendest.");
  }
  const parentEmail = str(req.data && req.data.parentEmail, 200).toLowerCase() || acc.get("parentEmail");
  if (!isEmail(parentEmail)) throw new HttpsError("invalid-argument", "Bitte gib eine gültige E-Mail-Adresse ein.");
  if (parentEmail === String(req.auth.token.email || "").toLowerCase()) {
    throw new HttpsError("invalid-argument", "Die Eltern-E-Mail muss eine andere Adresse sein als deine eigene.");
  }
  const user = await db.doc(`users/${uid}`).get();
  const sent = await sendConsentMail(uid, parentEmail, user.get("name") || "Dein Kind");
  if (!sent) {
    throw new HttpsError("unavailable",
      "Der E-Mail-Versand ist noch nicht eingerichtet. Bitte melde dich beim kumi-Team.");
  }
  return { ok: true };
});

function page(title, bodyHtml) {
  return `<!doctype html><html lang="de"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>${escapeHtml(title)} · kumi</title>
<style>
body{margin:0;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:#0E0F1A;color:#F4F5FB;
display:flex;min-height:100vh;align-items:center;justify-content:center;padding:16px;box-sizing:border-box}
.card{max-width:540px;width:100%;background:#171A2C;border-radius:24px;padding:28px;box-sizing:border-box}
.logo{font-weight:900;font-size:34px;color:#2DE1C2;margin-bottom:12px}
h1{margin:0 0 12px;font-size:24px} p,li{line-height:1.55;color:#C9CCE0} ul{padding-left:20px}
form{display:flex;gap:12px;flex-wrap:wrap;margin-top:22px}
button{border:0;border-radius:999px;padding:14px 22px;font-size:16px;font-weight:700;cursor:pointer}
.yes{background:#2DE1C2;color:#0E0F1A}.no{background:transparent;color:#FF5C8A;border:2px solid #FF5C8A}
</style></head><body><div class="card"><div class="logo">kumi</div><h1>${escapeHtml(title)}</h1>${bodyHtml}</div></body></html>`;
}

// Link aus der Eltern-E-Mail. GET zeigt nur die Seite an (E-Mail-Scanner
// öffnen Links automatisch), erst der Knopf (POST) entscheidet.
exports.parentConsent = onRequest(async (req, res) => {
  res.set("Cache-Control", "no-store");
  const token = String((req.method === "POST" ? req.body && req.body.token : req.query.token) || "");
  if (!/^[a-f0-9]{48}$/.test(token)) {
    res.status(400).send(page("Ungültiger Link", "<p>Dieser Link ist nicht gültig.</p>"));
    return;
  }
  const ref = db.doc(`consents/${token}`);
  const snap = await ref.get();
  const createdAt = snap.exists ? snap.get("createdAt") : null;
  const uid = snap.exists ? snap.get("uid") : "";
  const user = uid ? await db.doc(`users/${uid}`).get() : null;
  if (!snap.exists || !user || !user.exists || (createdAt && Date.now() - createdAt.toMillis() > 14 * DAY_MS)) {
    res.status(410).send(page("Link abgelaufen",
      "<p>Dieser Link ist abgelaufen oder wurde schon benutzt. Dein Kind kann in der App eine neue E-Mail anfordern.</p>"));
    return;
  }
  const name = escapeHtml(user.get("name") || "Dein Kind");

  if (req.method === "GET") {
    res.send(page("Zustimmung für kumi", `
      <p><b>${name}</b> (${escapeHtml(String(user.get("age") || ""))} Jahre) möchte kumi nutzen, eine App zum Freunde-Finden.</p>
      <ul>
        <li>Jugendliche (13–17) sind strikt von Erwachsenen getrennt.</li>
        <li>Chats nur zwischen Freunden, die sich gegenseitig bestätigt haben.</li>
        <li>Automatische Bildprüfung, Melden und Blockieren überall.</li>
        <li>Socials (Instagram usw.) sind für Minderjährige standardmäßig ausgeblendet.</li>
      </ul>
      <form method="post">
        <input type="hidden" name="token" value="${token}">
        <button class="yes" name="action" value="approve">Ich stimme zu</button>
        <button class="no" name="action" value="decline">Ablehnen und Konto löschen</button>
      </form>`));
    return;
  }

  if (req.method !== "POST") {
    res.status(405).send(page("Nicht erlaubt", "<p>Bitte nutze die Knöpfe auf der Seite.</p>"));
    return;
  }

  const action = String((req.body && req.body.action) || "");
  if (action === "approve") {
    await setClaims(uid, { active: true });
    await db.doc(`accounts/${uid}`).update({
      status: "active",
      consentAt: FieldValue.serverTimestamp(),
      consentEmail: snap.get("parentEmail") || "",
    });
    await db.doc(`users/${uid}`).update({ active: true });
    await ref.delete();
    res.send(page("Danke!", `<p>Das Konto von <b>${name}</b> ist jetzt freigeschaltet. Die App aktualisiert sich automatisch.</p>`));
  } else if (action === "decline") {
    await deleteUserData(uid);
    res.send(page("Abgelehnt", "<p>Das Konto wurde mit allen Daten gelöscht.</p>"));
  } else {
    res.status(400).send(page("Ungültige Anfrage", "<p>Bitte nutze die Knöpfe auf der Seite.</p>"));
  }
});

// ---- Admin ---------------------------------------------------------------

exports.claimAdmin = onCall(async (req) => {
  const uid = requireAuth(req);
  const email = String(req.auth.token.email || "").toLowerCase();
  const allowed = ADMIN_EMAILS.value().split(",").map((s) => s.trim().toLowerCase()).filter(Boolean);
  if (!email || !allowed.includes(email)) throw new HttpsError("permission-denied", "Kein Admin-Zugang für dieses Konto.");
  if (req.auth.token.email_verified !== true) {
    throw new HttpsError("failed-precondition", "Bitte bestätige zuerst deine E-Mail-Adresse.");
  }
  await setClaims(uid, { admin: true });
  return { ok: true };
});

async function banUser(uid) {
  await setClaims(uid, { active: false }).catch((e) => logger.warn("Claims beim Sperren", e.message));
  await db.doc(`accounts/${uid}`).update({ status: "banned", bannedAt: FieldValue.serverTimestamp() }).catch(() => {});
  await db.doc(`users/${uid}`).update({ active: false }).catch(() => {});
  const chats = await db.collection("chats").where("members", "array-contains", uid).get();
  const w = db.bulkWriter();
  chats.forEach((c) => w.update(c.ref, { closed: true }));
  await w.close();
  await getAuth().revokeRefreshTokens(uid).catch(() => {});
}

exports.adminAction = onCall(async (req) => {
  const adminUid = requireAdmin(req);
  const reportId = str(req.data && req.data.reportId, 300);
  const action = req.data && req.data.action;
  if (!isSafeId(reportId) || !["ban", "dismiss"].includes(action)) {
    throw new HttpsError("invalid-argument", "Ungültige Aktion.");
  }
  const ref = db.doc(`reports/${reportId}`);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError("not-found", "Meldung nicht gefunden.");
  const target = snap.get("target");
  const handled = { handledBy: adminUid, handledAt: FieldValue.serverTimestamp() };

  if (action === "ban") {
    await banUser(target);
    const open = await db.collection("reports").where("target", "==", target).where("status", "==", "open").get();
    const w = db.bulkWriter();
    open.forEach((d) => w.update(d.ref, { status: "banned", ...handled }));
    await w.close();
  } else {
    await ref.update({ status: "dismissed", ...handled });
    const [u, acc] = await Promise.all([db.doc(`users/${target}`).get(), db.doc(`accounts/${target}`).get()]);
    if (u.exists && u.get("hiddenForReview") === true && acc.exists && acc.get("status") === "active") {
      await u.ref.update({ active: true, hiddenForReview: false });
    }
  }
  return { ok: true };
});

// ---- Freundschaften ------------------------------------------------------

async function befriend(aUid, bUid) {
  const [a, b] = await Promise.all([db.doc(`users/${aUid}`).get(), db.doc(`users/${bUid}`).get()]);
  // Sicherheitsprüfung: Freundschaften nur innerhalb derselben Altersgruppe
  if (!a.exists || !b.exists || a.get("ageGroup") !== b.get("ageGroup")) {
    throw new HttpsError("failed-precondition", "Diese Freundschaft ist nicht möglich.");
  }
  const chatId = pairId(aUid, bUid);
  const now = FieldValue.serverTimestamp();
  const batch = db.batch();
  batch.set(db.doc(`chats/${chatId}`), {
    members: [aUid, bUid],
    ageGroup: a.get("ageGroup"),
    closed: false,
    createdAt: now,
    lastAt: now,
    lastMessage: "Ihr seid jetzt Freunde! Sagt Hallo 👋",
    lastSender: "",
    readAt: {},
    memberInfo: { [aUid]: memberInfo(a), [bUid]: memberInfo(b) },
  });
  batch.set(db.doc(`users/${aUid}/friends/${bUid}`), { since: now, chatId });
  batch.set(db.doc(`users/${bUid}/friends/${aUid}`), { since: now, chatId });
  batch.update(db.doc(`accounts/${aUid}`), { gems: FieldValue.increment(FRIEND_BONUS) });
  batch.update(db.doc(`accounts/${bUid}`), { gems: FieldValue.increment(FRIEND_BONUS) });
  await batch.commit();
  return chatId;
}

// ---- Test-Bot ---------------------------------------------------------------
// Ein Profil pro Altersgruppe, das Anfragen sofort annimmt und im Chat antwortet.
// So lässt sich der Chat ohne zweites Handy testen. Die App zeigt den Bot nur
// in Test-Versionen (Debug-Builds) an.

const BOT_NAME = "kumi Bot";
const BOT_BIO = "Ich bin der Testbot von kumi. Schreib mir – ich antworte sofort, damit du den Chat testen kannst. 🤖";

function botUidFor(group) {
  return `turp-bot-${group}`;
}

async function ensureBot(group) {
  const botUid = botUidFor(group);
  const ref = db.doc(`users/${botUid}`);
  const snap = await ref.get();
  if (snap.exists && (snap.get("photos") || []).length > 0) {
    // Name/Bio nach einer Umbenennung der App aktualisieren
    if (snap.get("name") !== BOT_NAME || snap.get("bio") !== BOT_BIO) {
      await ref.update({ name: BOT_NAME, bio: BOT_BIO });
    }
    return botUid;
  }

  const file = getStorage().bucket().file(`media/${botUid}/avatar.png`);
  await file.save(fs.readFileSync(path.join(__dirname, "assets", "bot.png")), {
    resumable: false,
    metadata: { contentType: "image/png", metadata: { firebaseStorageDownloadTokens: crypto.randomUUID() } },
  });
  const url = await downloadUrl(file);
  const now = FieldValue.serverTimestamp();
  const batch = db.batch();
  batch.set(ref, {
    name: BOT_NAME,
    age: 0,
    country: "DE",
    bio: BOT_BIO,
    interests: ["💻 Coden", "😂 Memes"],
    photos: [url],
    videoUrl: "",
    color: DEFAULT_COLOR,
    ageGroup: group,
    active: true,
    hiddenForReview: false,
    bot: true,
    random: Math.random(),
    createdAt: now,
  });
  batch.set(db.doc(`accounts/${botUid}`), {
    status: "active",
    ageGroup: group,
    gems: 0,
    streak: 0,
    lastCheckIn: "",
    inviteCode: "",
    invitedBy: "",
    parentEmail: "",
    plus: false,
    fcmToken: "",
    videoPath: "",
    bot: true,
    createdAt: now,
  });
  batch.set(db.doc(`users/${botUid}/private/socials`), { instagram: "", snapchat: "", tiktok: "", visible: false });
  await batch.commit();
  return botUid;
}

async function botSay(chatId, botUid, text) {
  await db.collection(`chats/${chatId}/messages`).add({
    senderId: botUid,
    type: "text",
    text,
    createdAt: FieldValue.serverTimestamp(),
  });
}

const BOT_GREETING = "Hey! 👋 Ich bin der kumi-Testbot. Schreib mir etwas – ich antworte sofort, damit du den Chat testen kannst.";

function botReply(msg, preview) {
  if (msg.type === "audio") return "Sprachnachricht angekommen 🎤 – Abspielen klappt ✅";
  if (msg.type === "image") {
    return String(preview).includes("blockiert")
      ? "Dein Foto wurde von der automatischen Bildprüfung blockiert 🚫"
      : "Foto angekommen 📷 – die automatische Bildprüfung hat es durchgelassen ✅";
  }
  const text = String(msg.text || "").toLowerCase();
  if (/(^|\s)(hallo|hi|hey|moin|servus|hallöchen)(\s|!|$)/.test(text)) {
    return "Hey! 😊 Deine Nachricht ist angekommen – der Chat funktioniert ✅";
  }
  if (text.includes("?")) return "Gute Frage! 🤔 Ich bin nur ein Testbot, aber deine Nachricht ist angekommen ✅";
  const replies = [
    "Angekommen ✅",
    "Läuft! Der Chat funktioniert 🙌",
    "Hab ich bekommen 👍",
    "Schreib ruhig weiter – ich antworte immer sofort.",
    "Tipp: Halte eine Nachricht gedrückt, um sie zu melden.",
    "Du kannst mir auch ein Foto schicken – dann siehst du, ob die Bildprüfung klappt 📷",
  ];
  return replies[Math.floor(Math.random() * replies.length)];
}

exports.startTestChat = onCall(async (req) => {
  const uid = requireActive(req);
  const botUid = await ensureBot(req.auth.token.ageGroup);
  const friend = await db.doc(`users/${uid}/friends/${botUid}`).get();
  if (friend.exists && friend.get("chatId")) {
    const chatId = friend.get("chatId");
    await db.doc(`chats/${chatId}`).update({ closed: false }).catch(() => {});
    return { chatId };
  }
  const chatId = await befriend(botUid, uid);
  await botSay(chatId, botUid, BOT_GREETING);
  return { chatId };
});

exports.sendRequest = onCall(async (req) => {
  const uid = requireActive(req);
  const toUid = str(req.data && req.data.toUid, 128);
  if (!isSafeId(toUid) || toUid === uid) throw new HttpsError("invalid-argument", "Ungültige Person.");
  const group = req.auth.token.ageGroup;

  const [me, other, blockedByMe, blockedMe, friend] = await Promise.all([
    db.doc(`users/${uid}`).get(),
    db.doc(`users/${toUid}`).get(),
    db.doc(`users/${uid}/blocked/${toUid}`).get(),
    db.doc(`users/${toUid}/blocked/${uid}`).get(),
    db.doc(`users/${uid}/friends/${toUid}`).get(),
  ]);
  if (!me.exists || me.get("ageGroup") !== group) {
    throw new HttpsError("failed-precondition", "Bitte starte die App neu.");
  }
  const unavailable = new HttpsError("not-found", "Dieses Profil ist gerade nicht verfügbar.");
  if (!other.exists || other.get("ageGroup") !== group || other.get("active") !== true) throw unavailable;
  if (blockedByMe.exists || blockedMe.exists) throw unavailable;
  if (friend.exists) throw new HttpsError("already-exists", "Ihr seid schon befreundet.");

  // Test-Bot: nimmt jede Anfrage sofort an (kostenlos)
  if (other.get("bot") === true) {
    const chatId = await befriend(toUid, uid);
    await db.doc(`users/${uid}/seen/${toUid}`).set({ at: FieldValue.serverTimestamp(), liked: true });
    await botSay(chatId, toUid, BOT_GREETING);
    return { matched: true, chatId };
  }

  // Hat die andere Person mich schon angefragt? Dann direkt Freunde – kostenlos.
  const reverseRef = db.doc(`requests/${toUid}_${uid}`);
  const reverse = await reverseRef.get();
  if (reverse.exists && reverse.get("status") === "pending") {
    const chatId = await befriend(toUid, uid);
    await reverseRef.update({ status: "accepted", respondedAt: FieldValue.serverTimestamp(), chatId });
    await db.doc(`users/${uid}/seen/${toUid}`).set({ at: FieldValue.serverTimestamp(), liked: true });
    await notify(toUid, "Ihr seid jetzt Freunde 🎉", "{name} hat deine Anfrage erwidert. Sagt Hallo!",
      { type: "accepted", chatId }, { name: me.get("name") });
    return { matched: true, chatId };
  }

  const reqRef = db.doc(`requests/${uid}_${toUid}`);
  const accRef = db.doc(`accounts/${uid}`);
  const gems = await db.runTransaction(async (tx) => {
    const [acc, existing] = await Promise.all([tx.get(accRef), tx.get(reqRef)]);
    if (existing.exists && existing.get("status") === "pending") {
      throw new HttpsError("already-exists", "Du hast schon eine Anfrage gesendet.");
    }
    if (existing.exists && existing.get("status") === "declined") {
      throw new HttpsError("failed-precondition", "Eine Anfrage ist hier gerade nicht möglich.");
    }
    const current = acc.get("gems") || 0;
    const cost = isPlus(acc) ? PLUS_REQUEST_COST : REQUEST_COST;
    if (current < cost) {
      throw new HttpsError("resource-exhausted",
        `Nicht genug Gems – eine Anfrage kostet ${cost} 💎. Hol dir neue beim täglichen Check-in oder im Shop!`);
    }
    tx.update(accRef, { gems: current - cost });
    tx.set(reqRef, {
      from: uid,
      to: toUid,
      ageGroup: group,
      status: "pending",
      createdAt: FieldValue.serverTimestamp(),
      fromName: me.get("name") || "",
      fromPhoto: (me.get("photos") || [])[0] || "",
      fromColor: me.get("color") || DEFAULT_COLOR,
      fromAge: me.get("age") || 0,
      fromCountry: me.get("country") || "",
      toName: other.get("name") || "",
      toPhoto: (other.get("photos") || [])[0] || "",
      toColor: other.get("color") || DEFAULT_COLOR,
    });
    tx.set(db.doc(`users/${uid}/seen/${toUid}`), { at: FieldValue.serverTimestamp(), liked: true });
    return current - cost;
  });

  await notify(toUid, "Neue Freundschaftsanfrage 👋", "{name} möchte mit dir befreundet sein.", { type: "request" }, { name: me.get("name") });
  return { matched: false, gems };
});

exports.respondRequest = onCall(async (req) => {
  const uid = requireActive(req);
  const requestId = str(req.data && req.data.requestId, 300);
  const accept = req.data && req.data.accept === true;
  if (!isSafeId(requestId)) throw new HttpsError("invalid-argument", "Ungültige Anfrage.");

  const ref = db.doc(`requests/${requestId}`);
  const snap = await ref.get();
  if (!snap.exists || snap.get("to") !== uid || snap.get("status") !== "pending") {
    throw new HttpsError("not-found", "Diese Anfrage gibt es nicht mehr.");
  }
  if (!accept) {
    await ref.update({ status: "declined", respondedAt: FieldValue.serverTimestamp() });
    if (snap.get("letter") === true && snap.get("chatId")) {
      const chatRef = db.doc(`chats/${snap.get("chatId")}`);
      const chat = await chatRef.get();
      // nur löschen, solange der Brief-Chat noch offen ist (nicht nach einer Freundschaft)
      if (chat.exists && chat.get("pending") === true) await db.recursiveDelete(chatRef);
    }
    return { ok: true };
  }
  const from = snap.get("from");
  const chatId = await befriend(from, uid);
  await ref.update({ status: "accepted", respondedAt: FieldValue.serverTimestamp(), chatId });
  const me = await db.doc(`users/${uid}`).get();
  await notify(from, "Anfrage angenommen 🎉", "{name} hat deine Anfrage angenommen. Sagt Hallo!",
    { type: "accepted", chatId }, { name: me.get("name") });
  return { chatId };
});

// ---- Briefe ----------------------------------------------------------------
// Ein Brief ist eine Anfrage mit erster Nachricht: Die andere Person bekommt sofort
// einen Chat und entscheidet dort, ob sie annimmt. Bis dahin kann niemand weiterschreiben.

exports.sendLetter = onCall(async (req) => {
  const uid = requireActive(req);
  const toUid = str(req.data && req.data.toUid, 128);
  const text = str(req.data && req.data.text, LETTER_MAX_LEN);
  if (!isSafeId(toUid) || toUid === uid) throw new HttpsError("invalid-argument", "Ungültige Person.");
  if (!text) throw new HttpsError("invalid-argument", "Die Nachricht ist leer.");
  const group = req.auth.token.ageGroup;

  const [me, other, blockedByMe, blockedMe, friend] = await Promise.all([
    db.doc(`users/${uid}`).get(),
    db.doc(`users/${toUid}`).get(),
    db.doc(`users/${uid}/blocked/${toUid}`).get(),
    db.doc(`users/${toUid}/blocked/${uid}`).get(),
    db.doc(`users/${uid}/friends/${toUid}`).get(),
  ]);
  if (!me.exists || me.get("ageGroup") !== group) throw new HttpsError("failed-precondition", "Bitte starte die App neu.");
  const unavailable = new HttpsError("not-found", "Dieses Profil ist gerade nicht verfügbar.");
  if (!other.exists || other.get("ageGroup") !== group || other.get("active") !== true) throw unavailable;
  if (other.get("bot") === true) throw unavailable;
  if (blockedByMe.exists || blockedMe.exists) throw unavailable;
  if (friend.exists) throw new HttpsError("already-exists", "Ihr seid schon befreundet.");

  // Hat die andere Person mich schon angefragt? Dann direkt Freunde – der Brief kostet nichts.
  const reverseRef = db.doc(`requests/${toUid}_${uid}`);
  const reverse = await reverseRef.get();
  if (reverse.exists && reverse.get("status") === "pending") {
    const chatId = await befriend(toUid, uid);
    await reverseRef.update({ status: "accepted", respondedAt: FieldValue.serverTimestamp(), chatId });
    await db.doc(`users/${uid}/seen/${toUid}`).set({ at: FieldValue.serverTimestamp(), liked: true });
    await db.collection(`chats/${chatId}/messages`).add({ senderId: uid, type: "text", text, createdAt: FieldValue.serverTimestamp() });
    return { matched: true, chatId };
  }

  const reqRef = db.doc(`requests/${uid}_${toUid}`);
  const accRef = db.doc(`accounts/${uid}`);
  const chatId = pairId(uid, toUid);
  const chatRef = db.doc(`chats/${chatId}`);
  const letters = await db.runTransaction(async (tx) => {
    const [acc, existing] = await Promise.all([tx.get(accRef), tx.get(reqRef)]);
    if (existing.exists && existing.get("status") === "pending") {
      throw new HttpsError("already-exists", "Du hast schon eine Anfrage gesendet.");
    }
    if (existing.exists && existing.get("status") === "declined") {
      throw new HttpsError("failed-precondition", "Eine Anfrage ist hier gerade nicht möglich.");
    }
    const have = acc.get("letters") || 0;
    if (have < 1) {
      throw new HttpsError("resource-exhausted", "Du hast keine Briefe mehr – hol dir 5 Stück für 500 💎 im Shop!");
    }
    const now = FieldValue.serverTimestamp();
    tx.update(accRef, { letters: have - 1 });
    tx.set(reqRef, {
      from: uid,
      to: toUid,
      ageGroup: group,
      status: "pending",
      letter: true,
      chatId,
      createdAt: now,
      fromName: me.get("name") || "",
      fromPhoto: (me.get("photos") || [])[0] || "",
      fromColor: me.get("color") || DEFAULT_COLOR,
      fromAge: me.get("age") || 0,
      fromCountry: me.get("country") || "",
      toName: other.get("name") || "",
      toPhoto: (other.get("photos") || [])[0] || "",
      toColor: other.get("color") || DEFAULT_COLOR,
    });
    tx.set(chatRef, {
      members: [uid, toUid],
      ageGroup: group,
      closed: false,
      pending: true,
      pendingFrom: uid,
      createdAt: now,
      lastAt: now,
      lastMessage: text.slice(0, 80),
      lastSender: uid,
      readAt: {},
      memberInfo: { [uid]: memberInfo(me), [toUid]: memberInfo(other) },
    });
    tx.set(chatRef.collection("messages").doc(), { senderId: uid, type: "text", text, letter: true, createdAt: now });
    tx.set(db.doc(`users/${uid}/seen/${toUid}`), { at: now, liked: true });
    return have - 1;
  });
  return { matched: false, chatId, letters };
});

// Geschenk zum Start der Briefe: jedes Konto bekommt einmalig 5 Briefe (holt die neue App-Version ab)
exports.claimLetterGift = onCall(async (req) => {
  const uid = requireActive(req);
  const ref = db.doc(`accounts/${uid}`);
  return db.runTransaction(async (tx) => {
    const acc = await tx.get(ref);
    if (!acc.exists) throw new HttpsError("not-found", "Konto nicht gefunden.");
    if (acc.get("letterGift") === true) return { granted: 0 };
    tx.update(ref, { letterGift: true, letters: FieldValue.increment(LETTER_PACK) });
    return { granted: LETTER_PACK };
  });
});

exports.buyLetters = onCall(async (req) => {
  const uid = requireActive(req);
  const ref = db.doc(`accounts/${uid}`);
  return db.runTransaction(async (tx) => {
    const acc = await tx.get(ref);
    if (!acc.exists) throw new HttpsError("not-found", "Konto nicht gefunden.");
    const gems = acc.get("gems") || 0;
    if (gems < LETTER_PACK_COST) {
      throw new HttpsError("resource-exhausted", `Nicht genug Gems – ${LETTER_PACK} Briefe kosten ${LETTER_PACK_COST} 💎.`);
    }
    const letters = (acc.get("letters") || 0) + LETTER_PACK;
    tx.update(ref, { gems: gems - LETTER_PACK_COST, letters });
    return { gems: gems - LETTER_PACK_COST, letters };
  });
});

// ---- Gems ----------------------------------------------------------------

exports.checkIn = onCall(async (req) => {
  const uid = requireActive(req);
  const today = todayBerlin();
  const yesterday = shiftDay(today, -1);
  const ref = db.doc(`accounts/${uid}`);
  return db.runTransaction(async (tx) => {
    const acc = await tx.get(ref);
    if (!acc.exists) throw new HttpsError("not-found", "Konto nicht gefunden.");
    if (acc.get("lastCheckIn") === today) {
      throw new HttpsError("already-exists", "Du hast heute schon eingecheckt. Bis morgen! 👋");
    }
    const streak = acc.get("lastCheckIn") === yesterday ? (acc.get("streak") || 0) + 1 : 1;
    // kumi+: doppelte Belohnung
    const reward = (CHECKIN_BASE + Math.min(streak - 1, 5) * STREAK_BONUS) * (isPlus(acc) ? 2 : 1);
    const letters = ((streak - 1) % CHECKIN_CYCLE) + 1 === LETTER_DAY ? 1 : 0;
    const update = { lastCheckIn: today, streak, gems: FieldValue.increment(reward) };
    if (letters) update.letters = FieldValue.increment(letters);
    tx.update(ref, update);
    return { reward, streak, letters };
  });
});

// ---- Profilfotos & Video (mit automatischer Prüfung) ----------------------

// Bilder ändern sich nie (jedes bekommt eine neue Adresse) – darf lange zwischengespeichert werden
const IMMUTABLE_CACHE = "private, max-age=31536000, immutable";

exports.addProfileMedia = onCall({ timeoutSeconds: 300, memory: "1GiB", cpu: 1 }, async (req) => {
  // Vorwärmen: Die App ruft das auf, während man noch ein Foto aussucht
  if (req.data && req.data.warmup === true) {
    requireAuth(req);
    await warmUp();
    return { ok: true };
  }

  const uid = requireActive(req);
  const path = str(req.data && req.data.path, 300);
  const kind = req.data && req.data.kind === "video" ? "video" : "photo";
  if (!path.startsWith(`uploads/${uid}/`) || path.includes("..")) {
    throw new HttpsError("invalid-argument", "Ungültige Datei.");
  }

  const bucket = getStorage().bucket();
  const file = bucket.file(path);
  let meta;
  try {
    [meta] = await file.getMetadata();
  } catch (e) {
    throw new HttpsError("not-found", "Upload nicht gefunden. Bitte versuch es noch einmal.");
  }
  const type = String(meta.contentType || "");
  const size = Number(meta.size || 0);
  const fits = kind === "photo"
    ? type.startsWith("image/") && size <= 10 * 1024 * 1024
    : type.startsWith("video/") && size <= 40 * 1024 * 1024;
  if (!fits) {
    await file.delete().catch(() => {});
    throw new HttpsError("invalid-argument", "Dateityp oder Größe passt nicht.");
  }

  const userRef = db.doc(`users/${uid}`);
  if (kind === "photo") {
    const u = await userRef.get();
    if ((u.get("photos") || []).length >= MAX_PHOTOS) {
      await file.delete().catch(() => {});
      throw new HttpsError("resource-exhausted", `Du kannst höchstens ${MAX_PHOTOS} Fotos haben.`);
    }
  }

  let safe;
  try {
    if (kind === "photo") {
      safe = await isStoredImageSafe(file, req.auth.token.ageGroup, (m, e) => logger.warn(m, e));
    } else {
      safe = await isVideoSafe(`gs://${bucket.name}/${path}`);
    }
  } catch (e) {
    logger.error("Automatische Prüfung fehlgeschlagen", e);
    await file.delete().catch(() => {});
    throw new HttpsError("unavailable",
      "Die automatische Prüfung ist gerade nicht erreichbar. Bitte versuch es später noch einmal.");
  }
  if (!safe) {
    await file.delete().catch(() => {});
    await db.collection("moderationLog").add({ uid, kind: `profile_${kind}`, at: FieldValue.serverTimestamp() });
    throw new HttpsError("permission-denied", "Das verstößt gegen unsere Regeln und wurde nicht hochgeladen.");
  }

  let ext = kind === "photo" ? "jpg" : String(path.split(".").pop() || "").toLowerCase();
  if (!/^[a-z0-9]{2,5}$/.test(ext)) ext = "mp4";
  const destPath = `media/${uid}/${crypto.randomUUID()}.${ext}`;
  await file.move(destPath);
  const dest = bucket.file(destPath);
  await dest.setMetadata({ cacheControl: IMMUTABLE_CACHE }).catch(() => {});
  const url = await downloadUrl(dest);

  if (kind === "photo") {
    await db.runTransaction(async (tx) => {
      const u = await tx.get(userRef);
      const photos = u.get("photos") || [];
      if (photos.length >= MAX_PHOTOS) {
        throw new HttpsError("resource-exhausted", `Du kannst höchstens ${MAX_PHOTOS} Fotos haben.`);
      }
      tx.update(userRef, { photos: [...photos, url] });
    });
  } else {
    const accRef = db.doc(`accounts/${uid}`);
    const oldPath = (await accRef.get()).get("videoPath");
    if (oldPath) await bucket.file(oldPath).delete().catch(() => {});
    await userRef.update({ videoUrl: url });
    await accRef.update({ videoPath: destPath });
  }
  return { url };
});

exports.removeProfileMedia = onCall(async (req) => {
  const uid = requireAuth(req);
  const url = str(req.data && req.data.url, 2000);
  const path = pathFromDownloadUrl(url);
  if (!path.startsWith(`media/${uid}/`)) throw new HttpsError("invalid-argument", "Ungültige Datei.");
  const userRef = db.doc(`users/${uid}`);
  const u = await userRef.get();
  if ((u.get("photos") || []).includes(url)) {
    await userRef.update({ photos: FieldValue.arrayRemove(url) });
  } else if (u.get("videoUrl") === url) {
    await userRef.update({ videoUrl: "" });
    await db.doc(`accounts/${uid}`).update({ videoPath: "" });
  } else {
    throw new HttpsError("not-found", "Datei nicht gefunden.");
  }
  await getStorage().bucket().file(path).delete().catch(() => {});
  return { ok: true };
});

// ---- Lounge (globaler Chatraum je Altersgruppe) ---------------------------

exports.postRoomMessage = onCall(async (req) => {
  const uid = requireActive(req);
  const group = req.auth.token.ageGroup;
  const text = str(req.data && req.data.text, 500);
  if (!text) throw new HttpsError("invalid-argument", "Die Nachricht ist leer.");

  const slowRef = db.doc(`rooms/${group}/slowmode/${uid}`);
  await db.runTransaction(async (tx) => {
    const s = await tx.get(slowRef);
    const last = s.exists && s.get("lastAt") ? s.get("lastAt").toMillis() : 0;
    const wait = SLOW_MODE_MS - (Date.now() - last);
    if (wait > 0) {
      throw new HttpsError("resource-exhausted", `Slow-Mode: Warte noch ${Math.ceil(wait / 1000)} Sekunden.`);
    }
    tx.set(slowRef, { lastAt: Timestamp.now() });
  });

  const me = await db.doc(`users/${uid}`).get();
  const cleaned = clean(text);
  await db.collection(`rooms/${group}/messages`).add({
    senderId: uid,
    name: me.get("name") || "",
    photo: (me.get("photos") || [])[0] || "",
    color: me.get("color") || DEFAULT_COLOR,
    text: cleaned.text,
    flagged: cleaned.flagged,
    createdAt: FieldValue.serverTimestamp(),
  });
  return { ok: true };
});

// ---- Melden & Blockieren --------------------------------------------------

exports.report = onCall(async (req) => {
  const uid = requireActive(req);
  const targetUid = str(req.data && req.data.targetUid, 128);
  if (!isSafeId(targetUid) || targetUid === uid) throw new HttpsError("invalid-argument", "Ungültige Person.");
  const target = await db.doc(`users/${targetUid}`).get();
  if (!target.exists) throw new HttpsError("not-found", "Profil nicht gefunden.");
  const group = target.get("ageGroup");

  await db.collection("reports").add({
    reporter: uid,
    target: targetUid,
    targetName: target.get("name") || "",
    reason: str(req.data.reason, 80) || "Sonstiges",
    details: str(req.data.details, 1000),
    context: str(req.data.context, 1000),
    ageGroup: group,
    priority: group === "teen" ? 1 : 2, // Meldungen aus der Jugend-Community zuerst
    status: "open",
    createdAt: FieldValue.serverTimestamp(),
  });

  // Sicherheitsnetz: Melden mehrere verschiedene Personen dasselbe Profil,
  // wird es bis zur Prüfung durch einen Admin versteckt.
  const open = await db.collection("reports").where("target", "==", targetUid).where("status", "==", "open").get();
  const reporters = new Set(open.docs.map((d) => d.get("reporter")));
  if (reporters.size >= AUTO_HIDE_REPORTS && target.get("active") === true) {
    await target.ref.update({ active: false, hiddenForReview: true });
  }
  return { ok: true };
});

exports.blockUser = onCall(async (req) => {
  const uid = requireActive(req);
  const target = str(req.data && req.data.targetUid, 128);
  if (!isSafeId(target) || target === uid) throw new HttpsError("invalid-argument", "Ungültige Person.");

  // Name und Bild merken, damit die Liste "Blockierte Nutzer" sie auch später zeigen kann
  const targetSnap = await db.doc(`users/${target}`).get();
  const info = targetSnap.exists ? memberInfo(targetSnap) : { name: "", photo: "", color: DEFAULT_COLOR };

  const batch = db.batch();
  batch.set(db.doc(`users/${uid}/blocked/${target}`), { at: FieldValue.serverTimestamp(), ...info });
  batch.set(db.doc(`users/${uid}/seen/${target}`), { at: FieldValue.serverTimestamp(), liked: false });
  batch.delete(db.doc(`users/${uid}/friends/${target}`));
  batch.delete(db.doc(`users/${target}/friends/${uid}`));
  batch.delete(db.doc(`requests/${uid}_${target}`));
  batch.delete(db.doc(`requests/${target}_${uid}`));
  await batch.commit();

  const chatRef = db.doc(`chats/${pairId(uid, target)}`);
  if ((await chatRef.get()).exists) await chatRef.update({ closed: true });
  return { ok: true };
});

// Entblocken: jederzeit möglich. Die Person kann einen dann wieder finden und anfragen;
// eine frühere Freundschaft wird nicht automatisch wiederhergestellt.
exports.unblockUser = onCall(async (req) => {
  const uid = requireAuth(req);
  const target = str(req.data && req.data.targetUid, 128);
  if (!isSafeId(target) || target === uid) throw new HttpsError("invalid-argument", "Ungültige Person.");

  const batch = db.batch();
  batch.delete(db.doc(`users/${uid}/blocked/${target}`));
  // Beim Blockieren als "gesehen" markiert – damit die Person im Entdecken wieder auftauchen kann
  batch.delete(db.doc(`users/${uid}/seen/${target}`));
  await batch.commit();
  return { ok: true };
});

// ---- Käufe (Google Play) ----------------------------------------------------

function monthSpent(acc, month) {
  const s = (acc && acc.get("spend")) || {};
  return s.month === month ? Number(s.cents) || 0 : 0;
}

function playFailure(e) {
  logger.error("Google-Play-Abfrage fehlgeschlagen", { status: e.status, message: e.message });
  if (e.status === 401 || e.status === 403) {
    return new HttpsError("failed-precondition",
      "Käufe sind auf dem Server noch nicht freigeschaltet. Keine Sorge: Der Kauf wird beim nächsten Start gutgeschrieben.");
  }
  if (e.status === 400 || e.status === 404 || e.status === 410) {
    return new HttpsError("not-found", "Diesen Kauf kennt Google nicht.");
  }
  return new HttpsError("unavailable", "Google Play ist gerade nicht erreichbar. Der Kauf wird beim nächsten Start nachgeholt.");
}

// Vor dem Kauf: Produkt gültig? Für 13–17 das Monatslimit prüfen.
exports.purchaseCheck = onCall(async (req) => {
  const uid = requireActive(req);
  const productId = str(req.data && req.data.productId, 64);
  const cents = billing.priceCents(productId);
  if (cents === null) throw new HttpsError("invalid-argument", "Unbekanntes Produkt.");
  if (req.auth.token.ageGroup === "teen") {
    const acc = await db.doc(`accounts/${uid}`).get();
    const spent = monthSpent(acc, todayBerlin().slice(0, 7));
    if (spent + cents > billing.TEEN_MONTHLY_LIMIT_CENTS) {
      const limit = (billing.TEEN_MONTHLY_LIMIT_CENTS / 100).toFixed(0);
      throw new HttpsError("failed-precondition",
        `Unter 18 kannst du bei kumi höchstens ${limit} € im Monat ausgeben. Nächsten Monat geht es wieder.`);
    }
  }
  return { ok: true, accountTag: billing.accountTag(uid) };
});

// Nach dem Kauf: bei Google prüfen, dann Gems gutschreiben bzw. kumi+ freischalten.
// Jeder Kauf wird nur einmal gutgeschrieben (Dokument purchases/{Hash des Tokens}).
exports.verifyPurchase = onCall(async (req) => {
  const uid = requireAuth(req);
  const productId = str(req.data && req.data.productId, 64);
  const token = str(req.data && req.data.purchaseToken, 2000);
  if (token.length < 10) throw new HttpsError("invalid-argument", "Ungültiger Kauf.");
  const pkg = PLAY_PACKAGE.value();
  const tag = billing.accountTag(uid);
  const month = todayBerlin().slice(0, 7);
  const docRef = db.doc(`purchases/${billing.tokenKey(token)}`);
  const accRef = db.doc(`accounts/${uid}`);

  const pack = billing.GEM_PACKS[productId];
  if (pack) {
    let p;
    try {
      p = await billing.getProductPurchase(pkg, productId, token);
    } catch (e) {
      throw playFailure(e);
    }
    if (p.purchaseState === 2) return { status: "pending" };
    if (p.purchaseState !== 0) throw new HttpsError("failed-precondition", "Dieser Kauf wurde storniert.");
    if (p.obfuscatedExternalAccountId && p.obfuscatedExternalAccountId !== tag) {
      throw new HttpsError("permission-denied", "Dieser Kauf gehört zu einem anderen Konto.");
    }
    const qty = Math.max(1, Number(p.quantity) || 1);
    const credited = await db.runTransaction(async (tx) => {
      const [doc, acc] = await Promise.all([tx.get(docRef), tx.get(accRef)]);
      if (doc.exists) return 0;
      if (!acc.exists) throw new HttpsError("not-found", "Konto nicht gefunden.");
      const gems = pack.gems * qty;
      const cents = pack.cents * qty;
      tx.set(docRef, {
        uid, productId, kind: "gems", gems, cents, orderId: p.orderId || "", at: FieldValue.serverTimestamp(),
      });
      tx.update(accRef, { gems: FieldValue.increment(gems), spend: { month, cents: monthSpent(acc, month) + cents } });
      return gems;
    });
    // Verbrauchen, damit das Paket wieder gekauft werden kann
    if (p.consumptionState !== 1) {
      await billing.consumeProduct(pkg, productId, token)
        .catch((e) => logger.warn("Kauf konnte nicht verbraucht werden", { status: e.status, message: e.message }));
    }
    return { status: "ok", gems: credited };
  }

  if (productId === billing.PLUS_PRODUCT) {
    let sub;
    try {
      sub = await billing.getSubscription(pkg, token);
    } catch (e) {
      throw playFailure(e);
    }
    const ext = sub.externalAccountIdentifiers || {};
    if (ext.obfuscatedExternalAccountId && ext.obfuscatedExternalAccountId !== tag) {
      throw new HttpsError("permission-denied", "Dieses Abo gehört zu einem anderen Konto.");
    }
    if (sub.subscriptionState === "SUBSCRIPTION_STATE_PENDING") return { status: "pending" };
    const expiry = Math.max(0, ...(sub.lineItems || [])
      .filter((i) => i.productId === billing.PLUS_PRODUCT)
      .map((i) => Date.parse(i.expiryTime) || 0));
    const active = [
      "SUBSCRIPTION_STATE_ACTIVE",
      "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
      "SUBSCRIPTION_STATE_CANCELED", // gekündigt, läuft aber noch bis zum Ende
    ].includes(sub.subscriptionState) && expiry > Date.now();

    await db.runTransaction(async (tx) => {
      const [doc, acc] = await Promise.all([tx.get(docRef), tx.get(accRef)]);
      if (!acc.exists) throw new HttpsError("not-found", "Konto nicht gefunden.");
      const update = { plus: active, plusUntil: Timestamp.fromMillis(expiry || Date.now()) };
      if (!doc.exists) {
        tx.set(docRef, {
          uid, productId, kind: "plus", cents: billing.PLUS_CENTS, orderId: sub.latestOrderId || "",
          at: FieldValue.serverTimestamp(),
        });
        update.spend = { month, cents: monthSpent(acc, month) + billing.PLUS_CENTS };
      }
      tx.update(accRef, update);
      tx.set(db.doc(`users/${uid}`), { plus: active }, { merge: true });
    });
    if (sub.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING") {
      await billing.acknowledgeSubscription(pkg, billing.PLUS_PRODUCT, token)
        .catch((e) => logger.warn("Abo konnte nicht bestätigt werden", { status: e.status, message: e.message }));
    }
    return { status: active ? "ok" : "expired", plusUntil: expiry };
  }

  throw new HttpsError("invalid-argument", "Unbekanntes Produkt.");
});

// ---- Konto löschen --------------------------------------------------------

async function deleteUserData(uid) {
  const bucket = getStorage().bucket();

  const chats = await db.collection("chats").where("members", "array-contains", uid).get();
  for (const c of chats.docs) {
    const other = (c.get("members") || []).find((m) => m !== uid);
    if (other) await db.doc(`users/${other}/friends/${uid}`).delete().catch(() => {});
    await db.recursiveDelete(c.ref);
  }

  const [sent, received, consents, teenRoom, adultRoom, acc] = await Promise.all([
    db.collection("requests").where("from", "==", uid).get(),
    db.collection("requests").where("to", "==", uid).get(),
    db.collection("consents").where("uid", "==", uid).get(),
    db.collection("rooms/teen/messages").where("senderId", "==", uid).get(),
    db.collection("rooms/adult/messages").where("senderId", "==", uid).get(),
    db.doc(`accounts/${uid}`).get(),
  ]);
  const w = db.bulkWriter();
  [sent, received, consents, teenRoom, adultRoom].forEach((q) => q.forEach((d) => w.delete(d.ref)));
  if (acc.exists && acc.get("inviteCode")) w.delete(db.doc(`inviteCodes/${acc.get("inviteCode")}`));
  await w.close();

  await db.recursiveDelete(db.doc(`users/${uid}`));
  await db.recursiveDelete(db.doc(`accounts/${uid}`));
  for (const prefix of [`media/${uid}/`, `uploads/${uid}/`, `chatUploads/${uid}/`]) {
    await bucket.deleteFiles({ prefix }).catch(() => {});
  }
  await getAuth().deleteUser(uid).catch(() => {});
}

exports.deleteAccount = onCall({ timeoutSeconds: 300 }, async (req) => {
  const uid = requireAuth(req);
  await deleteUserData(uid);
  return { ok: true };
});

// ---- Automatik: Chat-Nachrichten -----------------------------------------

exports.onMessageCreated = onDocumentCreated(
  { document: "chats/{chatId}/messages/{msgId}", timeoutSeconds: 120, memory: "512MiB", cpu: 1 },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const msg = snap.data();
    const chatRef = db.doc(`chats/${event.params.chatId}`);
    const chatSnap = await chatRef.get();
    if (!chatSnap.exists) return;
    const chat = chatSnap.data();

    let preview;
    if (msg.type === "image") {
      let ok = false;
      let url = "";
      const path = msg.imagePath;
      if (typeof path === "string" && path.startsWith(`chatUploads/${msg.senderId}/`)) {
        try {
          const file = getStorage().bucket().file(path);
          ok = await isStoredImageSafe(file, chat.ageGroup, (m, e) => logger.warn(m, e));
          if (ok) {
            await file.setMetadata({ cacheControl: IMMUTABLE_CACHE }).catch(() => {});
            url = await downloadUrl(file);
          } else {
            await file.delete().catch(() => {});
            await db.collection("moderationLog").add({
              uid: msg.senderId, kind: "chat_image", chatId: event.params.chatId, at: FieldValue.serverTimestamp(),
            });
          }
        } catch (e) {
          logger.error("Bildprüfung im Chat fehlgeschlagen", e);
          ok = false;
        }
      }
      await snap.ref.update({ imageStatus: ok ? "ok" : "blocked", imageUrl: url });
      preview = ok ? "📷 Foto" : "📷 Foto (blockiert)";
    } else if (msg.type === "audio") {
      // Sprachnachricht: Datei prüfen (Typ, Größe) und dann zum Abspielen freigeben
      let ok = false;
      let url = "";
      const path = msg.audioPath;
      if (typeof path === "string" && path.startsWith(`chatUploads/${msg.senderId}/`) && path.endsWith(".m4a")) {
        try {
          const file = getStorage().bucket().file(path);
          const [meta] = await file.getMetadata();
          const size = Number(meta.size || 0);
          if (String(meta.contentType || "").startsWith("audio/") && size > 0 && size <= 5 * 1024 * 1024) {
            url = await downloadUrl(file);
            ok = true;
          } else {
            await file.delete().catch(() => {});
          }
        } catch (e) {
          logger.error("Sprachnachricht konnte nicht geprüft werden", e);
        }
      }
      await snap.ref.update({ audioStatus: ok ? "ok" : "blocked", audioUrl: url });
      preview = "🎤 Sprachnachricht";
    } else {
      const c = clean(msg.text || "");
      if (c.flagged) await snap.ref.update({ text: c.text, flagged: true });
      preview = c.text.slice(0, 80);
    }

    await chatRef.update({ lastMessage: preview, lastAt: FieldValue.serverTimestamp(), lastSender: msg.senderId });
    const other = (chat.members || []).find((m) => m !== msg.senderId);
    if (!other) return;

    // Schreibt jemand dem Test-Bot, antwortet er kurz darauf
    if (other.startsWith("turp-bot-")) {
      if (chat.closed) return;
      await new Promise((r) => setTimeout(r, 1200));
      await botSay(event.params.chatId, other, botReply(msg, preview));
      return;
    }

    const senderName = (chat.memberInfo && chat.memberInfo[msg.senderId] && chat.memberInfo[msg.senderId].name) || "kumi";
    if (chat.pending === true) {
      await notify(other, "Neuer Brief ✉️", "{name} hat dir einen Brief geschrieben. Lies ihn und entscheide, ob ihr Freunde werdet.",
        { type: "message", chatId: event.params.chatId }, { name: senderName });
      return;
    }
    await notify(other, senderName, preview, { type: "message", chatId: event.params.chatId });
  });

// ---- Automatik: Profiländerungen ------------------------------------------

exports.onProfileWritten = onDocumentWritten("users/{uid}", async (event) => {
  const after = event.data && event.data.after;
  if (!after || !after.exists) return;
  const d = after.data();
  const beforeSnap = event.data.before;
  const before = beforeSnap && beforeSnap.exists ? beforeSnap.data() : {};

  // Wortfilter für Name und Bio
  const updates = {};
  for (const f of ["name", "bio"]) {
    if (typeof d[f] === "string" && d[f] !== before[f]) {
      const c = clean(d[f]);
      if (c.flagged) updates[f] = c.text;
    }
  }
  // Nur Interessen aus dem Katalog – kein freier Text, der ungeprüft im Profil steht
  if (JSON.stringify(d.interests) !== JSON.stringify(before.interests)) {
    const list = Array.isArray(d.interests) ? d.interests : [];
    const ok = [...new Set(list.filter((i) => typeof i === "string" && ALLOWED_INTERESTS.has(i)))].slice(0, MAX_INTERESTS);
    if (JSON.stringify(ok) !== JSON.stringify(list)) updates.interests = ok;
  }
  if (Object.keys(updates).length > 0) await after.ref.update(updates);

  // Name/Foto/Farbe in bestehenden Chats aktualisieren
  if (!beforeSnap || !beforeSnap.exists) return;
  const changed = ["name", "photos", "color"].some((f) => JSON.stringify(d[f]) !== JSON.stringify(before[f]));
  if (!changed) return;
  const info = { name: updates.name || d.name || "", photo: (d.photos || [])[0] || "", color: d.color || DEFAULT_COLOR };
  const chats = await db.collection("chats").where("members", "array-contains", event.params.uid).get();
  const w = db.bulkWriter();
  chats.forEach((c) => w.update(c.ref, { [`memberInfo.${event.params.uid}`]: info }));
  await w.close();
});

// ---- Automatik: täglich um 3 Uhr ------------------------------------------

async function moveToAdult(uid) {
  await setClaims(uid, { ageGroup: "adult" });
  await db.doc(`accounts/${uid}`).update({ ageGroup: "adult" });
  await db.doc(`users/${uid}`).update({ ageGroup: "adult" });

  // Bestehende Kontakte zu Minderjährigen werden geschlossen
  const [chats, sent, received] = await Promise.all([
    db.collection("chats").where("members", "array-contains", uid).get(),
    db.collection("requests").where("from", "==", uid).get(),
    db.collection("requests").where("to", "==", uid).get(),
  ]);
  const w = db.bulkWriter();
  chats.forEach((c) => {
    const other = (c.get("members") || []).find((m) => m !== uid);
    w.update(c.ref, { closed: true, closedReason: "age" });
    if (other) {
      w.delete(db.doc(`users/${uid}/friends/${other}`));
      w.delete(db.doc(`users/${other}/friends/${uid}`));
    }
  });
  [sent, received].forEach((q) => q.forEach((d) => w.delete(d.ref)));
  await w.close();

  await notify(uid, "Happy Birthday! 🎂", "Du bist jetzt 18 und wechselst in die Erwachsenen-Community von kumi.");
}

exports.dailyMaintenance = onSchedule(
  { schedule: "every day 03:00", timeZone: "Europe/Berlin", timeoutSeconds: 540 },
  async () => {
    const today = todayBerlin();
    const year = Number(today.slice(0, 4));
    const monthDay = today.slice(5);
    const monthDays = [monthDay];
    if (monthDay === "02-28" && !isLeapYear(year)) monthDays.push("02-29");

    // Geburtstage: Alter aktualisieren, mit 16 freischalten, mit 18 Gruppe wechseln
    const birthdays = await db.collection("accounts").where("birthMonthDay", "in", monthDays).get();
    for (const doc of birthdays.docs) {
      try {
        const acc = doc.data();
        const refDate = acc.birthMonthDay === "02-29" && monthDay === "02-28" ? `${year}-03-01` : today;
        const age = ageOn(acc.birthDate, refDate);
        await db.doc(`users/${doc.id}`).update({ age });
        if (acc.status === "pending_consent" && age >= 16) {
          await setClaims(doc.id, { active: true });
          await doc.ref.update({ status: "active" });
          await db.doc(`users/${doc.id}`).update({ active: true });
        }
        if (acc.ageGroup === "teen" && age >= 18) await moveToAdult(doc.id);
      } catch (e) {
        logger.error("Geburtstag konnte nicht verarbeitet werden", { uid: doc.id, error: e.message });
      }
    }

    // Abgelaufene kumi+-Abos beenden (verlängerte Abos meldet die App beim nächsten Start neu)
    const plusAccounts = await db.collection("accounts").where("plus", "==", true).get();
    for (const doc of plusAccounts.docs) {
      const until = doc.get("plusUntil");
      if (until && until.toMillis() < Date.now()) {
        await doc.ref.update({ plus: false }).catch(() => {});
        await db.doc(`users/${doc.id}`).update({ plus: false }).catch(() => {});
      }
    }

    // Lounge-Nachrichten älter als 7 Tage löschen
    const cutoff = Timestamp.fromMillis(Date.now() - 7 * DAY_MS);
    for (const g of ["teen", "adult"]) {
      const old = await db.collection(`rooms/${g}/messages`).where("createdAt", "<", cutoff).limit(2000).get();
      const w = db.bulkWriter();
      old.forEach((d) => w.delete(d.ref));
      await w.close();
    }
  });

// ---- Anmelden mit Google ----------------------------------------------------

// Web-Client-ID für „Mit Google“. Steht in google-services.json, sobald Google-Login
// in Firebase eingeschaltet ist – so klappt es auch ohne neue App-Version.
let googleWebClient = "";
exports.googleClientId = onCall(async () => {
  if (googleWebClient) return { clientId: googleWebClient };
  try {
    const project = process.env.GCLOUD_PROJECT || JSON.parse(process.env.FIREBASE_CONFIG || "{}").projectId;
    const headers = { Authorization: `Bearer ${await accessToken()}` };
    const apps = await (await fetch(`https://firebase.googleapis.com/v1beta1/projects/${project}/androidApps`, { headers })).json();
    const app = (apps.apps || []).find((a) => a.packageName === "app.turp");
    if (!app) return { clientId: "" };
    const cfg = await (await fetch(`https://firebase.googleapis.com/v1beta1/${app.name}/config`, { headers })).json();
    const json = JSON.parse(Buffer.from(cfg.configFileContents || "", "base64").toString("utf8") || "{}");
    const clients = (((json.client || [])[0] || {}).oauth_client) || [];
    const web = clients.find((c) => c.client_type === 3);
    googleWebClient = web ? web.client_id : "";
  } catch (e) {
    logger.warn("Google-Client-ID nicht gefunden", e.message);
  }
  return { clientId: googleWebClient };
});
