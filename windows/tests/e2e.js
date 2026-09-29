'use strict';

// Celý průchod na skutečném Electronu s podvrženým aktivním oknem (PAUSE_E2E=1),
// v oddělené složce dat, takže nesahá na tvoje statistiky ani nastavení.
// Spuštění: npm run e2e   (screenshoty jdou do tests/e2e-out/)

const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const assert = require('node:assert/strict');

const ROOT = path.join(__dirname, '..');
const OUT = path.join(__dirname, 'e2e-out');
const PORT = 9339;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const WA = { id: 1, title: 'WhatsApp', bounds: { x: 100, y: 80, width: 1200, height: 800 }, owner: { name: 'WhatsApp.Root', path: 'C:/x/WhatsApp.Root.exe' } };
const OTHER = { id: 2, title: 'Poznámky', bounds: { x: 0, y: 0, width: 800, height: 600 }, owner: { name: 'Notepad', path: 'C:/x/notepad.exe' } };

async function run() {
  fs.mkdirSync(OUT, { recursive: true });
  const dataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'pause-e2e-'));
  fs.writeFileSync(path.join(dataDir, 'config.json'), JSON.stringify({
    version: 1, enabled: true, pollMs: 400, language: 'cs', autostart: false,
    targets: [{
      id: 'whatsapp', label: 'WhatsApp', enabled: true, match: { process: ['whatsapp'], title: ['whatsapp'] },
      cooldownSec: 3, reasonMinChars: 20, sessionGapSec: 0, pauseText: 'Zvědom si, proč tam jdeš'
    }]
  }));

  const env = { ...process.env, PAUSE_E2E: '1', PAUSE_USER_DATA: dataDir };
  delete env.ELECTRON_RUN_AS_NODE;
  const electron = path.join(ROOT, 'node_modules', 'electron', 'dist', 'electron.exe');
  const app = spawn(electron, ['.', '--hidden', `--inspect=${PORT}`], { cwd: ROOT, env, stdio: ['ignore', 'pipe', 'pipe'] });
  let output = '';
  app.stdout.on('data', (d) => (output += d));
  app.stderr.on('data', (d) => (output += d));

  let ws;
  try {
    let list;
    for (let i = 0; i < 30 && !list; i++) {
      await sleep(300);
      try { list = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json(); } catch (e) {}
    }
    assert.ok(list && list[0], 'Pause se nespustila:\n' + output);
    ws = new WebSocket(list[0].webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject; });
    let seq = 0;
    const waiting = new Map();
    ws.onmessage = (m) => {
      const d = JSON.parse(m.data);
      if (waiting.has(d.id)) { waiting.get(d.id)(d); waiting.delete(d.id); }
    };
    // Spustí kód v hlavním procesu Pause.
    const inMain = (body) => new Promise((resolve) => {
      const id = ++seq;
      waiting.set(id, resolve);
      ws.send(JSON.stringify({
        id, method: 'Runtime.evaluate', params: {
          expression: `(async () => { const T = globalThis.__pauseE2E; const { BrowserWindow } = T; ${body} })()`,
          awaitPromise: true, returnByValue: true
        }
      }));
    }).then((d) => {
      if (!d.result || d.result.exceptionDetails) throw new Error(JSON.stringify(d.result || d));
      return d.result.result.value;
    });
    for (let i = 0; i < 30; i++) {
      if (await inMain('return !!T;').catch(() => false)) break;
      await sleep(300);
    }

    const overlay = "const w = BrowserWindow.getAllWindows().find((x) => x.webContents.getURL().includes('overlay.html'));";
    const setWindow = (w) => inMain(`T.fakeWindow = ${JSON.stringify(w)}; return true;`);
    const overlayState = () => inMain(`${overlay} if (!w) return null; return { visible: w.isVisible(), bounds: w.getBounds(),
      phase: await w.webContents.executeJavaScript("document.querySelector('.is-active') && document.querySelector('.is-active').id") };`);
    const inOverlay = (js) => inMain(`${overlay} return w ? await w.webContents.executeJavaScript(${JSON.stringify(js)}) : null;`);
    const shot = (name) => inMain(`${overlay} if (!w) return false; const img = await w.webContents.capturePage();
      process.getBuiltinModule('fs').writeFileSync(${JSON.stringify(path.join(OUT, name))}, img.toPNG()); return true;`);
    const lastEvent = () => JSON.parse(fs.readFileSync(path.join(dataDir, 'stats.json'), 'utf8')).events.at(-1);
    const step = (msg) => console.log('OK  ' + msg);

    await setWindow(OTHER);
    await sleep(1200);
    assert.equal(await overlayState(), null, 'Překryv nad jinou appkou');
    step('jiná appka: nic se neděje');

    await setWindow(WA);
    await sleep(1800);
    let st = await overlayState();
    assert.ok(st && st.visible, 'Překryv se nad WhatsAppem neukázal: ' + JSON.stringify(st));
    assert.equal(st.phase, 'phaseText');
    assert.equal(await inOverlay("document.getElementById('pauseText').textContent"), 'Zvědom si, proč tam jdeš');
    await shot('1-text.png');
    step('vstup do WhatsAppu: velký text přes okno ' + JSON.stringify(st.bounds));

    await sleep(3000);
    assert.equal((await overlayState()).phase, 'phaseReason');
    assert.equal(await inOverlay("document.getElementById('continueBtn').disabled"), true, 'Pokračovat jde bez důvodu');
    await shot('2-reason.png');
    step('po 3 s otázka, Pokračovat je zamčené, dokud nenapíšeš 20 znaků');

    await inMain(`${overlay} w.close(); return true;`);
    await sleep(500);
    assert.ok(await overlayState(), 'Překryv šlo zavřít (Alt+F4)');
    step('překryv nejde zavřít (Alt+F4)');

    await inOverlay("const t = document.getElementById('reasonInput'); t.value = 'odpovím na jednu pracovní zprávu'; t.dispatchEvent(new Event('input')); document.getElementById('continueBtn').click(); true");
    await sleep(2200);
    assert.equal(await overlayState(), null, 'Překryv po Pokračovat nezmizel');
    await sleep(2500);
    assert.equal(await overlayState(), null, 'Překryv naskočil, i když zůstávám ve WhatsAppu');
    assert.equal(lastEvent().outcome, 'continued');
    step('Pokračovat: pustí dovnitř a dokud ve WhatsAppu zůstáváš, neptá se');

    await setWindow(OTHER);
    await sleep(4500);
    await setWindow(WA);
    await sleep(1800);
    st = await overlayState();
    assert.ok(st && st.visible, 'Po návratu do WhatsAppu se nezeptal znovu');
    step('odejdeš a vrátíš se (klik na lištu): zeptá se znovu');

    await sleep(3200);
    await inOverlay("document.getElementById('abandonBtn').click(); true");
    // Skutečný WhatsApp se teď minimalizuje; podvržené okno to neumí, tak ho "schováme" samo.
    await sleep(300);
    await setWindow(OTHER);
    await sleep(2600);
    assert.equal(await overlayState(), null, 'Překryv po Rozmyslel jsem si to nezmizel');
    assert.equal(lastEvent().outcome, 'abandoned');
    step('Rozmyslel jsem si to: zavře a zapíše do statistik');

    await setWindow(WA);
    await sleep(1800);
    st = await overlayState();
    assert.ok(st && st.visible, 'Po rozmyšlení vznikl klid a WhatsApp šel otevřít bez dotazu');
    step('po rozmyšlení žádný klid: další otevření se zeptá hned');

    console.log('\nE2E OK');
  } finally {
    try { if (ws) ws.close(); } catch (e) {}
    app.kill();
    await sleep(500);
    fs.rmSync(dataDir, { recursive: true, force: true });
  }
}

run().catch((e) => {
  console.error('E2E FAIL:', e.message);
  process.exit(1);
});
