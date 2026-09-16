'use strict';

const { app, BrowserWindow, Tray, Menu, ipcMain, nativeImage, shell, screen } = require('electron');
const path = require('path');
const { Store } = require('./store');
const { getActiveWindow, SessionTracker } = require('./detector');

// ------- Stav aplikace -------
let store = null;
let tray = null;
let dashboardWin = null;
let overlayWin = null;
let pollTimer = null;
let tracker = new SessionTracker();
let currentIntervention = null; // { target, startedAt }
let isQuitting = false;

const ASSETS = path.join(__dirname, '..', 'assets');
const RENDERER = path.join(__dirname, '..', 'renderer');
const PRELOAD = path.join(__dirname, 'preload.js');

// Jména, která bereme jako "naše vlastní okno", ať se netriggerujeme sami.
function selfNames() {
  const names = ['electron', 'mezera'];
  try {
    names.push(path.basename(process.execPath).toLowerCase());
  } catch (e) {}
  return names;
}

function isSelfWindow(win) {
  if (!win || !win.owner) return false;
  const proc = String(win.owner.name || '').toLowerCase();
  const exe = String(win.owner.path || '').toLowerCase();
  return selfNames().some((n) => proc.includes(n) || exe.includes(n));
}

// ------- Tray -------
function trayImage() {
  const p = path.join(ASSETS, 'tray.png');
  const img = nativeImage.createFromPath(p);
  if (!img.isEmpty()) return img;
  return nativeImage.createEmpty();
}

function buildTrayMenu() {
  const cfg = store.getConfig();
  return Menu.buildFromTemplate([
    { label: 'Otevřít Pause', click: () => showDashboard() },
    { type: 'separator' },
    {
      label: cfg.enabled ? 'Pozastavit hlídání' : 'Zapnout hlídání',
      click: () => {
        const c = store.getConfig();
        c.enabled = !c.enabled;
        store.saveConfig(c);
        refreshTray();
        if (dashboardWin) dashboardWin.webContents.send('config:changed', store.getConfig());
      }
    },
    { type: 'separator' },
    { label: 'Konec', click: () => { isQuitting = true; app.quit(); } }
  ]);
}

function refreshTray() {
  if (!tray) return;
  const cfg = store.getConfig();
  tray.setToolTip(cfg.enabled ? 'Pause - hlídá' : 'Pause - pozastaveno');
  tray.setContextMenu(buildTrayMenu());
}

function setupTray() {
  tray = new Tray(trayImage());
  refreshTray();
  tray.on('click', () => showDashboard());
  tray.on('double-click', () => showDashboard());
}

// ------- Dashboard okno -------
function showDashboard() {
  if (dashboardWin) {
    if (dashboardWin.isMinimized()) dashboardWin.restore();
    dashboardWin.show();
    dashboardWin.focus();
    return;
  }
  dashboardWin = new BrowserWindow({
    width: 1040,
    height: 760,
    minWidth: 860,
    minHeight: 620,
    backgroundColor: '#F7F8FB',
    title: 'Pause',
    icon: path.join(ASSETS, 'icon.png'),
    autoHideMenuBar: true,
    webPreferences: {
      preload: PRELOAD,
      contextIsolation: true,
      nodeIntegration: false
    }
  });
  dashboardWin.setMenuBarVisibility(false);
  dashboardWin.loadFile(path.join(RENDERER, 'dashboard.html'));
  dashboardWin.on('close', (e) => {
    // Křížek jen schová do tray, nekončí aplikaci.
    if (!isQuitting) {
      e.preventDefault();
      dashboardWin.hide();
    }
  });
  dashboardWin.on('closed', () => {
    dashboardWin = null;
  });
}

// Spočítá umístění overlaye přesně přes okno cílové appky (správný monitor,
// stejný rozměr). get-windows vrací fyzické pixely, Electron chce DIP -> převod.
function overlayPlacement(bounds) {
  try {
    if (bounds && bounds.width > 1 && bounds.height > 1) {
      const rect = {
        x: Math.round(bounds.x),
        y: Math.round(bounds.y),
        width: Math.round(bounds.width),
        height: Math.round(bounds.height)
      };
      const dip = screen.screenToDipRect(null, rect);
      return {
        x: Math.round(dip.x),
        y: Math.round(dip.y),
        width: Math.max(200, Math.round(dip.width)),
        height: Math.max(160, Math.round(dip.height))
      };
    }
  } catch (e) {}
  return null;
}

// ------- Overlay (intervence) -------
function openOverlay(target, bounds) {
  if (overlayWin) return; // už jedna běží
  tracker.suspend();
  currentIntervention = { target, startedAt: Date.now() };

  const place = overlayPlacement(bounds);
  const opts = {
    frame: false,
    alwaysOnTop: true,
    skipTaskbar: true,
    resizable: false,
    movable: false,
    minimizable: false,
    maximizable: false,
    hasShadow: false,
    backgroundColor: '#0E1220',
    show: false,
    webPreferences: {
      preload: PRELOAD,
      contextIsolation: true,
      nodeIntegration: false
    }
  };
  if (place) {
    opts.x = place.x;
    opts.y = place.y;
    opts.width = place.width;
    opts.height = place.height;
  }

  overlayWin = new BrowserWindow(opts);
  // Když se okno cíle nepodařilo zaměřit, aspoň překryjeme displej pod kurzorem.
  if (!place) {
    try {
      const d = screen.getDisplayNearestPoint(screen.getCursorScreenPoint());
      overlayWin.setBounds(d.workArea);
    } catch (e) {
      overlayWin.setFullScreen(true);
    }
  }
  overlayWin.setAlwaysOnTop(true, 'screen-saver');
  overlayWin.setVisibleOnAllWorkspaces(true);
  overlayWin.loadFile(path.join(RENDERER, 'overlay.html'));
  overlayWin.once('ready-to-show', () => {
    overlayWin.show();
    overlayWin.focus();
  });
  overlayWin.on('closed', () => {
    overlayWin = null;
  });
}

function closeOverlay() {
  const t = currentIntervention ? currentIntervention.target : null;
  if (overlayWin) {
    const w = overlayWin;
    overlayWin = null;
    try { w.destroy(); } catch (e) {}
  }
  if (t) tracker.markHandled(t.id, Date.now());
  currentIntervention = null;
  tracker.resume();
}

// ------- Polling smyčka -------
async function tick() {
  try {
    const cfg = store.getConfig();
    if (!cfg.enabled || overlayWin) return;
    const win = await getActiveWindow();
    const res = tracker.evaluate(win, cfg.targets, Date.now(), isSelfWindow(win));
    if (res.intervene && res.target) {
      openOverlay(res.target, win && win.bounds);
    }
  } catch (e) {
    // Detekce může občas selhat (zamčená obrazovka apod.), to nevadí.
  }
}

function startPolling() {
  stopPolling();
  const cfg = store.getConfig();
  pollTimer = setInterval(tick, Math.max(400, cfg.pollMs || 900));
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

// ------- IPC -------
function registerIpc() {
  // Dashboard: config + statistiky
  ipcMain.handle('config:get', () => store.getConfig());
  ipcMain.handle('config:save', (_e, next) => {
    const saved = store.saveConfig(next);
    startPolling(); // pollMs se mohl změnit
    refreshTray();
    applyAutostart(saved.autostart);
    return saved;
  });
  ipcMain.handle('stats:get', () => store.getEvents());
  ipcMain.handle('stats:clear', () => {
    store.clearStats();
    return true;
  });

  // Pomoc při konfiguraci: seznam právě otevřených oken (appky + tituly).
  ipcMain.handle('apps:listOpen', async () => {
    try {
      const mod = await import('get-windows');
      const wins = await mod.openWindows();
      const seen = new Map();
      for (const w of wins) {
        const name = (w.owner && w.owner.name) || 'neznámé';
        if (!seen.has(name)) {
          seen.set(name, { name, title: w.title || '', path: (w.owner && w.owner.path) || '' });
        }
      }
      return Array.from(seen.values());
    } catch (e) {
      return [];
    }
  });

  // Overlay lifecycle
  ipcMain.handle('overlay:getTarget', () => {
    return currentIntervention ? currentIntervention.target : null;
  });
  ipcMain.on('overlay:submit', (_e, payload) => {
    const it = currentIntervention;
    if (it) {
      store.addEvent({
        targetId: it.target.id,
        label: it.target.label,
        reason: String(payload && payload.reason ? payload.reason : ''),
        cooldownSec: it.target.cooldownSec,
        outcome: 'continued',
        dwellMs: Date.now() - it.startedAt
      });
    }
    closeOverlay();
    if (dashboardWin) dashboardWin.webContents.send('stats:changed');
  });
  ipcMain.on('overlay:abandon', (_e, payload) => {
    const it = currentIntervention;
    if (it) {
      store.addEvent({
        targetId: it.target.id,
        label: it.target.label,
        reason: String(payload && payload.reason ? payload.reason : ''),
        cooldownSec: it.target.cooldownSec,
        outcome: 'abandoned',
        dwellMs: Date.now() - it.startedAt
      });
    }
    closeOverlay();
    if (dashboardWin) dashboardWin.webContents.send('stats:changed');
  });

  ipcMain.on('ui:openExternal', (_e, url) => {
    try { shell.openExternal(url); } catch (e) {}
  });
}

function applyAutostart(enabled) {
  try {
    app.setLoginItemSettings({
      openAtLogin: !!enabled,
      args: ['--hidden']
    });
  } catch (e) {}
}

// ------- Boot -------
const gotLock = app.requestSingleInstanceLock();
if (!gotLock) {
  app.quit();
} else {
  app.on('second-instance', () => showDashboard());

  app.whenReady().then(() => {
    const userDataDir = app.getPath('userData');
    store = new Store(userDataDir);
    registerIpc();
    setupTray();
    startPolling();
    applyAutostart(store.getConfig().autostart);

    const startedHidden = process.argv.includes('--hidden');
    if (!startedHidden) showDashboard();
  });

  app.on('window-all-closed', (e) => {
    // Neukončovat, běžíme dál v tray.
  });

  app.on('before-quit', () => {
    isQuitting = true;
    stopPolling();
  });
}
