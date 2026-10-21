"use strict";

// Automatische Prüfung von Bildern (Cloud Vision SafeSearch) und
// Videos (Video Intelligence, Erkennung expliziter Inhalte).
// Die Bibliotheken werden erst bei Bedarf geladen, damit andere
// Funktionen schneller starten.

const LIKELY = new Set(["LIKELY", "VERY_LIKELY", 4, 5]);
const VERY_LIKELY = new Set(["VERY_LIKELY", 5]);

let visionClient = null;
let videoClient = null;

// ---- Schneller Weg: Vision direkt über die Web-Schnittstelle ----------------
// Die Vision-Bibliothek (gRPC) braucht beim Kaltstart oft 1–3 Sekunden zum Laden.
// Der direkte Aufruf mit dem Speicher-Pfad (gs://...) spart das und den Download.

let cachedToken = null;

/** Zugangs-Token des Funktions-Dienstkontos (vom Metadaten-Server, wird zwischengespeichert) */
async function accessToken() {
  if (cachedToken && cachedToken.expires > Date.now() + 60_000) return cachedToken.token;
  const res = await fetch(
    "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token",
    { headers: { "Metadata-Flavor": "Google" } },
  );
  if (!res.ok) throw new Error(`Token nicht erhalten (${res.status})`);
  const json = await res.json();
  cachedToken = { token: json.access_token, expires: Date.now() + (Number(json.expires_in) || 300) * 1000 };
  return cachedToken.token;
}

/** Holt schon mal das Token (für das Vorwärmen der Funktion) */
async function warmUp() {
  await accessToken().catch(() => {});
}

function verdict(s, ageGroup) {
  if (LIKELY.has(s.adult) || LIKELY.has(s.violence)) return false;
  // In der Jugend-Community strenger: auch sehr wahrscheinlich anzügliche Bilder blockieren
  if (ageGroup === "teen" && VERY_LIKELY.has(s.racy)) return false;
  return true;
}

async function safeSearchRest(gcsUri) {
  const token = await accessToken();
  const res = await fetch("https://vision.googleapis.com/v1/images:annotate", {
    method: "POST",
    headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      requests: [{ image: { source: { gcsImageUri: gcsUri } }, features: [{ type: "SAFE_SEARCH_DETECTION" }] }],
    }),
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(`Vision ${res.status}: ${(json.error && json.error.message) || ""}`);
  const r = json.responses && json.responses[0];
  if (!r) throw new Error("Leere Vision-Antwort");
  if (r.error && r.error.message) throw new Error(r.error.message);
  if (!r.safeSearchAnnotation) throw new Error("Keine SafeSearch-Antwort erhalten");
  return r.safeSearchAnnotation;
}

/**
 * Prüft ein Bild im Speicher. Erst der schnelle Weg; klappt der nicht,
 * wird das Bild heruntergeladen und mit der Bibliothek geprüft.
 * @param {import("@google-cloud/storage").File} file Datei im Storage-Bucket
 * @param {string} ageGroup "teen" oder "adult"
 * @param {(msg: string, err: any) => void} [warn]
 * @returns {Promise<boolean>} true, wenn das Bild unbedenklich ist
 */
async function isStoredImageSafe(file, ageGroup, warn) {
  try {
    const s = await safeSearchRest(`gs://${file.bucket.name}/${file.name}`);
    return verdict(s, ageGroup);
  } catch (e) {
    if (warn) warn("Schnelle Bildprüfung fehlgeschlagen, nehme den langsamen Weg", e);
    const [buf] = await file.download();
    return isImageSafe(buf, ageGroup);
  }
}

/**
 * @param {Buffer} buffer Bilddaten
 * @param {string} ageGroup "teen" oder "adult"
 * @returns {Promise<boolean>} true, wenn das Bild unbedenklich ist
 */
async function isImageSafe(buffer, ageGroup) {
  if (!visionClient) {
    const vision = require("@google-cloud/vision");
    visionClient = new vision.ImageAnnotatorClient();
  }
  const [result] = await visionClient.safeSearchDetection({ image: { content: buffer } });
  if (result.error && result.error.message) throw new Error(result.error.message);
  const s = result.safeSearchAnnotation;
  if (!s) throw new Error("Keine SafeSearch-Antwort erhalten");
  return verdict(s, ageGroup);
}

/**
 * @param {string} gcsUri gs://bucket/pfad
 * @returns {Promise<boolean>} true, wenn das Video unbedenklich ist
 */
async function isVideoSafe(gcsUri) {
  if (!videoClient) {
    const videoIntelligence = require("@google-cloud/video-intelligence");
    videoClient = new videoIntelligence.VideoIntelligenceServiceClient();
  }
  const [operation] = await videoClient.annotateVideo({
    inputUri: gcsUri,
    features: ["EXPLICIT_CONTENT_DETECTION"],
  });
  const [result] = await operation.promise();
  const annotation = result.annotationResults && result.annotationResults[0];
  if (annotation && annotation.error && annotation.error.message) throw new Error(annotation.error.message);
  const frames = (annotation && annotation.explicitAnnotation && annotation.explicitAnnotation.frames) || [];
  return !frames.some((f) => LIKELY.has(f.pornographyLikelihood));
}

module.exports = { isImageSafe, isStoredImageSafe, isVideoSafe, warmUp, accessToken };
