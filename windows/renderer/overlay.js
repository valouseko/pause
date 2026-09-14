'use strict';

// Řízení intervence: nádech -> proč -> pokračovat / rozmyslet si to.

const el = (id) => document.getElementById(id);

const phaseBreath = el('phaseBreath');
const phaseReason = el('phaseReason');
const phaseDone = el('phaseDone');
const orb = el('orb');
const ring = document.querySelector('.ring-progress');
const breathLabel = el('breathLabel');
const reasonInput = el('reasonInput');
const counter = el('counter');
const continueBtn = el('continueBtn');
const abandonBtn = el('abandonBtn');

const RING_CIRC = 917; // 2*pi*146

let target = null;
let minChars = 20;
let breathTimer = null;

function setPhase(node) {
  [phaseBreath, phaseReason, phaseDone].forEach((p) => p.classList.remove('is-active'));
  node.classList.add('is-active');
}

function setAppName(name) {
  el('appNameBreath').textContent = name;
  el('appNameQ').textContent = name;
  el('appNameBtn').textContent = name;
}

// ---------- Dýchání ----------
function runBreath(durationMs, onDone) {
  const phases = [
    { label: 'Nadechni se', dur: 4000, scale: 1.32 },
    { label: 'Zadrž', dur: 1400, scale: 1.32 },
    { label: 'Vydechni', dur: 4200, scale: 0.82 }
  ];

  // Kruh se plní za dobu cooldownu.
  ring.style.strokeDashoffset = String(RING_CIRC);
  // vynutíme reflow, ať transition naskočí
  void ring.getBoundingClientRect();
  ring.style.transition = `stroke-dashoffset ${durationMs}ms linear`;
  ring.style.strokeDashoffset = '0';

  const startedAt = Date.now();
  let i = 0;

  function step() {
    const elapsed = Date.now() - startedAt;
    if (elapsed >= durationMs) {
      breathLabel.textContent = 'Teď';
      onDone();
      return;
    }
    const p = phases[i % phases.length];
    breathLabel.textContent = p.label;
    orb.style.transitionDuration = p.dur + 'ms';
    orb.style.transform = `scale(${p.scale})`;
    i += 1;
    breathTimer = setTimeout(step, p.dur);
  }
  step();
}

// ---------- Krok proč ----------
function updateCounter() {
  const len = reasonInput.value.trim().length;
  counter.textContent = `${len} / ${minChars}`;
  const ok = len >= minChars;
  counter.classList.toggle('is-ok', ok);
  continueBtn.disabled = !ok;
}

function showReason() {
  setPhase(phaseReason);
  setTimeout(() => reasonInput.focus(), 400);
}

function finishContinue() {
  const reason = reasonInput.value.trim();
  setPhase(phaseDone);
  el('doneText').textContent = 'Jdi s rozmyslem.';
  setTimeout(() => {
    window.mezera.submit({ reason });
  }, 850);
}

function finishAbandon() {
  const reason = reasonInput.value.trim();
  setPhase(phaseDone);
  el('doneText').textContent = 'Dobrá volba. Ušetřený čas je tvůj.';
  setTimeout(() => {
    window.mezera.abandon({ reason });
  }, 950);
}

// ---------- Boot ----------
async function boot() {
  target = await window.mezera.getTarget();
  const label = target && target.label ? target.label : 'aplikace';
  minChars = (target && target.reasonMinChars) || 20;
  const cooldownSec = (target && target.cooldownSec) || 8;

  setAppName(label);
  reasonInput.setAttribute('minlength', String(minChars));
  reasonInput.placeholder = `Napiš aspoň ${minChars} znaků, popravdě...`;
  updateCounter();

  reasonInput.addEventListener('input', updateCounter);
  reasonInput.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter' && !continueBtn.disabled) {
      finishContinue();
    }
  });
  continueBtn.addEventListener('click', () => {
    if (!continueBtn.disabled) finishContinue();
  });
  abandonBtn.addEventListener('click', finishAbandon);

  // Esc nezavře appku bez důvodu, jen zdvořile připomene.
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') e.preventDefault();
  });

  setPhase(phaseBreath);
  runBreath(cooldownSec * 1000, showReason);
}

boot();
