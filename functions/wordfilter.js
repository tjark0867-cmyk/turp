"use strict";

// Einfacher Wortfilter für Namen, Bios, Chat- und Lounge-Nachrichten.
// Einträge mit * am Ende treffen auch Wortformen (fick* -> ficken, fickt ...).
// Die Liste lässt sich jederzeit erweitern.
const WORDS = [
  // Deutsch
  "arschloch*", "wichser*", "hurensohn*", "hurensöhne*", "hure*", "nutte*", "fotze*", "schlampe*",
  "missgeburt*", "spast*", "spasti*", "fick*", "gefickt", "bastard*", "pisser*", "schwuchtel*",
  "kanake*", "neger*", "titten*", "nacktbild*", "nacktfoto*", "bring dich um", "töte dich",
  "verpiss dich", "halt die fresse",
  // Englisch
  "fuck*", "motherfucker*", "bitch*", "asshole*", "cunt*", "pussy*", "whore*", "slut*",
  "nigger*", "nigga*", "faggot*", "retard*", "porn*", "nudes", "send nudes", "dick pic*",
  "kill yourself", "kys",
];

const LEET = { 0: "o", 1: "i", 3: "e", 4: "a", 5: "s", 7: "t", 8: "b", "@": "a", $: "s", "!": "i", "|": "i" };
const LETTER = "a-zäöüß";

function escapeRegex(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

const pattern = WORDS.map((w) => {
  const stem = w.endsWith("*");
  const base = escapeRegex(stem ? w.slice(0, -1) : w).replace(/ /g, "\\s+");
  return stem ? `${base}[${LETTER}]*` : base;
}).join("|");

const REGEX = new RegExp(`(^|[^${LETTER}])(${pattern})(?=$|[^${LETTER}])`, "giu");

function normalize(text) {
  return text.toLowerCase().replace(/[0134578@$!|]/g, (c) => LEET[c] || c);
}

/**
 * Ersetzt verbotene Wörter durch Sternchen.
 * @param {string} text
 * @returns {{text: string, flagged: boolean}}
 */
function clean(text) {
  if (typeof text !== "string" || text.length === 0) return { text: text || "", flagged: false };
  const norm = normalize(text);
  if (norm.length !== text.length) {
    // Sonderfall (z. B. Zeichen, die beim Kleinschreiben länger werden):
    // bei einem Treffer wird die ganze Nachricht ersetzt.
    REGEX.lastIndex = 0;
    return REGEX.test(norm) ? { text: "***", flagged: true } : { text, flagged: false };
  }
  const chars = text.split("");
  let flagged = false;
  REGEX.lastIndex = 0;
  let m;
  while ((m = REGEX.exec(norm)) !== null) {
    const start = m.index + m[1].length;
    const end = start + m[2].length;
    for (let i = start; i < end; i++) {
      if (!/\s/.test(chars[i])) chars[i] = "*";
    }
    flagged = true;
    if (m[0].length === 0) REGEX.lastIndex++;
  }
  return { text: chars.join(""), flagged };
}

module.exports = { clean };
