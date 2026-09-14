# Mezera - Android

Nativní appka (Kotlin + Jetpack Compose). Sleduje, kterou appku otevřeš, a u hlídaných
appek dá nejdřív klidnou obrazovku: nádech, "proč tam jdeš" (min. počet znaků) a statistiky.

## Jak získat APK (bez lokálního Android SDK)

APK se buildí v cloudu přes GitHub Actions. Po pushnutí do `main`:

1. Jdi na **Releases** repozitáře -> release **latest** -> stáhni `Mezera.apk` (klidně rovnou v telefonu).
2. V telefonu povol instalaci z neznámých zdrojů a APK nainstaluj.
3. Otevři Mezeru a projdi dvě oprávnění:
   - **Přístupnost (Accessibility):** aby viděla, kterou appku otvíráš.
   - **Překrytí ostatních appek:** aby se klidná obrazovka mohla ukázat.
4. Vyber appky, které chceš hlídat. Hotovo.

APK je **debug** varianta (podepsaná debug klíčem) - pro osobní použití stačí, jen se
nedá nahrát na Google Play. Na sideload do vlastního telefonu je ideální.

## Jak to funguje

- `AppWatchService` = AccessibilityService, čte jen který balíček jde do popředí
  (ne obsah obrazovky). Nic neposílá ven.
- Když je to hlídaná appka a není zrovna "klid mezi dotazy", spustí `InterventionActivity`.
- Ta ukáže dýchání (cooldown), pak dotaz na důvod, a podle volby tě pustí dál nebo tě
  vrátí na plochu. Každý průchod se uloží lokálně (SharedPreferences).

## Lokální build (kdybys někdy měl Android SDK)

```bash
cd android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Potřebuješ JDK 17 a Android SDK (platform 35, build-tools 35). Detaily v
[../docs/android-build.md](../docs/android-build.md).
