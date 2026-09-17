'use strict';

// Dashboard: nastavení hlídaných appek + statistiky.

let config = null;
const tr = (text, values) => PauseI18n.tr(config?.language || 'en', text, values);

const $ = (id) => document.getElementById(id);
const listEl = $('targetList');
const addPanel = $('addPanel');

// ---------- pomůcky ----------
function h(tag, attrs = {}, children = []) {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === 'class') node.className = v;
    else if (k === 'text') node.textContent = v;
    else if (k.startsWith('on') && typeof v === 'function') {
      node.addEventListener(k.slice(2).toLowerCase(), v);
    } else if (v === true) node.setAttribute(k, '');
    else if (v !== false && v != null) node.setAttribute(k, v);
  }
  for (const c of [].concat(children)) {
    if (c == null) continue;
    node.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
  }
  return node;
}

function slug(s) {
  return String(s || 'app')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '') || 'app';
}

function csv(arr) {
  return (arr || []).join(', ');
}
function parseCsv(str) {
  return String(str || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

async function persist() {
  config = await window.mezera.saveConfig(config);
}

function setupLanguage() {
  PauseI18n.apply(document, config.language);
  $('languageSelect').value = config.language;
  $('settingsButton').addEventListener('click', () => $('languageSettings').showModal());
  $('languageSelect').addEventListener('change', async event => {
    config.language = event.target.value;
    await persist();
    PauseI18n.apply(document, config.language);
    syncMaster();
    renderTargets();
    loadStats();
  });
}

// ---------- taby ----------
function setupTabs() {
  document.querySelectorAll('.tab').forEach((btn) => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab').forEach((b) => b.classList.remove('is-active'));
      document.querySelectorAll('.tabpane').forEach((p) => p.classList.remove('is-active'));
      btn.classList.add('is-active');
      $(`tab-${btn.dataset.tab}`).classList.add('is-active');
      if (btn.dataset.tab === 'stats') loadStats();
    });
  });
}

// ---------- master + autostart ----------
function syncMaster() {
  $('masterToggle').checked = !!config.enabled;
  $('masterText').textContent = config.enabled ? tr("Hlídání zapnuto") : tr("Hlídání pozastaveno");
}
function setupMaster() {
  syncMaster();
  $('masterToggle').addEventListener('change', async (e) => {
    config.enabled = e.target.checked;
    syncMaster();
    await persist();
  });
}
function setupAutostart() {
  $('autostartToggle').checked = !!config.autostart;
  $('autostartToggle').addEventListener('change', async (e) => {
    config.autostart = e.target.checked;
    await persist();
  });
}

// ---------- seznam cílů ----------
function targetSubline(t) {
  const parts = [];
  parts.push(tr("nádech {seconds}s", { seconds: t.cooldownSec }));
  parts.push(tr("důvod min. {count} znaků", { count: t.reasonMinChars }));
  const m = [];
  if (t.match.title.length) m.push(tr("web: {names}", { names: t.match.title.join(', ') }));
  if (t.match.process.length) m.push(tr("appka: {names}", { names: t.match.process.join(', ') }));
  if (m.length) parts.push(m.join(' · '));
  return parts.join('  ·  ');
}

function buildEditor(t, container) {
  const row = (labelText, hintText, input) =>
    h('div', { class: 'fld' }, [
      h('label', {}, [labelText, hintText ? h('span', { class: 'hint', text: ' ' + hintText }) : null]),
      input
    ]);

  const nameI = h('input', { type: 'text', value: t.label });
  const titleI = h('input', { type: 'text', value: csv(t.match.title) });
  const procI = h('input', { type: 'text', value: csv(t.match.process) });
  const coolI = h('input', { type: 'number', min: '0', max: '120', value: t.cooldownSec });
  const minI = h('input', { type: 'number', min: '0', max: '200', value: t.reasonMinChars });
  const gapI = h('input', { type: 'number', min: '0', max: '600', value: t.sessionGapSec });

  const grid = h('div', { class: 'target-edit' }, [
    row(tr("Název"), '', nameI),
    row(tr("Nádech (s)"), tr("kolik vteřin dýchat"), coolI),
    row(tr("Web podle titulku"), tr("např. youtube, instagram"), titleI),
    row(tr("Appka podle procesu"), tr("např. whatsapp, discord"), procI),
    row(tr("Min. znaků důvodu"), '', minI),
    row(tr("Klid mezi dotazy (s)"), tr("nezeptá se znovu, když se vrátíš do"), gapI)
  ]);

  const save = h('button', { class: 'btn-fill', text: tr("Uložit") });
  const cancel = h('button', { class: 'btn-quiet', text: tr("Zrušit") });
  save.addEventListener('click', async () => {
    t.label = nameI.value.trim() || t.label;
    t.match.title = parseCsv(titleI.value);
    t.match.process = parseCsv(procI.value);
    t.cooldownSec = Math.max(0, parseInt(coolI.value, 10) || 0);
    t.reasonMinChars = Math.max(0, parseInt(minI.value, 10) || 0);
    t.sessionGapSec = Math.max(0, parseInt(gapI.value, 10) || 0);
    await persist();
    renderTargets();
  });
  cancel.addEventListener('click', () => renderTargets());

  container.appendChild(grid);
  container.appendChild(h('div', { class: 'panel-actions' }, [save, cancel]));
}

function renderTargets() {
  listEl.innerHTML = '';
  if (!config.targets.length) {
    listEl.appendChild(h('div', { class: 'empty', text: tr("Zatím žádná appka. Přidej si první níž.") }));
    return;
  }
  for (const t of config.targets) {
    const wrap = h('div', { class: 'target' + (t.enabled ? ' on' : '') });

    const sw = h('label', { class: 'switch' }, [
      (() => {
        const i = h('input', { type: 'checkbox' });
        i.checked = t.enabled;
        i.addEventListener('change', async () => {
          t.enabled = i.checked;
          wrap.classList.toggle('on', t.enabled);
          await persist();
        });
        return i;
      })(),
      h('span', { class: 'slider' })
    ]);

    const editLink = h('button', { class: 'link-btn', text: tr("Upravit") });
    const delLink = h('button', { class: 'link-btn danger', text: tr("Smazat") });

    const main = h('div', { class: 'target-main' }, [
      h('div', { class: 'target-dot' }),
      h('div', { class: 'target-info' }, [
        h('div', { class: 'target-name', text: t.label }),
        h('div', { class: 'target-sub', text: targetSubline(t) })
      ]),
      h('div', { class: 'target-actions' }, [sw, editLink, delLink])
    ]);
    wrap.appendChild(main);

    let open = false;
    editLink.addEventListener('click', () => {
      if (open) {
        renderTargets();
        return;
      }
      open = true;
      editLink.textContent = tr("Zavřít");
      buildEditor(t, wrap);
    });
    delLink.addEventListener('click', async () => {
      config.targets = config.targets.filter((x) => x !== t);
      await persist();
      renderTargets();
    });

    listEl.appendChild(wrap);
  }
}

// ---------- přidání ----------
async function setupAdd() {
  $('addBtn').addEventListener('click', () => {
    if (!addPanel.hidden) {
      addPanel.hidden = true;
      return;
    }
    openAddPanel();
  });
}

async function openAddPanel() {
  addPanel.innerHTML = '';
  addPanel.hidden = false;

  const nameI = h('input', { type: 'text', placeholder: tr("např. TikTok") });
  const titleI = h('input', { type: 'text', placeholder: 'tiktok' });
  const procI = h('input', { type: 'text', placeholder: 'tiktok' });
  const coolI = h('input', { type: 'number', min: '0', max: '120', value: '10' });
  const minI = h('input', { type: 'number', min: '0', max: '200', value: '20' });

  const fld = (label, hint, input, full) =>
    h('div', { class: 'fld' + (full ? ' full' : '') }, [
      h('label', {}, [label, hint ? h('span', { class: 'hint', text: ' ' + hint }) : null]),
      input
    ]);

  const grid = h('div', { class: 'panel-grid' }, [
    fld(tr("Název"), '', nameI),
    fld(tr("Nádech (s)"), '', coolI),
    fld(tr("Web podle titulku okna"), tr("kus názvu záložky v prohlížeči"), titleI),
    fld(tr("Appka podle procesu"), tr("kus názvu .exe"), procI),
    fld(tr("Min. znaků důvodu"), '', minI)
  ]);

  const openAppsBox = h('div', { class: 'open-apps' }, [
    h('div', { class: 'open-apps-title', text: tr("Právě otevřené appky (klikni a doplní se):") }),
    h('div', { class: 'chips', text: tr("Načítám...") })
  ]);

  const add = h('button', { class: 'btn-fill', text: tr("Přidat") });
  const cancel = h('button', { class: 'btn-quiet', text: tr("Zrušit") });
  add.addEventListener('click', async () => {
    const label = nameI.value.trim();
    if (!label) {
      nameI.focus();
      return;
    }
    config.targets.push({
      id: slug(label) + '-' + Math.random().toString(36).slice(2, 5),
      label,
      enabled: true,
      match: { process: parseCsv(procI.value), title: parseCsv(titleI.value) },
      cooldownSec: Math.max(0, parseInt(coolI.value, 10) || 0),
      reasonMinChars: Math.max(0, parseInt(minI.value, 10) || 0),
      sessionGapSec: 45
    });
    await persist();
    addPanel.hidden = true;
    renderTargets();
  });
  cancel.addEventListener('click', () => {
    addPanel.hidden = true;
  });

  addPanel.appendChild(h('h3', { text: tr("Nová appka nebo web") }));
  addPanel.appendChild(
    h('p', { class: 'lead', text: tr("Stačí vyplnit jedno pole (web nebo appku). Klidně obojí.") })
  );
  addPanel.appendChild(grid);
  addPanel.appendChild(openAppsBox);
  addPanel.appendChild(h('div', { class: 'panel-actions' }, [add, cancel]));

  // doplníme právě otevřené appky jako klikatelné chipy
  try {
    const apps = await window.mezera.listOpenApps();
    const chips = openAppsBox.querySelector('.chips');
    chips.innerHTML = '';
    const filtered = apps
      .filter((a) => a.name && !/mezera|electron/i.test(a.name))
      .slice(0, 24);
    if (!filtered.length) {
      chips.appendChild(h('span', { class: 'hint', text: tr("nic nenačteno") }));
    }
    for (const a of filtered) {
      const chip = h('button', { class: 'chip', text: a.name });
      chip.addEventListener('click', () => {
        if (!nameI.value.trim()) nameI.value = a.name.replace(/\.exe$/i, '');
        procI.value = a.name.replace(/\.exe$/i, '');
      });
      chips.appendChild(chip);
    }
  } catch (e) {
    /* nevadí */
  }
}

// ---------- statistiky ----------
function fmtWhen(ts) {
  const d = new Date(ts);
  const now = new Date();
  const sameDay = d.toDateString() === now.toDateString();
  const y = new Date(now);
  y.setDate(now.getDate() - 1);
  const yest = d.toDateString() === y.toDateString();
  const hhmm = d.toLocaleTimeString(config.language === 'cs' ? 'cs-CZ' : 'en-GB', { hour: '2-digit', minute: '2-digit' });
  if (sameDay) return tr("dnes {time}", { time: hhmm });
  if (yest) return tr("včera {time}", { time: hhmm });
  return d.toLocaleString(config.language === 'cs' ? 'cs-CZ' : 'en-GB', { dateStyle: 'short', timeStyle: 'short' });
}

async function loadStats() {
  const events = await window.mezera.getStats();
  renderSummary(events);
  renderBars(events);
  renderTimeline(events);
}

function fmtSaved(minutes) {
  if (minutes >= 60) {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return m ? `${h} h ${m} min` : `${h} h`;
  }
  return `${minutes} min`;
}

function renderSummary(events) {
  const total = events.length;
  const abandoned = events.filter((e) => e.outcome === 'abandoned').length;
  const counts = {};
  for (const e of events) counts[e.label] = (counts[e.label] || 0) + 1;
  let top = '-';
  let topN = 0;
  for (const [k, v] of Object.entries(counts)) if (v > topN) (top = k), (topN = v);

  // Odhad ušetřeného času: každé "rozmyslel jsem si to" = cca 15 min neztraceného scrollování.
  const savedMin = abandoned * 15;

  const box = $('statsSummary');
  box.className = 'statsflow';
  box.innerHTML = '';

  const heroItem = (num, label) =>
    h('div', { class: 'hero-item' }, [
      h('div', { class: 'hero-n', text: String(num) }),
      h('div', { class: 'hero-l', text: label })
    ]);
  box.appendChild(
    h('div', { class: 'hero' }, [
      heroItem(total, tr("zásahů celkem")),
      heroItem(fmtSaved(savedMin), tr("ušetřeno (odhad)"))
    ])
  );

  const tile = (num, label) =>
    h('div', { class: 'stat' }, [
      h('div', { class: 'stat-num', text: String(num) }),
      h('div', { class: 'stat-label', text: label })
    ]);
  box.appendChild(
    h('div', { class: 'summary two' }, [
      tile(abandoned, tr("rozmyslel sis to")),
      tile(top, tr("kam nejčastěji"))
    ])
  );
}

function renderBars(events) {
  const box = $('statsBars');
  box.innerHTML = '';
  const counts = {};
  for (const e of events) counts[e.label] = (counts[e.label] || 0) + 1;
  const rows = Object.entries(counts).sort((a, b) => b[1] - a[1]);
  if (!rows.length) {
    box.appendChild(h('div', { class: 'empty', text: tr("Zatím žádná data. Až tě Pause zastaví, uvidíš to tu.") }));
    return;
  }
  const max = rows[0][1];
  for (const [label, count] of rows) {
    box.appendChild(
      h('div', { class: 'bar-row' }, [
        h('div', { class: 'bar-name', text: label }),
        h('div', { class: 'bar-track' }, [
          h('div', { class: 'bar-fill', style: `width:${Math.round((count / max) * 100)}%` })
        ]),
        h('div', { class: 'bar-count', text: String(count) })
      ])
    );
  }
}

function renderTimeline(events) {
  const box = $('statsTimeline');
  box.innerHTML = '';
  const recent = events.slice(-50).reverse();
  if (!recent.length) {
    box.appendChild(h('div', { class: 'empty', text: tr("Žádné důvody zatím.") }));
    return;
  }
  for (const e of recent) {
    const badgeClass = e.outcome === 'abandoned' ? 'badge abandoned' : 'badge continued';
    const badgeText = e.outcome === 'abandoned' ? tr("rozmyslel") : tr("vešel");
    const appLine = h('div', { class: 'tl-app', text: e.label + '  ' }, [
      h('span', { class: badgeClass, text: badgeText })
    ]);
    box.appendChild(
      h('div', { class: 'tl-item' }, [
        h('div', { class: 'tl-when', text: fmtWhen(e.ts) }),
        h('div', { class: 'tl-body' }, [
          appLine,
          h('div', { class: 'tl-reason', text: e.reason || tr("(bez důvodu)") })
        ])
      ])
    );
  }
}

$('clearStats').addEventListener('click', async () => {
  await window.mezera.clearStats();
  loadStats();
});

// ---------- boot ----------
async function init() {
  config = await window.mezera.getConfig();
  setupLanguage();
  setupTabs();
  setupMaster();
  setupAutostart();
  renderTargets();
  setupAdd();
  loadStats();
  window.mezera.onStatsChanged(() => loadStats());
  window.mezera.onConfigChanged((c) => {
    config = c;
    PauseI18n.apply(document, config.language);
    $('languageSelect').value = config.language;
    syncMaster();
    loadStats();
    renderTargets();
  });
}

init();
