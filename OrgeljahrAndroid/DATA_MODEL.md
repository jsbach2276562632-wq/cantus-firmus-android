# Orgeljahr Datenmodell

## 1. Zwei getrennte Datenspeicher

Orgeljahr behandelt redaktionelle Katalogdaten und private Nutzerdaten getrennt.

```text
Redaktionelle Quelldateien
        │ prüfen und erzeugen
        ▼
orgeljahr-catalog.db             Private App-Daten
(EG, Melodien, Werke,            (Gottesdienste und Leseposition)
 Kirchenjahr, Quellen)                     │
        │                                  ├── gottesdienste.json
        ▼                                  └── attachments/
CatalogDatabase (Room)
```

`orgeljahr-catalog.db` darf bei einem Katalog-Update vollständig ersetzt werden. Die privaten Gottesdienste und hochgeladenen Noten liegen außerhalb dieses Katalogs und bleiben dabei erhalten.

Der erste unterstützte Gesangbuchbestand ist nur der bundesweit gemeinsame Stammteil:

```text
editionId = eg1993-stamm
Nummern   = EG 1–535
Region    = DE-AT-CORE
```

Regionalteile werden später als weitere `HymnalEdition`-Datensätze ergänzt. EG-Nummern sind deshalb keine globalen Primärschlüssel.

## 2. Wichtigste Objekte

### Gesangbuch

- `hymnal_editions`: Ausgabe und Geltungsbereich, z. B. `eg1993-stamm`.
- `hymns`: EG-Nummer, Titel, erste Zeile, Rubrik und Kurzgeschichte.
- `hymn_texts`: Textfassungen eines Liedes mit Autor- und Rechteangaben.
- `hymn_verses`: einzelne Strophen, damit Strophen gezielt ausgewählt werden können.
- `melodies`: eigenständige Choralmelodien einschließlich Tonart, Metrum, Herkunft und Pfaden zu MusicXML bzw. gerenderten Noten.
- `hymn_melodies`: n:m-Verknüpfung zwischen Liedern und Melodien, jeweils mit Quelle, Prüfstatus und redaktioneller Begründung.
- `persons` und `hymn_contributors`: Textautor, Melodieautor, Bearbeiter, Übersetzer usw.

Ein EG-Eintrag und eine Melodie sind nicht dasselbe. Mehrere Texte können dieselbe Melodie verwenden; ein Lied kann außerdem alternative Melodien besitzen.

### Orgelwerke

- `musical_works`: Orgelwerk, Kantate oder andere Komposition mit Komponist, BWV/BuxWV/LV usw., Besetzung, Gattung, Tonart und Dauer.
- `work_melodies`: n:m-Verknüpfung eines Werkes mit der zugrunde liegenden Choralmelodie, einschließlich Beziehungstyp, Quelle und Konfidenz.

Die Empfehlung läuft immer über die Melodie:

```text
EG-Lied ── hymn_melodies ── Melodie ── work_melodies ── Orgelwerk
```

Beispiel:

```text
EG 4 Nun komm, der Heiden Heiland
  → Melodie NUN KOMM, DER HEIDEN HEILAND
  → BWV 599, BWV 659, BWV 660, BWV 661, BuxWV 211 …
```

### Kirchenjahr

- `liturgical_occasions`: Sonntag, Fest- oder Gedenktag, Kirchenjahreszeit und liturgische Farbe.
- `liturgical_date_rules`: feste Daten oder Regeln relativ zu Advent, Weihnachten, Ostern, Trinitatis bzw. Kirchenjahresende.
- `lectionaries`: Evangelium, Epistel, AT, Psalm, Wochen-/Tagesspruch und Hallelujavers als Bibelstellen.
- `sermon_readings`: Predigttexte der Reihen I–VI.
- `occasion_hymns`: die zwei Wochen- oder Tageslieder. Ein Lied außerhalb des EG-Stammteils kann zunächst als externe Referenz ohne `hymnId` gespeichert werden.
- `occasion_hymn_links`: n:m-Verknüpfung für Angaben, die auf mehrere EG-Einträge verweisen, z. B. EG 262/263 oder EG 147/535.

Die sechsjährige Predigtreihe beginnt mit Reihe I im Kirchenjahr 2018/2019 und wird aus dem Beginn des Kirchenjahres berechnet, nicht fest in einen Kalender geschrieben.

Die Kalenderauswahl berücksichtigt außerdem liturgische Kollisionen: beide Israelsonntag-Formulare teilen dasselbe Datum, der Letzte Sonntag nach Epiphanias geht einer kollidierenden Vorpassionsordnung vor und die letzten Sonntage des Kirchenjahres gehen einer gleichzeitigen Trinitatiszählung vor. `Sp.` wird ausschließlich als amtliche Bibelstellenreferenz gespeichert; ein fehlender Katalogeintrag wird nie durch einen Beispieltext ersetzt.

## 3. Warum stabile IDs verwendet werden

Beispiele:

```text
hymn:     eg1993-stamm:004
melody:   tune:eg1993-004-primary
work:     work:bach:bwv-659
occasion: ogtl2018:advent-1
source:   ogtl-2018
```

Titel und Nummern können sich in einer neuen Ausgabe ändern. Stabile IDs erhalten Beziehungen und erlauben später Konkordanzen zwischen EG 1993 und dem neuen Gesangbuch.

## 4. Rechte und Quellen

Jeder Text, jede Melodie und jedes Werk besitzt einen Rechtewert:

- `PUBLIC_DOMAIN`: vollständiger Inhalt darf aus einer belegten gemeinfreien Quelle eingebaut werden.
- `LICENSED`: vollständiger Inhalt nur gemäß dokumentierter Lizenz.
- `METADATA_ONLY`: Titel, Nummer und Sachangaben sind vorhanden, aber kein geschützter Volltext bzw. Notensatz.
- `UNKNOWN`: darf vor der Veröffentlichung nicht als vollständiger Inhalt exportiert werden.

`sources` speichert Titel, Herausgeber, URL, Abrufdatum, Lizenz und redaktionelle Notizen. Historische Angaben und beide Beziehungskanten müssen mindestens eine Quelle besitzen. Nur mit `VERIFIED` geprüfte Beziehungen gelangen in die Empfehlung.

## 5. Datenproduktion

Die 535 EG-Einträge werden nicht von Hand in Kotlin geschrieben. Vorgesehen ist folgende Pipeline:

```text
catalog-source/*.json oder *.csv
        │
        ├── Pflichtfelder prüfen
        ├── EG-Nummern und stabile IDs prüfen
        ├── Fremdschlüssel prüfen
        ├── doppelte Melodien erkennen
        ├── Rechte und Quellen prüfen
        ├── MusicXML validieren
        └── Kirchenjahr-Regeln über mehrere Jahre testen
                         │
                         ▼
              orgeljahr-catalog.db
                         │
                         ▼
        app/src/main/assets/catalog/
```

Room lädt später die vorgefertigte Datenbank mit `createFromAsset()`. Das Schema wird bei der App-Kompilierung exportiert und mit der gelieferten SQLite-Datei verglichen.

## 6. Such- und Empfehlungsweg

`hymn_search` ist eine SQLite-FTS-Tabelle für EG-Nummer, Titel, erste Zeile, beteiligte Personen und Melodienamen.

Der Kernquery für „Bach/Buxtehude dieser Woche“ lautet logisch:

```text
heutiges Datum
  → berechneter Sonn-/Festtag
  → occasion_hymns
  → hymns
  → hymn_melodies
  → melodies
  → work_melodies
  → musical_works
```

## 7. Aktueller Übergangsstand

Das UI liest EG, Kirchenjahr, Melodien und Werkbeziehungen inzwischen aus `CatalogDatabase`. `SampleData.kt` bleibt ausschließlich als technischer Notfall-Fallback erhalten. Private Gottesdienste und PerformancePlan bleiben davon unberührt.

Die geprüfte Beziehungsschicht umfasst jetzt 128 Melodieidentitäten, 168 EG→Melodie-Kanten für 161 EG-Einträge und 98 gemeinfreie Orgelwerke von Johann Sebastian Bach, Dieterich Buxtehude, Johannes Brahms und Max Reger. Sie bildet sowohl gemeinsame Melodien (z. B. EG 79/223/350, EG 106/107, EG 129/133/213, EG 262/263 und EG 123/352) als auch echte Alternativmelodien (getrennte Melodien bei EG 64, EG 74, EG 299, EG 358, EG 360, EG 495 und EG 533) ab. Sie bleibt absichtlich konservativ: bloße Titelähnlichkeit erzeugt keine Beziehung, Subnummern erben nichts automatisch und umstrittene Zuschreibungen werden nicht empfohlen. Deshalb wird etwa BWV 649 trotz des identischen Texttitels nicht mit der abweichenden EG-246-Melodie gleichgesetzt; ebenso wird bei EG 495 ohne eindeutigen Melodienachweis noch kein Orgelwerk zugeordnet. Eigene Regressionstests schützen die korrigierte Zuordnung von Regers Op. 67 Nr. 2 zu `Alles ist an Gottes Segen` (EG 123/352) sowie die getrennten Melodien von EG 74, EG 107 und EG 358.
