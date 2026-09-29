'use strict';

// Řízení intervence: nádech -> proč -> pokračovat / rozmyslet si to.

const el = (id) => document.getElementById(id);

const phaseBreath = el('phaseBreath');
const phaseReason = el('phaseReason');
const phaseDone = el('phaseDone');
const phaseText = el('phaseText');
const orb = el('orb');
const ring = document.querySelector('.ring-progress');
const breathLabel = el('breathLabel');
const reasonInput = el('reasonInput');
const counter = el('counter');
const continueBtn = el('continueBtn');
const abandonBtn = el('abandonBtn');

const RING_CIRC = 917; // 2*pi*146

let target = null;
let language = 'en';
const tr = (text, values) => PauseI18n.tr(language, text, values);
let minChars = 20;
let breathTimer = null;

function setPhase(node) {
  [phaseBreath, phaseText, phaseReason, phaseDone].forEach((p) => p.classList.remove('is-active'));
  node.classList.add('is-active');
}

function setAppName(name) {
  document.querySelector('.breath-sub').textContent = tr('Dej si vteřinu, než otevřeš {name}.', { name });
  document.querySelector('.question').textContent = tr('Proč jdeš do {name}?', { name });
  continueBtn.textContent = tr('Pokračovat do {name}', { name });
}

// ---------- Dýchání ----------
function runBreath(durationMs, onDone) {
  const phases = [
    { label: tr("Nadechni se"), dur: 4000, scale: 1.32 },
    { label: tr("Zadrž"), dur: 1400, scale: 1.32 },
    { label: tr("Vydechni"), dur: 4200, scale: 0.82 }
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
      breathLabel.textContent = tr("Teď");
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

// ---------- Vlastní text místo dýchání ----------
function runPauseText(text, durationMs, onDone) {
  el('pauseText').textContent = text;
  const meter = el('pauseMeter');
  const count = el('pauseCount');
  setPhase(phaseText);
  meter.style.transition = 'none';
  meter.style.transform = 'scaleX(0)';
  void meter.getBoundingClientRect();
  meter.style.transition = `transform ${durationMs}ms linear`;
  meter.style.transform = 'scaleX(1)';

  const endsAt = Date.now() + durationMs;
  function tick() {
    const left = endsAt - Date.now();
    if (left <= 0) {
      onDone();
      return;
    }
    count.textContent = String(Math.ceil(left / 1000));
    breathTimer = setTimeout(tick, Math.min(250, left));
  }
  tick();
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
  el('doneText').textContent = tr("Jdi s rozmyslem.");
  setTimeout(() => {
    window.mezera.submit({ reason });
  }, 850);
}

function finishAbandon() {
  const reason = reasonInput.value.trim();
  setPhase(phaseDone);
  el('doneText').textContent = tr("Dobrá volba. Ušetřený čas je tvůj.");
  setTimeout(() => {
    window.mezera.abandon({ reason });
  }, 950);
}

// ---------- Boot ----------
async function boot() {
  language = (await window.mezera.getConfig()).language || 'en';
  PauseI18n.apply(document, language);
  target = await window.mezera.getTarget();
  const label = target && target.label ? target.label : tr("aplikace");
  minChars = target?.reasonMinChars ?? 20;
  const cooldownSec = target?.cooldownSec ?? 8;

  setAppName(label);
  reasonInput.setAttribute('minlength', String(minChars));
  reasonInput.placeholder = tr('Napiš aspoň {count} znaků, popravdě...', { count: minChars });
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

  const pauseText = String(target?.pauseText || '').trim();
  if (cooldownSec <= 0) {
    showReason();
  } else if (pauseText) {
    runPauseText(pauseText, cooldownSec * 1000, showReason);
  } else {
    setPhase(phaseBreath);
    runBreath(cooldownSec * 1000, showReason);
  }
}

boot();
