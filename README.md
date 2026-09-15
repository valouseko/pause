# Pause

Appka, co ti dá vteřinu na rozmyšlenou, než skočíš do rozptylující aplikace. Inspirováno OneSec.

Když otevřeš appku (nebo web), na kterou si Pause nastavíš, překryje ji klidná obrazovka:

1. **Nádech / výdech** - dýchací animace na pár vteřin (cooldown, nedá se přeskočit).
2. **Proč tam jdeš?** - musíš slovy napsat důvod (min. počet znaků).
3. **Pokračuj, nebo si to rozmysli.** Každý průchod se zaloguje.
4. **Statistiky** - kolikrát tě zastavila, odhad ušetřeného času, důvody.

Funguje **na Windows i na Androidu**, napříč libovolnými appkami/weby. Tmavý OneSec vzhled.

## Odkazy

- **Landing + stažení:** https://valouseko.github.io/pause
- **APK napřímo:** https://github.com/valouseko/pause/releases/latest/download/Pause.apk
- **Repo:** https://github.com/valouseko/pause (veřejné)

## Struktura

- `windows/` - desktopová appka (Electron). Dá se rovnou spustit.
- `android/` - nativní appka (Kotlin + Jetpack Compose). APK se buildí v cloudu (GitHub Actions).
- `docs/` - landing page (GitHub Pages) + poznámky k buildu.

## Android - stažení a auto-update

APK stáhneš z landing page nebo z Releases. Po instalaci appka **sama hlídá aktualizace**:
při otevření zkontroluje `version.json` v Releases a nabídne stažení + instalaci nové verze.
Protože je APK podepsané **stálým klíčem**, nová verze se nainstaluje přes starou -
**oprávnění i statistiky zůstávají**. Detaily v `docs/android-build.md`.

## Windows - rychlý start

```powershell
cd windows
npm install
npm start
```

Běží v tray. Detaily v `windows/README.md`.

## Design

Tmavý OneSec styl: hluboké pozadí, jedna modrá/indigo, font Inter, zaoblené hrany,
dýchající kruh jako hrdina, hero souhrn s velkými čísly. Klid, žádný balast.
