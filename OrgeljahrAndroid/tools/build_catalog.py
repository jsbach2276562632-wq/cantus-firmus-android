#!/usr/bin/env python3
"""Build the traceable, prepackaged Orgeljahr catalog database."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sqlite3
import unicodedata
from datetime import datetime, timezone
from pathlib import Path

import requests
from bs4 import BeautifulSoup


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "catalog-source"
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets" / "catalog"
SCHEMA_DIR = ROOT / "app" / "schemas" / "de.orgeljahr.demo.data.catalog.CatalogDatabase"
DATABASE_FILE = ASSET_DIR / "orgeljahr-catalog.db"
DATABASE_VERSION_FILE = ASSET_DIR / "catalog-version.txt"
RELATION_FILE = SOURCE_DIR / "chorale-relations-v1.json"

EG_URL = "https://de.wikipedia.org/wiki/Liste_der_Kirchenlieder_im_Evangelischen_Gesangbuch"
OGTL_URL = "https://www.kirchenrecht-nordkirche.de/document/46600"
USER_AGENT = "OrgeljahrDataBuilder/0.1 (private research catalog)"

MONTHS = {
    "Januar": 1,
    "Februar": 2,
    "März": 3,
    "April": 4,
    "Mai": 5,
    "Juni": 6,
    "Juli": 7,
    "August": 8,
    "September": 9,
    "Oktober": 10,
    "November": 11,
    "Dezember": 12,
}


def fetch(url: str) -> str:
    response = requests.get(url, timeout=60, headers={"User-Agent": USER_AGENT})
    response.raise_for_status()
    return response.text


def clean(value: str) -> str:
    return " ".join(value.replace("\u00a0", " ").split())


def slugify(value: str) -> str:
    value = value.lower()
    replacements = {"ä": "ae", "ö": "oe", "ü": "ue", "ß": "ss"}
    for source, replacement in replacements.items():
        value = value.replace(source, replacement)
    value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9]+", "-", value).strip("-")


def hymn_id(number_label: str) -> str:
    match = re.fullmatch(r"(\d+)(?:[.,](\d+))?", number_label)
    if not match:
        raise ValueError(f"Invalid EG number: {number_label}")
    base = int(match.group(1))
    suffix = f".{int(match.group(2))}" if match.group(2) else ""
    return f"eg1993-stamm:{base:03d}{suffix}"


def hymn_sort_order(number_label: str) -> int:
    match = re.fullmatch(r"(\d+)(?:[.,](\d+))?", number_label)
    if not match:
        raise ValueError(f"Invalid EG number: {number_label}")
    return int(match.group(1)) * 1000 + int(match.group(2) or 0)


def parse_eg(html: str) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    hymns: list[dict] = []
    category = ""
    for element in soup.select("h2,h3,h4,dd"):
        text = clean(element.get_text(" ", strip=True))
        if element.name in {"h2", "h3", "h4"}:
            category = text
            continue
        if element.find("dd"):
            continue
        match = re.match(r"^(\d+(?:[.,]\d+)?)\s+(.+)$", text)
        if not match or int(re.match(r"\d+", match.group(1)).group()) > 535:
            continue
        number_label = match.group(1).replace(",", ".")
        title = clean(match.group(2))
        hymns.append(
            {
                "id": hymn_id(number_label),
                "editionId": "eg1993-stamm",
                "numberLabel": number_label,
                "sortOrder": hymn_sort_order(number_label),
                "title": title,
                "firstLine": title,
                "category": category,
                "language": "de",
                "history": "",
                "rightsStatus": "METADATA_ONLY",
                "sourceId": "wikipedia-eg-list",
            }
        )

    labels = [item["numberLabel"] for item in hymns]
    base_numbers = {int(re.match(r"\d+", label).group()) for label in labels}
    if len(hymns) != 567 or len(labels) != len(set(labels)) or base_numbers != set(range(1, 536)):
        raise ValueError(
            f"Unexpected EG directory: rows={len(hymns)}, unique={len(set(labels))}, bases={len(base_numbers)}"
        )
    return hymns


def parse_marked(text: str, labels: list[str]) -> dict[str, str]:
    label_pattern = "|".join(re.escape(label) for label in labels)
    matches = list(re.finditer(rf"(?:^|\s/\s)({label_pattern})\s*", text))
    result: dict[str, str] = {}
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        result[match.group(1)] = text[match.end() : end].strip(" / ")
    return result


def parse_song_refs(text: str) -> list[dict]:
    body = re.sub(r"^L\.\s*", "", text).strip()
    if body in {"-", "entfällt"}:
        return []
    marker = re.compile(
        r"(?:^|\s/\s)(?P<collection>EG(?:\.[A-Z])?(?:\s+[A-Za-zÄÖÜäöü]+)?|fT|HuT|SJ|SvH|WL|Wwdl)\s+"
        r"(?P<number>\d+(?:[.,]\d+)?(?:\s*(?:/|und)\s*\d+(?:[.,]\d+)?)*)\s+"
    )
    matches = list(marker.finditer(body))
    songs: list[dict] = []
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(body)
        title = body[match.end() : end].strip(" / ")
        title = re.sub(r"\s+\(?Ö\)?$", "", title).strip()
        songs.append(
            {
                "collectionCode": clean(match.group("collection")),
                "numberLabel": re.sub(
                    r"\s*(?:/|und)\s*",
                    "/",
                    match.group("number").replace(",", "."),
                ),
                "title": title,
            }
        )
    if len(songs) != 2:
        raise ValueError(f"Expected two Wochenlieder, found {len(songs)} in: {text}")
    return songs


def section_for(index: int) -> str:
    if index <= 4:
        return "Advent"
    if index <= 12:
        return "Weihnachten"
    if index <= 17:
        return "Epiphanias"
    if index <= 22:
        return "Vorpassion"
    if index <= 34:
        return "Passion"
    if index <= 44:
        return "Ostern"
    if index <= 46:
        return "Pfingsten"
    if index == 47:
        return "Trinitatis"
    if index <= 77:
        return "Trinitatiszeit"
    return "Feste und Gedenktage"


def color_for(index: int, name: str) -> str:
    if index <= 4:
        return "Violett"
    if index <= 17:
        return "Weiß"
    if index <= 22:
        return "Grün"
    if index <= 30:
        return "Violett"
    if index in {31, 32, 33, 34}:
        return "Schwarz"
    if index <= 44:
        return "Weiß"
    if index <= 46:
        return "Rot"
    if index == 47:
        return "Weiß"
    if index == 57:
        return "Grün"
    if index == 58:
        return "Violett"
    if index <= 75:
        return "Grün"
    if index == 76:
        return "Weiß"
    if index == 77:
        return "Violett"
    red_terms = ("Apostel", "Evangelist", "Märtyrer", "Reformation", "Heiligen", "Augsburgischen")
    return "Rot" if any(term in name for term in red_terms) else "Weiß"


def priority_for(index: int, name: str) -> int:
    """Resolve calendar collisions without letting optional memorials replace Sundays."""
    if index >= 78:
        if name.startswith("Erster Sonntag im Oktober"):
            return 130
        if "Gedenktag der Reformation" in name:
            return 120
        return 20
    if 14 <= index <= 16:  # Epiphany Sundays replace colliding pre-passion Sundays.
        return 110
    if index == 17:  # Last Epiphany replaces a colliding pre-passion Sunday.
        return 130
    if index == 57:  # Default of the two official Israelsonntag alternatives.
        return 111
    if index == 58:
        return 110
    if index == 76:  # Default of the two official final-Sunday alternatives.
        return 131
    if index in {73, 74, 75, 77}:
        return 130
    return 100


def dynamic_date_rule(index: int, occasion_id: str) -> list[dict]:
    def rule(rule_type: str, *, offset: int | None = None, month: int | None = None,
             day: int | None = None, ordinal: int | None = None, suffix: str = "main") -> dict:
        return {
            "id": f"{occasion_id}:{suffix}",
            "occasionId": occasion_id,
            "ruleType": rule_type,
            "month": month,
            "dayOfMonth": day,
            "offsetDays": offset,
            "ordinal": ordinal,
            "weekday": None,
            "notes": "",
        }

    if 1 <= index <= 4:
        return [rule("ADVENT_OFFSET", offset=(index - 1) * 7)]
    fixed = {
        5: (12, 24), 6: (12, 24), 7: (12, 25), 8: (12, 26),
        10: (12, 31), 11: (1, 1), 13: (1, 6),
    }
    if index in fixed:
        month, day = fixed[index]
        return [rule("FIXED_DATE", month=month, day=day)]
    if index in {9, 12}:
        return [rule("SUNDAY_AFTER_FIXED_DATE", month=12, day=25, ordinal=1 if index == 9 else 2)]
    if 14 <= index <= 16:
        return [rule("EPIPHANY_PATTERN", ordinal=index - 13)]
    if index == 17:
        return [rule("EPIPHANY_PATTERN", ordinal=0)]
    easter_offsets = {
        18: -77, 19: -70, 20: -63, 21: -56, 22: -49, 23: -46,
        24: -42, 25: -35, 26: -28, 27: -21, 28: -14, 29: -7,
        30: -3, 31: -2, 32: -2, 33: -2, 34: -1, 35: -1,
        36: 0, 37: 1, 38: 7, 39: 14, 40: 21, 41: 28, 42: 35,
        43: 39, 44: 42, 45: 49, 46: 50, 47: 56,
    }
    if index in easter_offsets:
        return [rule("EASTER_OFFSET", offset=easter_offsets[index])]
    if 48 <= index <= 56:
        trinity_number = index - 47
        return [rule("TRINITY_OFFSET", offset=trinity_number * 7)]
    if index in {57, 58}:
        trinity_number = 10
        return [rule("TRINITY_OFFSET", offset=trinity_number * 7)]
    if 59 <= index <= 72:
        trinity_number = index - 48
        return [rule("TRINITY_OFFSET", offset=trinity_number * 7)]
    end_offsets = {73: -14, 74: -7, 75: -4, 76: 0, 77: 0}
    if index in end_offsets:
        return [rule("CHURCH_YEAR_END_OFFSET", offset=end_offsets[index])]
    raise ValueError(f"No dynamic date rule for occasion index {index}")


def fixed_date_rules(name: str, occasion_id: str) -> list[dict]:
    if name.startswith("Erster Sonntag im Oktober"):
        return [{
            "id": f"{occasion_id}:main", "occasionId": occasion_id,
            "ruleType": "SUNDAY_ON_OR_AFTER_FIXED_DATE", "month": 10, "dayOfMonth": 1,
            "offsetDays": None, "ordinal": 1, "weekday": None, "notes": "",
        }]
    date_matches = list(re.finditer(r"(\d{1,2})\.\s+(" + "|".join(MONTHS) + r")", name))
    if not date_matches:
        raise ValueError(f"Cannot parse fixed commemoration date: {name}")
    rules = []
    for index, match in enumerate(date_matches, 1):
        rules.append({
            "id": f"{occasion_id}:date-{index}", "occasionId": occasion_id,
            "ruleType": "FIXED_DATE", "month": MONTHS[match.group(2)],
            "dayOfMonth": int(match.group(1)), "offsetDays": None, "ordinal": None,
            "weekday": None, "notes": "Alternative" if len(date_matches) > 1 else "",
        })
    return rules


def parse_ogtl(html: str, hymn_ids: set[str]) -> dict[str, list[dict]]:
    soup = BeautifulSoup(html, "html.parser")
    paragraphs = soup.select("#documentContent > div.para")
    texts = [clean(paragraph.get_text(" ", strip=True)) for paragraph in paragraphs]
    start = texts.index("1. Sonntag im Advent")
    end = next(index for index, text in enumerate(texts) if text.startswith("28. Dezember – Tag der unschuldigen Kinder"))
    if (end - start) % 2 != 0:
        raise ValueError("Unexpected OGTL paragraph pairing")

    occasions, date_rules, lectionaries, sermon_readings, occasion_hymns = [], [], [], [], []
    occasion_hymn_links = []
    names_seen: set[str] = set()
    occasion_index = 0
    for paragraph_index in range(start, end + 1, 2):
        occasion_index += 1
        raw_name = texts[paragraph_index]
        name = re.sub(r"\s+\d+\s*#\s*$", "", raw_name).strip()
        slug = slugify(name)
        if slug in names_seen:
            slug = f"{slug}-{occasion_index}"
        names_seen.add(slug)
        occasion_id = f"ogtl2018:{slug}"

        data_rows = [
            clean(node.get_text(" ", strip=True))
            for node in paragraphs[paragraph_index + 1].select("table tr > td > div.para")
            if clean(node.get_text(" ", strip=True))
        ]
        if len(data_rows) < 4:
            raise ValueError(f"Incomplete OGTL rows for {name}: {data_rows}")

        def marked_row(prefix: str, required: bool = True) -> str:
            value = next((row for row in data_rows if row.startswith(prefix)), "")
            if required and not value:
                raise ValueError(f"Missing {prefix} row for {name}: {data_rows}")
            return value

        readings = parse_marked(marked_row("Ev."), ["Ev.", "Ep.", "AT."])
        series = parse_marked(marked_row("I:"), ["I:", "II:", "III:", "IV:", "V:", "VI:"])
        propers = parse_marked(marked_row("Sp."), ["Sp.", "H.", "Ps."])
        songs = parse_song_refs(marked_row("L."))
        additional_row = marked_row("W.", required=False)
        additional = re.sub(r"^W\.\s*", "", additional_row).strip()

        alternative_group = None
        if occasion_index in {57, 58}:
            alternative_group = "israelsonntag"
        elif occasion_index in {76, 77}:
            alternative_group = "letzter-sonntag-kirchenjahr"

        occasions.append({
            "id": occasion_id,
            "slug": slug,
            "name": name,
            "season": section_for(occasion_index),
            "defaultColor": color_for(occasion_index, name),
            "alternativeGroup": alternative_group,
            "priority": priority_for(occasion_index, name),
            "description": "",
        })
        date_rules.extend(
            dynamic_date_rule(occasion_index, occasion_id)
            if occasion_index <= 77
            else fixed_date_rules(name, occasion_id)
        )
        lectionaries.append({
            "occasionId": occasion_id,
            "gospelReference": readings.get("Ev.", ""),
            "epistleReference": readings.get("Ep.", ""),
            "oldTestamentReference": readings.get("AT.", ""),
            "psalmReference": propers.get("Ps.", ""),
            "weeklyVerseReference": propers.get("Sp.", ""),
            "hallelujahReference": propers.get("H.", ""),
            "additionalReadings": additional,
            "sourceId": "ogtl-2018",
        })
        for number, roman in enumerate(["I:", "II:", "III:", "IV:", "V:", "VI:"], 1):
            sermon_readings.append({
                "occasionId": occasion_id,
                "seriesNumber": number,
                "scriptureReference": series.get(roman, ""),
            })
        for position, song in enumerate(songs, 1):
            linked_hymns = []
            if song["collectionCode"] == "EG":
                for number_label in song["numberLabel"].split("/"):
                    base_label = number_label.split(".")[0]
                    candidate = hymn_id(number_label)
                    base_candidate = hymn_id(base_label)
                    linked_hymn = (
                        candidate if candidate in hymn_ids
                        else base_candidate if base_candidate in hymn_ids
                        else None
                    )
                    if linked_hymn and linked_hymn not in linked_hymns:
                        linked_hymns.append(linked_hymn)
            occasion_hymn_id = f"{occasion_id}:song-{position}"
            occasion_hymns.append({
                "id": occasion_hymn_id,
                "occasionId": occasion_id,
                "position": position,
                "collectionCode": song["collectionCode"],
                "numberLabel": song["numberLabel"],
                "title": song["title"],
                "hymnId": linked_hymns[0] if linked_hymns else None,
            })
            occasion_hymn_links.extend(
                {"occasionHymnId": occasion_hymn_id, "hymnId": linked_hymn}
                for linked_hymn in linked_hymns
            )

    if len(occasions) != 108 or not 0 < len(occasion_hymns) <= 216 or len(sermon_readings) != 648:
        raise ValueError(
            f"Unexpected OGTL counts: occasions={len(occasions)}, songs={len(occasion_hymns)}, series={len(sermon_readings)}"
        )
    if len(lectionaries) != len(occasions) or any(not item["weeklyVerseReference"] for item in lectionaries):
        raise ValueError("Every OGTL occasion must carry an explicitly sourced Spruch reference")

    occasion_ids = {item["name"]: item["id"] for item in occasions}
    lectionary_by_occasion = {item["occasionId"]: item for item in lectionaries}
    expected_spruch = {
        "1. Sonntag im Advent": "Sach 9,9a",
        "Letzter Sonntag nach Epiphanias": "Jes 60,2",
        "12. Sonntag nach Trinitatis": "Jes 42,3a",
        "13. Sonntag nach Trinitatis": "Mt 25,40b",
    }
    for occasion_name, expected_reference in expected_spruch.items():
        actual_reference = lectionary_by_occasion[occasion_ids[occasion_name]]["weeklyVerseReference"]
        if actual_reference != expected_reference:
            raise ValueError(
                f"Unexpected Spruch for {occasion_name}: {actual_reference!r} != {expected_reference!r}"
            )

    rules_by_occasion = {item["occasionId"]: item for item in date_rules}
    expected_trinity_offsets = {
        "10. So. nach Trinitatis – Israelsonntag: Kirche und Israel": 70,
        "10. So. nach Trinitatis – Israelsonntag: Gedenktag der Zerstörung Jerusalems": 70,
        "11. Sonntag nach Trinitatis": 77,
        "12. Sonntag nach Trinitatis": 84,
        "13. Sonntag nach Trinitatis": 91,
        "24. Sonntag nach Trinitatis": 168,
    }
    for occasion_name, expected_offset in expected_trinity_offsets.items():
        actual_offset = rules_by_occasion[occasion_ids[occasion_name]]["offsetDays"]
        if actual_offset != expected_offset:
            raise ValueError(
                f"Unexpected Trinity offset for {occasion_name}: {actual_offset} != {expected_offset}"
            )

    multi_number_songs = {
        item["numberLabel"]: item["id"]
        for item in occasion_hymns
        if item["collectionCode"] == "EG" and "/" in item["numberLabel"]
    }
    for number_label in ("262/263", "147/535"):
        occasion_hymn_id = multi_number_songs.get(number_label)
        linked_count = sum(
            link["occasionHymnId"] == occasion_hymn_id
            for link in occasion_hymn_links
        )
        if not occasion_hymn_id or linked_count != 2:
            raise ValueError(f"Expected two EG links for {number_label}, got {linked_count}")
    return {
        "occasions": occasions,
        "dateRules": date_rules,
        "lectionaries": lectionaries,
        "sermonReadings": sermon_readings,
        "occasionHymns": occasion_hymns,
        "occasionHymnLinks": occasion_hymn_links,
    }


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def refresh_sources() -> tuple[list[dict], dict[str, list[dict]], dict]:
    hymns = parse_eg(fetch(EG_URL))
    ogtl = parse_ogtl(fetch(OGTL_URL), {hymn["id"] for hymn in hymns})
    write_json(SOURCE_DIR / "eg1993-stamm.json", hymns)
    write_json(SOURCE_DIR / "ogtl2018.json", ogtl)
    write_json(SOURCE_DIR / "source-manifest.json", {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "sources": [
            {"id": "wikipedia-eg-list", "url": EG_URL, "scope": "EG number/title/category metadata"},
            {"id": "ogtl-2018", "url": OGTL_URL, "scope": "official lectionary and hymn references"},
            {
                "id": "orgeljahr-chorale-relations-v1",
                "path": "chorale-relations-v1.json",
                "scope": "manually reviewed EG tune and public-domain organ-work relations",
            },
        ],
    })
    return hymns, ogtl, json.loads(RELATION_FILE.read_text(encoding="utf-8"))


def load_sources() -> tuple[list[dict], dict[str, list[dict]], dict]:
    return (
        json.loads((SOURCE_DIR / "eg1993-stamm.json").read_text(encoding="utf-8")),
        json.loads((SOURCE_DIR / "ogtl2018.json").read_text(encoding="utf-8")),
        json.loads(RELATION_FILE.read_text(encoding="utf-8")),
    )


def latest_room_schema() -> Path:
    candidates = [path for path in SCHEMA_DIR.glob("*.json") if path.stem.isdigit()]
    if not candidates:
        raise FileNotFoundError(f"No exported Room schema found in {SCHEMA_DIR}")
    return max(candidates, key=lambda path: int(path.stem))


def validate_relations(hymns: list[dict], relations: dict) -> None:
    required = {"sources", "persons", "melodies", "hymnMelodies", "works", "workMelodies"}
    missing = required - relations.keys()
    if missing:
        raise ValueError(f"Missing relation collections: {sorted(missing)}")

    hymn_ids = {item["id"] for item in hymns}
    source_ids = {item["id"] for item in relations["sources"]}
    person_ids = {item["id"] for item in relations["persons"]}
    melody_ids = {item["id"] for item in relations["melodies"]}
    work_ids = {item["id"] for item in relations["works"]}
    if len(melody_ids) != len(relations["melodies"]) or len(work_ids) != len(relations["works"]):
        raise ValueError("Duplicate melody or work IDs in relation source")
    if len(relations["melodies"]) < 12 or len(relations["works"]) < 25:
        raise ValueError("The reviewed anchor set is unexpectedly incomplete")

    primary_counts: dict[str, int] = {}
    linked_melodies: set[str] = set()
    mapped_hymn_ids: set[str] = set()
    hymn_melody_keys: set[tuple[str, str]] = set()
    for link in relations["hymnMelodies"]:
        if link["hymnId"] not in hymn_ids or link["melodyId"] not in melody_ids:
            raise ValueError(f"Invalid hymn-melody link: {link}")
        if link.get("sourceId") not in source_ids or link.get("confidence") != "VERIFIED":
            raise ValueError(f"Untraceable or unverified hymn-melody link: {link}")
        if "." in link["hymnId"].split(":")[-1]:
            raise ValueError(f"Anchor mappings must not implicitly target EG sub-items: {link}")
        key = (link["hymnId"], link["melodyId"])
        if key in hymn_melody_keys:
            raise ValueError(f"Duplicate hymn-melody link: {key}")
        hymn_melody_keys.add(key)
        mapped_hymn_ids.add(link["hymnId"])
        if link.get("isPrimary"):
            primary_counts[link["hymnId"]] = primary_counts.get(link["hymnId"], 0) + 1
        linked_melodies.add(link["melodyId"])
    if any(count != 1 for count in primary_counts.values()) or set(primary_counts) != mapped_hymn_ids:
        raise ValueError(f"Every mapped EG entry must have exactly one primary tune: {primary_counts}")

    work_keys: set[tuple[str | None, str]] = set()
    for work in relations["works"]:
        key = (work.get("composerPersonId"), work["catalogNumber"])
        if key in work_keys:
            raise ValueError(f"Duplicate composer/catalog work key: {key}")
        work_keys.add(key)
        if work.get("composerPersonId") not in person_ids or work.get("sourceId") not in source_ids:
            raise ValueError(f"Untraceable composer or work source: {work}")
        if work.get("scoring") != "Orgel" or work.get("rightsStatus") != "PUBLIC_DOMAIN":
            raise ValueError(f"Anchor set only accepts public-domain organ works: {work}")

    related_works: set[str] = set()
    work_linked_melodies: set[str] = set()
    for link in relations["workMelodies"]:
        if link["workId"] not in work_ids or link["melodyId"] not in melody_ids:
            raise ValueError(f"Invalid work-melody link: {link}")
        if link.get("sourceId") not in source_ids or link.get("confidence") != "VERIFIED":
            raise ValueError(f"Untraceable or unverified work-melody link: {link}")
        if link.get("relationType") != "CHORALE_BASIS":
            raise ValueError(f"Unsupported first-release relation type: {link}")
        related_works.add(link["workId"])
        work_linked_melodies.add(link["melodyId"])
    if related_works != work_ids:
        raise ValueError("Every reviewed work must participate in a work-melody relationship")
    if linked_melodies != melody_ids or work_linked_melodies - linked_melodies:
        raise ValueError("Every reviewed tune must map to an EG entry before works may reference it")


def create_schema(connection: sqlite3.Connection, schema: dict) -> None:
    database = schema["database"]
    for entity in database["entities"]:
        table_name = entity["tableName"]
        connection.execute(entity["createSql"].replace("${TABLE_NAME}", table_name))
        for index in entity.get("indices", []):
            connection.execute(index["createSql"].replace("${TABLE_NAME}", table_name))
        for trigger in entity.get("contentSyncTriggers", []):
            connection.execute(trigger)
    for query in database["setupQueries"]:
        connection.execute(query)


def insert_rows(connection: sqlite3.Connection, table: str, rows: list[dict]) -> None:
    if not rows:
        return
    columns = list(rows[0])
    sql = f"INSERT INTO {table} ({', '.join(columns)}) VALUES ({', '.join('?' for _ in columns)})"
    connection.executemany(sql, [[row[column] for column in columns] for row in rows])


def build_database(hymns: list[dict], ogtl: dict[str, list[dict]], relations: dict) -> None:
    validate_relations(hymns, relations)
    schema_file = latest_room_schema()
    schema = json.loads(schema_file.read_text(encoding="utf-8"))
    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    if DATABASE_FILE.exists():
        DATABASE_FILE.unlink()
    connection = sqlite3.connect(DATABASE_FILE)
    try:
        connection.execute("PRAGMA foreign_keys=ON")
        create_schema(connection, schema)
        generated_at = datetime.now(timezone.utc).isoformat()
        base_sources = [
            {
                "id": "wikipedia-eg-list",
                "title": "Liste der Kirchenlieder im Evangelischen Gesangbuch",
                "publisher": "Wikipedia-Autorinnen und -Autoren",
                "url": EG_URL,
                "accessedOn": generated_at[:10],
                "license": "CC BY-SA 4.0; only factual directory metadata imported",
                "notes": "Titles, EG numbers and thematic headings; no lyrics or notation imported.",
            },
            {
                "id": "ogtl-2018",
                "title": "Ordnung gottesdienstlicher Texte und Lieder",
                "publisher": "VELKD / UEK",
                "url": OGTL_URL,
                "accessedOn": generated_at[:10],
                "license": "Official church-law source; bibliographic and liturgical factual data",
                "notes": "Version adopted in 2017 and effective from Advent 2018.",
            },
        ]
        insert_rows(connection, "sources", base_sources + relations["sources"])
        insert_rows(connection, "hymnal_editions", [{
            "id": "eg1993-stamm",
            "shortName": "EG",
            "fullName": "Evangelisches Gesangbuch 1993 – Stammteil",
            "regionCode": "DE-AT-CORE",
            "publicationYear": 1993,
            "validFrom": "1993-01-01",
            "validUntil": None,
            "isNationalCore": 1,
        }])
        insert_rows(connection, "hymns", hymns)
        insert_rows(connection, "persons", relations["persons"])
        insert_rows(connection, "melodies", relations["melodies"])
        insert_rows(connection, "hymn_melodies", relations["hymnMelodies"])
        insert_rows(connection, "musical_works", relations["works"])
        insert_rows(connection, "work_melodies", relations["workMelodies"])
        insert_rows(connection, "liturgical_occasions", ogtl["occasions"])
        insert_rows(connection, "liturgical_date_rules", ogtl["dateRules"])
        insert_rows(connection, "lectionaries", ogtl["lectionaries"])
        insert_rows(connection, "sermon_readings", ogtl["sermonReadings"])
        insert_rows(connection, "occasion_hymns", ogtl["occasionHymns"])
        insert_rows(connection, "occasion_hymn_links", ogtl["occasionHymnLinks"])
        tune_names: dict[str, list[str]] = {}
        melody_names = {item["id"]: item["tuneName"] for item in relations["melodies"]}
        for link in relations["hymnMelodies"]:
            tune_names.setdefault(link["hymnId"], []).append(melody_names[link["melodyId"]])
        insert_rows(connection, "hymn_search", [
            {
                "rowid": index,
                "hymnId": hymn["id"],
                "numberLabel": hymn["numberLabel"],
                "title": hymn["title"],
                "firstLine": hymn["firstLine"],
                "contributors": "",
                "tuneNames": " ".join(tune_names.get(hymn["id"], [])),
            }
            for index, hymn in enumerate(hymns, 1)
        ])
        insert_rows(connection, "catalog_metadata", [
            {"key": "content_version", "value": generated_at[:10]},
            {"key": "scope", "value": "EG 1993 Stammteil + OGTL 2018 nationwide core"},
            {"key": "hymn_entry_count", "value": str(len(hymns))},
            {"key": "hymn_base_number_count", "value": "535"},
            {"key": "liturgical_occasion_count", "value": str(len(ogtl["occasions"]))},
            {"key": "occasion_hymn_count", "value": str(len(ogtl["occasionHymns"]))},
            {"key": "occasion_hymn_link_count", "value": str(len(ogtl["occasionHymnLinks"]))},
            {"key": "reviewed_melody_count", "value": str(len(relations["melodies"]))},
            {"key": "reviewed_work_count", "value": str(len(relations["works"]))},
            {"key": "relation_schema_version", "value": relations.get("version", "1")},
        ])
        connection.commit()

        foreign_key_errors = connection.execute("PRAGMA foreign_key_check").fetchall()
        integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
        if foreign_key_errors or integrity != "ok":
            raise ValueError(f"Database validation failed: fk={foreign_key_errors}, integrity={integrity}")
    finally:
        connection.close()
    DATABASE_VERSION_FILE.write_text(
        hashlib.sha256(DATABASE_FILE.read_bytes()).hexdigest() + "\n",
        encoding="utf-8",
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--refresh", action="store_true", help="Download and re-parse upstream sources")
    args = parser.parse_args()
    hymns, ogtl, relations = refresh_sources() if args.refresh else load_sources()
    build_database(hymns, ogtl, relations)
    print(f"Built {DATABASE_FILE}")
    print(f"EG entries: {len(hymns)} (535 base numbers)")
    print(f"Liturgical occasions: {len(ogtl['occasions'])}")
    print(f"Date rules: {len(ogtl['dateRules'])}")
    print(f"Sermon readings: {len(ogtl['sermonReadings'])}")
    print(f"Occasion hymns: {len(ogtl['occasionHymns'])}")
    print(f"Occasion hymn links: {len(ogtl['occasionHymnLinks'])}")
    print(f"Reviewed melodies: {len(relations['melodies'])}")
    print(f"Reviewed organ works: {len(relations['works'])}")


if __name__ == "__main__":
    main()
