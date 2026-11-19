'use strict';

// Propojení Pause -> Minutka: vstup do měřené appky, čas v popředí, zavření appky, křížek v Minutce.
const test = require('node:test');
const assert = require('node:assert/strict');
const { MinutkaLink } = require('../src/minutka');

const targets = [
  { id: 'whatsapp', label: 'WhatsApp', enabled: true, track: true, match: { process: ['whatsapp'], title: [] } },
  { id: 'vscode', label: 'VS Code', enabled: true, track: true, pause: false, match: { process: ['code'], title: [] } },
  { id: 'youtube', label: 'YouTube', enabled: true, track: false, match: { process: [], title: ['youtube'] } }
];
const wa = { title: 'WhatsApp', owner: { name: 'WhatsApp', path: 'C:/x/WhatsApp.exe' } };
const code = { title: 'main.js - VS Code', owner: { name: 'Code', path: 'C:/x/Code.exe' } };
const yt = { title: 'YouTube - Brave', owner: { name: 'brave', path: 'C:/x/brave.exe' } };

function setup() {
  const calls = [];
  let clock = 0;
  const server = new Map(); // key -> interrupt
  let open = [wa, code];
  const link = new MinutkaLink({
    now: () => clock,
    listOpen: async () => open,
    send: async (path, body) => {
      calls.push([path, body]);
      if (path.endsWith('/start')) server.set(body.key, { key: body.key });
      if (path.endsWith('/stop')) server.delete(body.key);
      return { interrupts: [...server.values()] };
    }
  });
  return { link, calls, server, advance: (ms) => { clock += ms; }, setOpen: (w) => { open = w; } };
}

test('entering a tracked app starts one interruption; untracked apps are ignored', async () => {
  const { link, calls, advance } = setup();
  await link.tick(yt, targets);
  assert.equal(calls.length, 0);
  await link.tick(wa, targets);
  advance(1000);
  await link.tick(wa, targets);
  await link.tick(code, targets);
  await link.tick(wa, targets); // návrat do běžícího přerušení nic nezdvojí
  const starts = calls.filter(([p]) => p.endsWith('/start')).map(([, b]) => b.key);
  assert.deepEqual(starts, ['whatsapp', 'vscode']);
});

test('no interruption while the Pause overlay is still waiting for an answer', async () => {
  const { link, calls } = setup();
  await link.tick(wa, targets, { blocked: true });
  assert.equal(calls.length, 0);
  await link.tick(wa, targets); // po "Pokračovat" je appka v popředí = vstup
  assert.equal(calls[0][0], '/api/track/interrupt/start');
});

test('foreground time is pinged and closing the app ends the interruption', async () => {
  const { link, calls, advance, setOpen } = setup();
  await link.tick(wa, targets);
  for (let i = 0; i < 20; i++) { advance(1000); await link.tick(wa, targets); }
  const ping = calls.find(([p]) => p.endsWith('/ping'));
  assert.ok(ping && ping[1].foregroundSec >= 14);
  setOpen([code]);
  advance(11000);
  await link.tick(yt, targets);
  const stop = calls.find(([p]) => p.endsWith('/stop'));
  assert.equal(stop[1].key, 'whatsapp');
  assert.equal(stop[1].reason, 'closed');
  assert.equal(link.running.size, 0);
});

test('closing via the Minutka cross is respected and a new visit starts a new interruption', async () => {
  const { link, calls, server, advance } = setup();
  await link.tick(wa, targets);
  server.clear(); // Ondra klikl na křížek v Minutce
  advance(16000);
  await link.tick(wa, targets); // ping vrátí prázdný seznam -> synchronizace
  assert.equal(link.running.size, 0);
  await link.tick(wa, targets); // pořád stejná návštěva -> nic
  assert.equal(calls.filter(([p]) => p.endsWith('/start')).length, 1);
  await link.tick(yt, targets);
  await link.tick(wa, targets); // nový vstup
  assert.equal(calls.filter(([p]) => p.endsWith('/start')).length, 2);
});

test('when Minutka is offline nothing is remembered, so the next entry retries', async () => {
  const link = new MinutkaLink({ now: () => 0, listOpen: async () => [], send: async () => null });
  await link.tick(wa, targets);
  assert.equal(link.running.size, 0);
});
