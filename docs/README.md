# Web Pause

GitHub Pages publikuje obsah `docs/` z větve `master`. Web nepotřebuje build ani nové závislosti.

- `index.html`: český obsah, včetně návodu dostupného bez JavaScriptu.
- `styles.css`: sdílený vzhled a responzivní rozložení.
- `site.js`: anglické překlady podle `data-i18n` a přepínání jazyka.

Při úpravě textu uprav český obsah i odpovídající anglický klíč. Překlady obsahují jen vlastní statické HTML; nevkládej do nich uživatelský obsah. Elementy s `data-i18n` nevnořuj do sebe.

Jazyk se vybírá v pořadí: platné `?lang=cs` / `?lang=en`, uložená ruční volba, jazyk prohlížeče. Pro ostatní jazyky je výchozí angličtina. Ruční přepnutí ukládá volbu do localStorage, mění URL a zachovává kotvu i otevřené návody. Když úložiště není dostupné, přepínání dál funguje.

Anglický odkaz ke sdílení: https://valouseko.github.io/pause/?lang=en

## Kontrola změn

Otevři web přes lokální HTTP server a zkontroluj:

- CZ a EN na mobilní i desktopové šířce, včetně 320 px.
- Přepnutí jazyka, obnovení stránky, uloženou volbu a explicitní `?lang=`.
- Odkaz `?lang=en#omezene`, který má otevřít příslušný návod.
- Ovládání přepínače klávesnicí a rozbalovací návody.
- Přímý odkaz na APK, GitHub Releases a interní odkazy.
- Český základní obsah a rozbalovací návody bez JavaScriptu.

Návody vycházejí z nastavení aplikace a podpory výrobců odkazované přímo na stránce. Cesty v systémovém nastavení se mohou lišit podle modelu a verze systému; web netvrdí, že byla instalace fyzicky ověřena na všech telefonech.
