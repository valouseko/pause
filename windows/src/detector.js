'use strict';

// Detekce aktivního okna + logika, kdy zasáhnout.
// get-windows je ESM, takže ho načítáme dynamickým importem.

const path = require('path');

let _activeWindow = null;
async function getActiveWindow() {
  if (!_activeWindow) {
    const mod = await import('get-windows');
    _activeWindow = mod.activeWindow;
  }
  try {
    return await _activeWindow();
  } catch (e) {
    return null;
  }
}

function basename(p) {
  if (!p) return '';
  try {
    return path.basename(p);
  } catch (e) {
    return '';
  }
}

// Vrátí cíl (target), kterému odpovídá aktivní okno, jinak null.
function matchTarget(win, targets) {
  if (!win) return null;
  const title = String(win.title || '').toLowerCase();
  const owner = win.owner || {};
  const proc = String(owner.name || '').toLowerCase();
  const exe = basename(owner.path || '').toLowerCase();

  for (const t of targets) {
    if (!t.enabled) continue;
    const procPatterns = (t.match && t.match.process) || [];
    for (const p of procPatterns) {
      const needle = String(p || '').toLowerCase().trim();
      if (!needle) continue;
      if (proc.includes(needle) || exe.includes(needle)) return t;
    }
    const titlePatterns = (t.match && t.match.title) || [];
    for (const tp of titlePatterns) {
      const needle = String(tp || '').toLowerCase().trim();
      if (!needle) continue;
      if (title && title.includes(needle)) return t;
    }
  }
  return null;
}

// Sleduje "sezení" v aplikacích, aby se okno intervence neukazovalo pořád dokola.
// - když poprvé vstoupíš do cíle -> intervence
// - dokud jsi v něm nebo se vrátíš do sessionGap sekund -> žádná další intervence
// - když jsi pryč dýl než sessionGap a vrátíš se -> zase intervence
class SessionTracker {
  constructor() {
    this.lastSeen = new Map(); // targetId -> timestamp posledního výskytu v popředí
    this.suspended = false; // když běží overlay, mrazíme rozhodování
  }

  suspend() {
    this.suspended = true;
  }

  resume() {
    this.suspended = false;
  }

  // Vrátí { intervene: bool, target } pro dané okno.
  evaluate(win, targets, now, isSelf) {
    if (this.suspended) return { intervene: false, target: null };
    // Vlastní okno (overlay/dashboard) ignorujeme, ať se netriggerujeme sami.
    if (isSelf) return { intervene: false, target: null };

    const target = matchTarget(win, targets);
    if (!target) return { intervene: false, target: null };

    const last = this.lastSeen.get(target.id);
    const gapMs = (target.sessionGapSec || 45) * 1000;
    let intervene = false;
    if (last == null || now - last > gapMs) {
      intervene = true;
    }
    // Osvěžíme čas, dokud jsme v cíli (i po intervenci), aby se neopakovala.
    this.lastSeen.set(target.id, now);
    return { intervene, target };
  }

  // Po dokončení intervence označíme sezení jako aktivní ke "now".
  markHandled(targetId, now) {
    this.lastSeen.set(targetId, now);
  }
}

module.exports = { getActiveWindow, matchTarget, SessionTracker };
