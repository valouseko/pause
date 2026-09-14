# Build APK - jak a proč přes cloud

## Proč ne lokálně

Tenhle stroj nemá Android toolchain (chybí JDK pro kompilaci, Android SDK, Gradle).
Doinstalovat to jde, ale je to velké a na tomhle stroji navíc **Avast láme SSL**, takže
stahování balíčků přes Node/Gradle občas dostane porušená data. Proto se APK buildí
na GitHub runnerech, kde je toolchain připravený a síť čistá.

## Jak to běží (GitHub Actions)

Workflow: [`.github/workflows/android.yml`](../.github/workflows/android.yml)

Na každý push do `main`, který sáhne na `android/**`:

1. Checkout + JDK 17 (Temurin).
2. `android-actions/setup-android` nainstaluje SDK a přijme licence.
3. `sdkmanager` doinstaluje `platforms;android-35` a `build-tools;35.0.0`.
4. `./gradlew assembleDebug` postaví `app-debug.apk`.
5. APK se nahraje jako **artifact** a zároveň do **Release `latest`** jako `Mezera.apk`.

Stáhnout: Releases -> `latest` -> `Mezera.apk`. Funguje i přímo z mobilního prohlížeče.

## Verze nástrojů

- Gradle 8.11.1 (wrapper je v repu)
- Android Gradle Plugin 8.7.3
- Kotlin 2.1.0 + Compose compiler plugin 2.1.0
- Compose BOM 2024.12.01
- compileSdk 35, targetSdk 34, minSdk 26

## Podpis a distribuce

Zatím se dělá jen **debug APK** (debug keystore z runneru). To stačí na sideload do
vlastního telefonu. Kdybychom chtěli release APK / Play Store, doplní se podepisovací
keystore do GitHub Secrets a `signingConfig` do `app/build.gradle.kts`.

## Lokální build (volitelně)

Když bys chtěl buildit na svém stroji:

1. Nainstaluj **JDK 17** (Temurin) a nastav `JAVA_HOME`.
2. Nainstaluj **Android command-line tools**, přes `sdkmanager` doinstaluj
   `platform-tools`, `platforms;android-35`, `build-tools;35.0.0` a přijmi licence
   (`sdkmanager --licenses`).
3. V `android/` vytvoř `local.properties` s `sdk.dir=C:\\cesta\\k\\Android\\Sdk`.
4. `./gradlew assembleDebug`.
