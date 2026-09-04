# Cantus Firmus für Android

Dies ist das Android-Studio-Projekt der Demo-Version 0.16 von Cantus Firmus.
Die allgemeine Projektbeschreibung, Installationsanleitung und die Hinweise zu
Datenquellen und Rechten stehen in der [README im Repository-Stamm](../README.md).

## Lokaler Build

Voraussetzungen sind JDK 17 und Android SDK 35:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Die Anwendung speichert Gottesdienstpläne und importierte Dateien lokal. Es
werden keine Zugangsdaten oder Signaturschlüssel im Repository benötigt.
