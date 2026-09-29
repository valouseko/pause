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
// - sessionGap 0 = intervence při KAŽDÉM vstupu (klik na lištu, alt+tab), i když appka běží
// - nevyřízená intervence (ani "Pokračovat", ani "Rozmyslel jsem si to") se vrací,
//   dokud ji nevyřídíš, takže nejde obejít přepnutím okna
const GRACE_AFTER_CONTINUE_MS = 4000;

class SessionTracker {
  constructor() {
    this.lastSeen = new Map(); // targetId -> timestamp posledního výskytu v popředí
    this.pending = new Set(); // targetId se zásahem, který ještě nikdo nevyřídil
    this.graceUntil = new Map(); // targetId -> do kdy po "Pokračovat" nezasahovat (návrat fokusu)
    this.activeId = null; // cíl v popředí při minulém vyhodnocení
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
    // Návrat z něj do cíle se ale počítá jako nový vstup.
    if (isSelf) {
      this.activeId = null;
      return { intervene: false, target: null };
    }

    const target = matchTarget(win, targets);
    if (!target) {
      this.activeId = null;
      return { intervene: false, target: null };
    }

    const entering = this.activeId !== target.id;
    this.activeId = target.id;
    const last = this.lastSeen.get(target.id);
    const gapMs = Math.max(0, Number(target.sessionGapSec ?? 45)) * 1000;

    let intervene;
    if (this.pending.has(target.id)) intervene = true;
    else if (now < (this.graceUntil.get(target.id) || 0)) intervene = false;
    else if (gapMs === 0) intervene = entering || last == null;
    else intervene = last == null || now - last > gapMs;

    // Osvěžíme čas, dokud jsme v cíli (i po intervenci), aby se neopakovala.
    this.lastSeen.set(target.id, now);
    if (intervene) this.pending.add(target.id);
    return { intervene, target };
  }

  // "Pokračovat": sezení je platné od teď, krátká ochrana, než se fokus vrátí do cíle.
  markContinued(targetId, now) {
    this.pending.delete(targetId);
    this.lastSeen.set(targetId, now);
    this.graceUntil.set(targetId, now + GRACE_AFTER_CONTINUE_MS);
    this.activeId = targetId;
  }

  // "Rozmyslel jsem si to": žádný cooldown, další vstup do cíle zastaví znovu.
  markAbandoned(targetId) {
    this.pending.delete(targetId);
    this.lastSeen.delete(targetId);
    this.graceUntil.delete(targetId);
    this.activeId = null;
  }
}

module.exports = { getActiveWindow, matchTarget, SessionTracker };
