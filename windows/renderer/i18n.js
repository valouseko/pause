'use strict';

// Shared by the dashboard, intervention screen and Electron tray.
(function (root, factory) {
  if (typeof module === 'object' && module.exports) module.exports = factory();
  else root.PauseI18n = factory();
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  const english = {
  "vteřina na rozmyšlenou": "a moment to think",
  "Zapnout nebo pozastavit hlídání": "Enable or pause monitoring",
  "Hlídání zapnuto": "Monitoring enabled",
  "Hlídání pozastaveno": "Monitoring paused",
  "Appky": "Apps",
  "Statistiky": "Statistics",
  "Vyber appky a weby, u kterých se tě Pause zeptá, proč tam jdeš. Weby chytá podle názvu okna v prohlížeči, appky podle názvu procesu.": "Choose the apps and websites where Pause will ask why you are opening them. Websites are detected by window title, apps by process name.",
  "+ Přidat appku nebo web": "+ Add an app or website",
  "Spouštět s Windows": "Start with Windows",
  "Pause naběhne na pozadí po přihlášení.": "Pause starts in the background when you sign in.",
  "Kam nejčastěji chodíš": "Your most opened apps",
  "Poslední důvody": "Recent reasons",
  "Vymazat statistiky": "Clear statistics",
  "Název": "Name",
  "Nádech (s)": "Breathing time (s)",
  "kolik vteřin dýchat": "how many seconds to breathe",
  "Web podle titulku": "Website by window title",
  "např. youtube, instagram": "e.g. youtube, instagram",
  "Appka podle procesu": "App by process name",
  "např. whatsapp, discord": "e.g. whatsapp, discord",
  "Min. znaků důvodu": "Minimum reason length",
  "Klid mezi dotazy (s)": "Time between pauses (s)",
  "nezeptá se znovu, když se vrátíš do": "no new pause if you return within this time",
  "Uložit": "Save",
  "Zrušit": "Cancel",
  "Zatím žádná appka. Přidej si první níž.": "No apps yet. Add your first one below.",
  "Upravit": "Edit",
  "Smazat": "Delete",
  "Zavřít": "Close",
  "např. TikTok": "e.g. TikTok",
  "Web podle titulku okna": "Website by window title",
  "kus názvu záložky v prohlížeči": "part of a browser tab title",
  "kus názvu .exe": "part of the .exe name",
  "Právě otevřené appky (klikni a doplní se):": "Open apps (click to fill in):",
  "Načítám...": "Loading...",
  "Přidat": "Add",
  "Nová appka nebo web": "New app or website",
  "Stačí vyplnit jedno pole (web nebo appku). Klidně obojí.": "Fill in at least one match field: website, app, or both.",
  "nic nenačteno": "no apps found",
  "zásahů celkem": "total pauses",
  "ušetřeno (odhad)": "time saved (estimate)",
  "rozmyslel sis to": "times you changed your mind",
  "kam nejčastěji": "most opened",
  "Zatím žádná data. Až tě Pause zastaví, uvidíš to tu.": "No data yet. Your pauses will appear here.",
  "Žádné důvody zatím.": "No reasons yet.",
  "rozmyslel": "changed mind",
  "vešel": "continued",
  "(bez důvodu)": "(no reason)",
  "Nadechni se": "Breathe in",
  "Zadrž": "Hold",
  "Vydechni": "Breathe out",
  "Teď": "Now",
  "Jdi s rozmyslem.": "Go with intention.",
  "Dobrá volba. Ušetřený čas je tvůj.": "Good choice. That time is yours.",
  "aplikace": "app",
  "Napiš to popravdě. Uvidíš to pak ve statistikách.": "Be honest. You can look back at your reasons in Statistics.",
  "Rozmyslel jsem si to": "I changed my mind",
  "Nastavení": "Settings",
  "Jazyk": "Language",
  "Volba se uloží a zůstane i po aktualizaci.": "Your choice is saved and kept after updates.",
  "Otevřít Pause": "Open Pause",
  "Pozastavit hlídání": "Pause monitoring",
  "Zapnout hlídání": "Enable monitoring",
  "Konec": "Quit",
  "Pause - hlídá": "Pause - monitoring",
  "Pause - pozastaveno": "Pause - paused",
  "neznámé": "unknown",
  "nádech {seconds}s": "breathe {seconds}s",
  "důvod min. {count} znaků": "reason: at least {count} characters",
  "web: {names}": "web: {names}",
  "appka: {names}": "app: {names}",
  "dnes {time}": "today {time}",
  "včera {time}": "yesterday {time}",
  "Napiš aspoň {count} znaků, popravdě...": "Write at least {count} characters, honestly...",
  "Dej si vteřinu, než otevřeš {name}.": "Take a moment before opening {name}.",
  "Proč jdeš do {name}?": "Why are you opening {name}?",
  "Pokračovat do {name}": "Continue to {name}"
};
  function tr(language, text, values = {}) {
    const translated = language === 'cs' ? text : (english[text] || text);
    return translated.replace(/\{(\w+)\}/g, (match, key) => Object.hasOwn(values, key) ? String(values[key]) : match);
  }
  function apply(document, language) {
    document.documentElement.lang = language === 'cs' ? 'cs' : 'en';
    document.querySelectorAll('[data-i18n]').forEach(node => {
      node.textContent = tr(language, node.dataset.i18n);
    });
    for (const attribute of ['title', 'aria-label', 'placeholder']) {
      document.querySelectorAll(`[data-i18n-${attribute}]`).forEach(node => {
        node.setAttribute(attribute, tr(language, node.getAttribute(`data-i18n-${attribute}`)));
      });
    }
  }
  return { tr, apply };
});
