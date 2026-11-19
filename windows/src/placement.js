'use strict';

// Kam dát překryv: přes okno cílové appky, ale VŽDY celý na monitoru a dost velký, aby se vešla
// otázka i tlačítka. Malé okno (nebo okno částečně mimo obrazovku) dřív ořízlo tlačítka a nešlo ven.
const MIN_W = 720;
const MIN_H = 640;

// rect a workArea v DIP: { x, y, width, height }
function fitOverlay(rect, workArea) {
  const wa = workArea;
  const w = Math.min(Math.max(MIN_W, rect.width), wa.width);
  const h = Math.min(Math.max(MIN_H, rect.height), wa.height);
  // zvětšení kolem středu okna cíle, pak posunout dovnitř monitoru
  let x = Math.round(rect.x + (rect.width - w) / 2);
  let y = Math.round(rect.y + (rect.height - h) / 2);
  x = Math.max(wa.x, Math.min(x, wa.x + wa.width - w));
  y = Math.max(wa.y, Math.min(y, wa.y + wa.height - h));
  return { x, y, width: Math.round(w), height: Math.round(h) };
}

module.exports = { fitOverlay, MIN_W, MIN_H };
