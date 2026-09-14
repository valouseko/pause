'use strict';

const { contextBridge, ipcRenderer } = require('electron');

// Bezpečný most mezi rendererem a hlavním procesem.
contextBridge.exposeInMainWorld('mezera', {
  // Config
  getConfig: () => ipcRenderer.invoke('config:get'),
  saveConfig: (cfg) => ipcRenderer.invoke('config:save', cfg),
  onConfigChanged: (cb) => ipcRenderer.on('config:changed', (_e, cfg) => cb(cfg)),

  // Statistiky
  getStats: () => ipcRenderer.invoke('stats:get'),
  clearStats: () => ipcRenderer.invoke('stats:clear'),
  onStatsChanged: (cb) => ipcRenderer.on('stats:changed', () => cb()),

  // Pomoc při konfiguraci
  listOpenApps: () => ipcRenderer.invoke('apps:listOpen'),

  // Overlay
  getTarget: () => ipcRenderer.invoke('overlay:getTarget'),
  submit: (payload) => ipcRenderer.send('overlay:submit', payload),
  abandon: (payload) => ipcRenderer.send('overlay:abandon', payload),

  openExternal: (url) => ipcRenderer.send('ui:openExternal', url)
});
