'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { Store, defaultConfig } = require('../src/store');
const { tr } = require('../renderer/i18n');

test('old config defaults to English; explicit choice and user data survive reload', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'pause-language-'));
  try {
    const oldConfig = defaultConfig();
    delete oldConfig.language;
    oldConfig.enabled = false;
    oldConfig.targets[0].label = 'My custom label';
    fs.writeFileSync(path.join(dir, 'config.json'), JSON.stringify(oldConfig));
    const original = new Store(dir);
    original.addEvent({label: 'My custom label', reason: 'A reason I wrote', outcome: 'continued'});
    assert.equal(original.getConfig().language, 'en');
    original.saveConfig({...original.getConfig(), language: 'cs'});
    const restarted = new Store(dir);
    assert.equal(restarted.getConfig().language, 'cs');
    assert.equal(restarted.getConfig().enabled, false);
    assert.equal(restarted.getConfig().targets[0].label, 'My custom label');
    assert.equal(restarted.getEvents()[0].reason, 'A reason I wrote');
    restarted.saveConfig({...restarted.getConfig(), language: 'en'});
    assert.equal(new Store(dir).getConfig().language, 'en');
  } finally {
    assert.equal(path.dirname(path.resolve(dir)), path.resolve(os.tmpdir()));
    fs.rmSync(dir, {recursive: true, force: true});
  }
});

test('dynamic text preserves app labels and selects the requested language', () => {
  assert.equal(tr('en', 'Pokračovat do {name}', {name:'YouTube'}), 'Continue to YouTube');
  assert.equal(tr('cs', 'Pokračovat do {name}', {name:'YouTube'}), 'Pokračovat do YouTube');
  assert.equal(tr('en', 'Napiš aspoň {count} znaků, popravdě...', {count:0}), 'Write at least 0 characters, honestly...');
  assert.equal(tr('en', 'Pause - hlídá'), 'Pause - monitoring');
});
