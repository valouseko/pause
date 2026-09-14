# Mezera

Appka, co ti dá vteřinu na rozmyšlenou, než skočíš do rozptylující aplikace. Inspirováno OneSec.

Když otevřeš appku (nebo web), na kterou si Mezeru nastavíš, překryje ji klidná obrazovka:

1. **Nádech / výdech** - dýchací animace na pár vteřin (cooldown, nedá se přeskočit).
2. **Proč tam jdeš?** - musíš slovy napsat důvod (min. 20 znaků).
3. **Pokračuj, nebo si to rozmysli.** Každý průchod se zaloguje.
4. **Statistiky** - kolikrát jsi kam šel a s jakým důvodem.

Funguje **na Windows i na Androidu**, napříč libovolnými appkami/weby, které si nastavíš (YouTube, Instagram, Messenger, Facebook, WhatsApp, cokoliv).

## Struktura

- `windows/` - desktopová appka (Electron). Dá se rovnou spustit a vyzkoušet.
- `android/` - nativní appka (Kotlin + Jetpack Compose). APK se buildí v cloudu přes GitHub Actions.
- `docs/` - poznámky, rozhodnutí, jak buildit.

## Windows - rychlý start

```powershell
cd windows
npm install
npm start
```

Appka se schová do tray (lišta u hodin). Otevřením ikony se dostaneš na nastavení a statistiky.
Detaily v `windows/README.md`.

## Android - jak získat APK

Zdrojáky jsou v `android/`. APK se nevytváří lokálně (chybí Android SDK), ale v cloudu:
push do GitHubu → GitHub Actions zbuildí `app-debug.apk` → stáhneš z Releases přímo do telefonu.
Detaily v `docs/android-build.md`.

## Design

Čistá bílá, klidná modrá, font Inter, zaoblené hrany, hodně prostoru, jemné animace.
Žádný balast. Cíl je klid, ne dashboard.
