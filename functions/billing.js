"use strict";

// Käufe über Google Play: Produkte, Preise und die Abfragen bei der
// Google Play Developer API. Gutgeschrieben wird nur, was Google bestätigt.

const crypto = require("crypto");

// Gem-Pakete (Produkt-IDs wie in der Play Console). cents = Preis in Euro-Cent (inkl. MwSt.)
const GEM_PACKS = {
  gems_50: { gems: 50, cents: 99 },
  gems_150: { gems: 150, cents: 249 },
  gems_350: { gems: 350, cents: 499 },
  gems_800: { gems: 800, cents: 999 },
  gems_2000: { gems: 2000, cents: 1999 },
};

// kumi+ (Monats-Abo)
const PLUS_PRODUCT = "kumi_plus";
const PLUS_CENTS = 399;

// Schutz für 13–17: höchstens so viel pro Kalendermonat
const TEEN_MONTHLY_LIMIT_CENTS = 3000;

const SCOPE = "https://www.googleapis.com/auth/androidpublisher";
let tokenCache = null;

/** Zugangs-Token des Funktions-Dienstkontos mit Berechtigung für die Play-API */
async function playToken() {
  if (tokenCache && tokenCache.expires > Date.now() + 60_000) return tokenCache.token;
  const res = await fetch(
    "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token?scopes=" +
      encodeURIComponent(SCOPE),
    { headers: { "Metadata-Flavor": "Google" } },
  );
  if (!res.ok) throw new PlayError(500, `Token nicht erhalten (${res.status})`);
  const json = await res.json();
  tokenCache = { token: json.access_token, expires: Date.now() + (Number(json.expires_in) || 300) * 1000 };
  return tokenCache.token;
}

class PlayError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function play(method, path, body) {
  const token = await playToken();
  const res = await fetch(`https://androidpublisher.googleapis.com/androidpublisher/v3/${path}`, {
    method,
    headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let json = {};
  try {
    json = text ? JSON.parse(text) : {};
  } catch (e) {
    json = {};
  }
  if (!res.ok) throw new PlayError(res.status, (json.error && json.error.message) || text.slice(0, 300));
  return json;
}

const enc = encodeURIComponent;

function getProductPurchase(pkg, productId, token) {
  return play("GET", `applications/${enc(pkg)}/purchases/products/${enc(productId)}/tokens/${enc(token)}`);
}

function consumeProduct(pkg, productId, token) {
  return play("POST", `applications/${enc(pkg)}/purchases/products/${enc(productId)}/tokens/${enc(token)}:consume`);
}

function getSubscription(pkg, token) {
  return play("GET", `applications/${enc(pkg)}/purchases/subscriptionsv2/tokens/${enc(token)}`);
}

function acknowledgeSubscription(pkg, productId, token) {
  return play(
    "POST",
    `applications/${enc(pkg)}/purchases/subscriptions/${enc(productId)}/tokens/${enc(token)}:acknowledge`,
    {},
  );
}

/** Verschleierte Konto-Kennung für Google (kein Klartext-Nutzer-ID), 64 Zeichen */
function accountTag(uid) {
  return crypto.createHash("sha256").update(`kumi:${uid}`).digest("hex");
}

function tokenKey(token) {
  return crypto.createHash("sha256").update(token).digest("hex");
}

function priceCents(productId) {
  if (GEM_PACKS[productId]) return GEM_PACKS[productId].cents;
  if (productId === PLUS_PRODUCT) return PLUS_CENTS;
  return null;
}

module.exports = {
  GEM_PACKS,
  PLUS_PRODUCT,
  PLUS_CENTS,
  TEEN_MONTHLY_LIMIT_CENTS,
  PlayError,
  getProductPurchase,
  consumeProduct,
  getSubscription,
  acknowledgeSubscription,
  accountTag,
  tokenKey,
  priceCents,
};
