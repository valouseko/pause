'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { SessionTracker } = require('../src/detector');

const wa = { id: 'whatsapp', enabled: true, match: { process: ['whatsapp'], title: [] }, sessionGapSec: 0 };
const yt = { id: 'youtube', enabled: true, match: { process: [], title: ['youtube'] }, sessionGapSec: 45 };
const waWin = { title: 'WhatsApp', owner: { name: 'WhatsApp.exe', path: 'C:/x/WhatsApp.exe' } };
const ytWin = { title: 'Video - YouTube - Chrome', owner: { name: 'chrome.exe', path: 'C:/x/chrome.exe' } };
const other = { title: 'Code', owner: { name: 'Code.exe', path: 'C:/x/Code.exe' } };

test('gap 0: every entry from another window intervenes, staying inside does not', () => {
  const t = new SessionTracker();
  assert.equal(t.evaluate(waWin, [wa], 1000, false).intervene, true);
  t.markContinued('whatsapp', 2000);
  assert.equal(t.evaluate(waWin, [wa], 3000, false).intervene, false);
  assert.equal(t.evaluate(waWin, [wa], 60000, false).intervene, false);
  t.evaluate(other, [wa], 61000, false);
  assert.equal(t.evaluate(waWin, [wa], 62000, false).intervene, true);
});

test('gap 0: short grace after continue avoids an instant loop while focus returns', () => {
  const t = new SessionTracker();
  t.evaluate(waWin, [wa], 1000, false);
  t.markContinued('whatsapp', 2000);
  t.evaluate(other, [wa], 2500, false);
  assert.equal(t.evaluate(waWin, [wa], 3000, false).intervene, false);
  t.evaluate(other, [wa], 9000, false);
  assert.equal(t.evaluate(waWin, [wa], 10000, false).intervene, true);
});

test('abandon never creates a cooldown', () => {
  const t = new SessionTracker();
  t.evaluate(ytWin, [yt], 1000, false);
  t.markAbandoned('youtube');
  assert.equal(t.evaluate(ytWin, [yt], 2000, false).intervene, true);
});

test('unresolved intervention comes back until it is answered', () => {
  const t = new SessionTracker();
  assert.equal(t.evaluate(ytWin, [yt], 1000, false).intervene, true);
  t.evaluate(other, [yt], 2000, false);
  assert.equal(t.evaluate(ytWin, [yt], 3000, false).intervene, true);
});

test('gap > 0 keeps the old session behaviour', () => {
  const t = new SessionTracker();
  t.evaluate(ytWin, [yt], 1000, false);
  t.markContinued('youtube', 1000);
  t.evaluate(other, [yt], 10000, false);
  assert.equal(t.evaluate(ytWin, [yt], 20000, false).intervene, false);
  t.evaluate(other, [yt], 21000, false);
  assert.equal(t.evaluate(ytWin, [yt], 80000, false).intervene, true);
});

test('main process and renderer scripts parse', () => {
  const { execFileSync } = require('node:child_process');
  const path = require('node:path');
  for (const f of ['src/main.js', 'src/detector.js', 'src/store.js', 'src/preload.js', 'renderer/overlay.js', 'renderer/dashboard.js', 'renderer/i18n.js']) {
    execFileSync(process.execPath, ['--check', path.join(__dirname, '..', f)]);
  }
});
