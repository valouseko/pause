'use strict';

// Propojení s Minutkou (časový audit v Ondrově kalendářové appce, http://127.0.0.1:8790).
// Appky s volbou "Měřit v Minutce" (target.track) hlásí jako přerušení: vstup = start,
// průběžně čas v popředí, zavření appky = konec. Konec kliknutím na křížek řeší Minutka sama.
// Když Minutka neběží, nic se neděje (Pause funguje dál samostatně).

const http = require('http');
const { matchTarget } = require('./detector');

const PING_MS = 15000;
const CLOSE_CHECK_MS = 10000;

function post(baseUrl, path, body, timeout = 4000) {
  return new Promise((resolve) => {
    let url;
    try { url = new URL(path, baseUrl); } catch (e) { return resolve(null); }
    const data = Buffer.from(JSON.stringify(body || {}));
    const req = http.request(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Content-Length': data.length },
      timeout
    }, (res) => {
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => {
        try { resolve(res.statusCode < 300 ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : null); } catch (e) { resolve(null); }
      });
    });
    req.on('timeout', () => req.destroy());
    req.on('error', () => resolve(null));
    req.end(data);
  });
}

class MinutkaLink {
  // send(path, body) -> Promise<odpověď|null>; listOpen() -> Promise<[okna]>
  constructor({ send, listOpen, now = () => Date.now() }) {
    this.send = send;
    this.listOpen = listOpen;
    this.now = now;
    this.activeId = null; // měřená appka v popředí při minulém ticku
    this.running = new Map(); // targetId -> { fgMs } (přerušení, které Minutka eviduje)
    this.lastTick = null;
    this.lastPing = 0;
    this.lastCloseCheck = 0;
  }

  tracked(targets) {
    return (targets || []).filter((t) => t.enabled && t.track);
  }

  // Srovná seznam běžících přerušení podle Minutky (uživatel mohl kliknout na křížek).
  sync(res) {
    if (!res || !Array.isArray(res.interrupts)) return;
    const keys = new Set(res.interrupts.map((i) => i.key));
    for (const key of [...this.running.keys()]) if (!keys.has(key)) this.running.delete(key);
    for (const key of keys) if (!this.running.has(key)) this.running.set(key, { fgMs: 0 });
  }

  // Volá se z hlavní smyčky Pause. blocked = právě běží zásah Pause (overlay), vstup se ještě nepočítá.
  async tick(win, targets, { isSelf = false, blocked = false } = {}) {
    const now = this.now();
    const dt = this.lastTick == null ? 0 : Math.min(5000, now - this.lastTick);
    this.lastTick = now;
    const list = this.tracked(targets);
    const target = !blocked && !isSelf ? matchTarget(win, list) : null;
    const entering = target && this.activeId !== target.id;
    if (this.activeId && this.running.has(this.activeId) && (!target || target.id === this.activeId)) {
      this.running.get(this.activeId).fgMs += dt; // čas, kdy bylo okno opravdu v popředí
    }
    this.activeId = target ? target.id : null;
    const jobs = [];
    if (entering && !this.running.has(target.id)) {
      this.running.set(target.id, { fgMs: 0 });
      jobs.push(this.send('/api/track/interrupt/start', { key: target.id, app: target.label }).then((res) => {
        if (!res) this.running.delete(target.id); // Minutka neběží, zkusíme při dalším vstupu
        else this.sync(res);
      }));
    }
    if (this.running.size && now - this.lastPing >= PING_MS) {
      this.lastPing = now;
      for (const [key, r] of this.running) {
        jobs.push(this.send('/api/track/interrupt/ping', { key, foregroundSec: Math.round(r.fgMs / 1000) }).then((res) => this.sync(res)));
      }
    }
    if (this.running.size && now - this.lastCloseCheck >= CLOSE_CHECK_MS) {
      this.lastCloseCheck = now;
      jobs.push(this.closeFinished(list));
    }
    await Promise.all(jobs);
  }

  // Appka zavřená (žádné její okno) = konec přerušení.
  async closeFinished(list) {
    let wins;
    try { wins = await this.listOpen(); } catch (e) { return; }
    if (!Array.isArray(wins)) return;
    for (const [key, r] of [...this.running]) {
      const t = list.find((x) => x.id === key);
      const open = t && wins.some((w) => matchTarget(w, [t]));
      if (open) continue;
      this.running.delete(key);
      if (this.activeId === key) this.activeId = null;
      await this.send('/api/track/interrupt/stop', { key, reason: 'closed', foregroundSec: Math.round(r.fgMs / 1000) });
    }
  }
}

module.exports = { MinutkaLink, post };
