"use strict";

// Automatische Prüfung von Bildern (Cloud Vision SafeSearch) und
// Videos (Video Intelligence, Erkennung expliziter Inhalte).
// Die Bibliotheken werden erst bei Bedarf geladen, damit andere
// Funktionen schneller starten.

const LIKELY = new Set(["LIKELY", "VERY_LIKELY", 4, 5]);
const VERY_LIKELY = new Set(["VERY_LIKELY", 5]);

let visionClient = null;
let videoClient = null;

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
  if (LIKELY.has(s.adult) || LIKELY.has(s.violence)) return false;
  // In der Jugend-Community strenger: auch sehr wahrscheinlich anzügliche Bilder blockieren
  if (ageGroup === "teen" && VERY_LIKELY.has(s.racy)) return false;
  return true;
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

module.exports = { isImageSafe, isVideoSafe };
