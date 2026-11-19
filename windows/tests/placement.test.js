'use strict';

// Překryv musí být vždy celý na monitoru a dost velký na otázku i tlačítka.
const test = require('node:test');
const assert = require('node:assert/strict');
const { fitOverlay } = require('../src/placement');

const wa = { x: 0, y: 0, width: 1638, height: 986 };

test('a big target window is covered exactly', () => {
  assert.deepEqual(fitOverlay({ x: 80, y: 64, width: 960, height: 640 }, wa), { x: 80, y: 64, width: 960, height: 640 });
});

test('a small window grows around its centre to the minimum size', () => {
  const r = fitOverlay({ x: 500, y: 300, width: 400, height: 500 }, wa);
  assert.deepEqual(r, { x: 340, y: 230, width: 720, height: 640 });
});

test('a window partly off the right/bottom edge is pulled fully onto the monitor', () => {
  const r = fitOverlay({ x: 1400, y: 700, width: 500, height: 600 }, wa);
  assert.ok(r.x + r.width <= wa.width && r.y + r.height <= wa.height && r.x >= 0 && r.y >= 0, JSON.stringify(r));
});

test('a window on a monitor above the main one stays on that monitor', () => {
  const top = { x: 0, y: -1152, width: 2048, height: 1104 };
  const r = fitOverlay({ x: -50, y: -1200, width: 300, height: 300 }, top);
  assert.deepEqual([r.x, r.y], [0, -1152]);
});

test('a monitor smaller than the minimum gets the whole work area', () => {
  const small = { x: 0, y: 0, width: 640, height: 480 };
  assert.deepEqual(fitOverlay({ x: 10, y: 10, width: 100, height: 100 }, small), { x: 0, y: 0, width: 640, height: 480 });
});
