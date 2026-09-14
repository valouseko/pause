'use strict';

// Jednoduché lokální úložiště: config + statistiky jako JSON v userData.
// Žádná cloudová data, všechno zůstává na stroji.

const fs = require('fs');
const path = require('path');

function defaultTargets() {
  // Seed: nejčastější rozptylovače. Ondra si to v UI přepne, jak chce.
  // match.process = kus názvu procesu/exe (desktop appky).
  // match.title = kus názvu okna (chytne i weby v prohlížeči).
  return [
    {
      id: 'youtube',
      label: 'YouTube',
      enabled: true,
      match: { process: [], title: ['youtube'] },
      cooldownSec: 10,
      reasonMinChars: 20,
      sessionGapSec: 45
    },
    {
      id: 'instagram',
      label: 'Instagram',
      enabled: true,
      match: { process: ['instagram'], title: ['instagram'] },
      cooldownSec: 10,
      reasonMinChars: 20,
      sessionGapSec: 45
    },
    {
      id: 'facebook',
      label: 'Facebook',
      enabled: true,
      match: { process: [], title: ['facebook'] },
      cooldownSec: 10,
      reasonMinChars: 20,
      sessionGapSec: 45
    },
    {
      id: 'messenger',
      label: 'Messenger',
      enabled: true,
      match: { process: ['messenger'], title: ['messenger'] },
      cooldownSec: 8,
      reasonMinChars: 20,
      sessionGapSec: 45
    },
    {
      id: 'whatsapp',
      label: 'WhatsApp',
      enabled: true,
      match: { process: ['whatsapp'], title: ['whatsapp'] },
      cooldownSec: 8,
      reasonMinChars: 20,
      sessionGapSec: 45
    }
  ];
}

function defaultConfig() {
  return {
    version: 1,
    enabled: true,
    pollMs: 900,
    autostart: false,
    targets: defaultTargets()
  };
}

class Store {
  constructor(dir) {
    this.dir = dir;
    this.configPath = path.join(dir, 'config.json');
    this.statsPath = path.join(dir, 'stats.json');
    this.config = this._load(this.configPath, defaultConfig());
    this.statsData = this._load(this.statsPath, { events: [] });
    this._mergeDefaults();
  }

  _load(file, fallback) {
    try {
      const raw = fs.readFileSync(file, 'utf8');
      const parsed = JSON.parse(raw);
      return parsed && typeof parsed === 'object' ? parsed : fallback;
    } catch (e) {
      return fallback;
    }
  }

  _save(file, data) {
    try {
      fs.mkdirSync(path.dirname(file), { recursive: true });
      fs.writeFileSync(file, JSON.stringify(data, null, 2), 'utf8');
    } catch (e) {
      console.error('Nepovedlo se uložit', file, e);
    }
  }

  // Doplní chybějící pole na cílech, kdyby se schéma rozšířilo.
  _mergeDefaults() {
    if (!Array.isArray(this.config.targets)) this.config.targets = defaultTargets();
    for (const t of this.config.targets) {
      if (typeof t.cooldownSec !== 'number') t.cooldownSec = 10;
      if (typeof t.reasonMinChars !== 'number') t.reasonMinChars = 20;
      if (typeof t.sessionGapSec !== 'number') t.sessionGapSec = 45;
      if (!t.match) t.match = { process: [], title: [] };
      if (!Array.isArray(t.match.process)) t.match.process = [];
      if (!Array.isArray(t.match.title)) t.match.title = [];
      if (typeof t.enabled !== 'boolean') t.enabled = true;
    }
    if (typeof this.config.pollMs !== 'number') this.config.pollMs = 900;
    if (typeof this.config.enabled !== 'boolean') this.config.enabled = true;
  }

  getConfig() {
    return this.config;
  }

  saveConfig(next) {
    this.config = next;
    this._mergeDefaults();
    this._save(this.configPath, this.config);
    return this.config;
  }

  addEvent(evt) {
    const event = Object.assign(
      { id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`, ts: Date.now() },
      evt
    );
    this.statsData.events.push(event);
    // Necháme rozumný strop, ať soubor nebobtná donekonečna.
    if (this.statsData.events.length > 5000) {
      this.statsData.events = this.statsData.events.slice(-5000);
    }
    this._save(this.statsPath, this.statsData);
    return event;
  }

  getEvents() {
    return this.statsData.events || [];
  }

  clearStats() {
    this.statsData = { events: [] };
    this._save(this.statsPath, this.statsData);
  }
}

module.exports = { Store, defaultConfig, defaultTargets };
