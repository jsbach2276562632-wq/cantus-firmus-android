# Cantus Firmus for Android

Cantus Firmus is an experimental Android app for organists working in German Protestant churches. It brings together the church year, the core section of the *Evangelisches Gesangbuch*, hymn-tune relationships, organ repertoire, and a practical service reader.

The current installable build is [Cantus Firmus Demo 0.16](Cantus-Firmus-Demo-0.16.apk).
It is a debug-signed demonstration build for personal testing, not a production
release for Google Play.

## What it does

- Calculates the current Sunday or feast within the church year and shows the appointed readings and hymns.
- Provides a searchable catalogue of the nationwide core section of the *Evangelisches Gesangbuch*.
- Groups EG entries by melody, so that every hymn associated with a tune can be found from one page.
- Connects hymn tunes with relevant organ works by Bach, Buxtehude, Brahms, Reger, and other composers.
- Includes a main-service order and an empty, fully customisable service order.
- Lets the organist add sections, arrange them freely, and record an EG number, piece, registration, duration, and notes for each section.
- Imports personal PDF scores and images for use during the service.
- Presents the complete service as a full-screen performance plan. Every section begins with a clear information page, followed by its attached score pages. Empty sections still receive their own page.
- Keeps the screen awake, remembers the reading position, and preloads PDF pages for smoother page turns.

Service plans and imported files remain in the app's private local storage. No account, Google Play installation, or server connection is required.

## Installing the demo on an Android tablet

1. Download `Cantus-Firmus-Demo-0.16.apk` to the tablet.
2. Open it from the Files app.
3. If Android blocks the installation, allow the Files app to install applications from external sources.
4. Return to the APK and install it.

The exact name of the setting varies between Android and HarmonyOS versions. Uninstalling the app also removes its locally stored service plans and attachments. Backup and restore are not yet implemented.

## Project structure

The Android Studio project is in [`OrgeljahrAndroid`](OrgeljahrAndroid). Auditable catalogue source files are stored in [`OrgeljahrAndroid/catalog-source`](OrgeljahrAndroid/catalog-source), while the database generation and validation scripts are in [`OrgeljahrAndroid/tools`](OrgeljahrAndroid/tools).

The catalogue is generated from these source files rather than maintained as an opaque database file. This makes corrections reviewable and allows the database to be rebuilt consistently.

## Building

The project requires JDK 17 and Android SDK 35.

```powershell
cd OrgeljahrAndroid
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

The debug APK is written to `OrgeljahrAndroid/app/build/outputs/apk/debug/app-debug.apk`.

The checked-in demo APK is built for portfolio and device-testing purposes. A
production release should use a private release keystore configured outside the
repository; signing keys must never be committed here.

## Data and rights

The repository currently focuses on metadata, relationships between hymn tunes and public-domain repertoire, and documents imported by the user. It does not include a licensed digital edition of the *Evangelisches Gesangbuch*. Complete lyrics, copyrighted engraving, or other protected material should only be added when the relevant rights have been cleared.

The liturgical and musical catalogue is still being expanded and checked. This repository should not yet be treated as an authoritative reference edition.

## License

The application source code is licensed under the [MIT License](LICENSE).
Catalogue files remain subject to the source-specific attribution and rights
descriptions in `OrgeljahrAndroid/catalog-source`; the MIT license does not grant
additional rights to third-party metadata or user-imported documents.
