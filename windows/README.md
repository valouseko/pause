# Mezera - Windows

Desktopová verze. Sleduje, které okno máš vepředu, a když otevřeš appku nebo web,
který si nastavíš, překryje ho klidnou obrazovkou: nádech, "proč tam jdeš" a statistiky.

## Spuštění

1. Musíš mít nainstalovaný [Node.js](https://nodejs.org) (LTS).
2. Poprvé nainstaluj závislosti:
   ```powershell
   cd windows
   npm install
   ```
3. Spusť:
   ```powershell
   npm start
   ```
   Nebo jen dvakrát klikni na **`spustit.cmd`** (vyskočí okno konzole).
   Tichá varianta bez konzole = **`Mezera.vbs`**.

Appka běží v tray (ikona u hodin). Klik na ni otevře nastavení a statistiky.
Křížkem se okno jen schová, appka hlídá dál. Konec přes tray (pravý klik -> Konec).

## Jak to funguje

- Každých ~0,9 s se kouká, které okno je vepředu (`get-windows`).
- Weby pozná podle **názvu okna** (Brave/Chrome do titulku dávají název stránky, takže
  "... - YouTube" se chytne). Appky podle **názvu procesu** (např. `whatsapp`).
- Když vejdeš do hlídaného cíle, ukáže se celoobrazovkové okno: dýchání (cooldown),
  pak dotaz na důvod (min. počet znaků), pak Pokračovat / Rozmyslel jsem si to.
- Dokud jsi v appce (nebo se vrátíš do pár desítek vteřin), znovu neotravuje.
- Vše se ukládá lokálně (nic neodchází ven): `%AppData%\Mezera\config.json` a `stats.json`.

## Spouštět s Windows

V appce je přepínač **Spouštět s Windows**. Případně ručně: dej zástupce na `Mezera.vbs`
do složky, kterou otevřeš přes `Win+R` -> `shell:startup`.

## Balení do instalačky (.exe) - volitelné

V `package.json` je připravený `electron-builder`:

```powershell
npm run dist
```

Pozor: electron-builder i samotný Electron si stahují binárky z GitHubu. Na tomhle stroji
**Avast láme SSL**, takže stažení přes Node občas spadne ("Electron failed to install correctly").
Když to nastane:

- Electron binárku dотáhni přes PowerShell (ten jede přes systémový cert store):
  stáhni `electron-v<verze>-win32-x64.zip` z GitHub releases, rozbal do
  `node_modules/electron/dist/` a vedle vytvoř `node_modules/electron/path.txt` s obsahem `electron.exe`.
- Pro `electron-builder` platí totéž pro jeho cache (`winCodeSign`, `nsis`).

Pro běžné používání ale stačí `npm start` / `spustit.cmd`, balit nemusíš.

## Poznámka pro vývoj v tomhle repu

Když spouštíš z prostředí, které nastavuje `ELECTRON_RUN_AS_NODE=1` (třeba integrovaný
terminál ve VS Code), Electron se chová jako čisté Node a spadne na `app is undefined`.
Před spuštěním proměnnou zruš: v PowerShellu `Remove-Item Env:ELECTRON_RUN_AS_NODE`.
Mimo takové prostředí (normální spuštění) se tohle neděje.
