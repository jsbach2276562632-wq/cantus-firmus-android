#!/usr/bin/env python3
"""Inspect the generated catalog and run relation-level regression checks."""

from __future__ import annotations

import sqlite3
from datetime import date, timedelta
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATABASE = ROOT / "app" / "src" / "main" / "assets" / "catalog" / "orgeljahr-catalog.db"

EXPECTED_BY_EG = {
    "4": {"BWV 599", "BWV 659", "BWV 660", "BWV 661", "BuxWV 211", "Op. 67 Nr. 29"},
    "10": {"BWV 658", "BuxWV 220", "BuxWV 221", "Op. 67 Nr. 42"},
    "23": {"BWV 604", "BuxWV 188", "BuxWV 189"},
    "24": {"BWV 606", "BWV 700", "BWV 701", "BWV 738", "BWV 769", "Op. 67 Nr. 40"},
    "25": {"BWV 607"},
    "27": {"BWV 609", "BuxWV 202", "Op. 67 Nr. 23"},
    "30": {"Op. 122 Nr. 8"},
    "67": {"BWV 601", "BuxWV 191", "BuxWV 192"},
    "70": {"BWV 739", "BuxWV 223", "Op. 67 Nr. 49"},
    "73": {"BWV 609", "BuxWV 202", "Op. 67 Nr. 23"},
    "76": {"BWV 622"},
    "79": {"BWV 623", "BuxWV 224"},
    "80": {"WoO 7"},
    "81": {"BWV 1093"},
    "85": {"BWV 727", "Op. 122 Nr. 9", "Op. 122 Nr. 10", "Op. 67 Nr. 14"},
    "91": {"BWV 1093"},
    "99": {"BWV 627"},
    "101": {"BWV 625", "BWV 695", "BWV 718"},
    "115": {"BWV 728", "Op. 67 Nr. 20"},
    "125": {"BWV 651", "BWV 652", "BuxWV 199", "BuxWV 200"},
    "126": {"BWV 631", "BWV 667"},
    "123": {"Op. 67 Nr. 2"},
    "139": {"Op. 67 Nr. 27"},
    "146": {"BWV 636", "BWV 682", "BWV 683", "BWV 737", "BuxWV 207", "BuxWV 219", "Op. 67 Nr. 39"},
    "147": {"BWV 645", "Op. 67 Nr. 41"},
    "152": {"Op. 67 Nr. 44"},
    "223": {"BWV 623", "BuxWV 224"},
    "241": {"Op. 67 Nr. 7"},
    "246": set(),
    "275": {"BWV 640", "BWV 712"},
    "289": {"BuxWV 212", "BuxWV 213", "BuxWV 214", "BuxWV 215"},
    "299": {"BWV 686", "BWV 687"},
    "321": {"Op. 67 Nr. 27"},
    "328": {"Op. 67 Nr. 7"},
    "341": {"BWV 734", "BuxWV 210", "Op. 67 Nr. 28"},
    "342": {"BWV 638", "BuxWV 186", "Op. 67 Nr. 10"},
    "344": {"BWV 636", "BWV 682", "BWV 683", "BWV 737", "BuxWV 207", "BuxWV 219", "Op. 67 Nr. 39"},
    "345": {"BuxWV 179"},
    "347": {"Op. 67 Nr. 5"},
    "350": {"BWV 623", "BuxWV 224"},
    "352": {"Op. 67 Nr. 2"},
    "353": {"Op. 67 Nr. 26"},
    "362": {"BuxWV 184", "Op. 67 Nr. 6"},
    "365": {"BWV 658", "BuxWV 220", "BuxWV 221", "Op. 67 Nr. 42"},
    "369": {"BWV 642", "BWV 647", "BWV 690", "BWV 691", "Op. 67 Nr. 45", "Op. 67 Nr. 46"},
    "372": {"Op. 67 Nr. 44"},
    "391": {"Op. 67 Nr. 35"},
    "396": {"BWV 610", "BWV 713", "BWV 1105", "Op. 67 Nr. 21"},
    "397": {"BWV 1115"},
    "398": {"BWV 615"},
    "402": {"Op. 67 Nr. 26"},
    "412": {"Op. 67 Nr. 25"},
    "414": {"Op. 67 Nr. 7"},
    "519": {"BWV 616"},
    "525": {"Op. 67 Nr. 25"},
    "526": {"BWV 728", "Op. 67 Nr. 20"},
    "516": {"Op. 67 Nr. 5"},
    "533": {"Op. 67 Nr. 5"},
    "535": {"BWV 645", "Op. 67 Nr. 41"},
}

MAPPED_WITHOUT_WORKS = {
    "7", "11", "36", "37", "58", "100", "108", "110", "128", "136",
    "149", "196", "200", "222", "243", "244", "293", "295", "302", "320",
    "324", "331", "346", "351", "405", "441", "450", "495", "498", "502",
    "518", "9", "14", "32", "34", "39", "45", "68", "74", "134", "140",
    "141", "144", "154", "250", "251", "274", "290", "294", "300", "308",
    "333", "357", "358", "359", "377", "378", "401", "452", "485",
    "16", "19", "56", "64", "65", "94", "96", "97", "98", "116", "117",
    "129", "133", "137", "142", "153", "199", "213", "225", "235", "237",
    "253", "262", "263", "264", "267", "268", "269", "271", "309", "312",
    "313", "329", "360", "363", "388", "512", "106", "107", "349", "442",
}


def first_advent(year: int) -> date:
    value = date(year, 11, 27)
    return value + timedelta(days=(6 - value.weekday()) % 7)


def easter(year: int) -> date:
    a = year % 19
    b, c = divmod(year, 100)
    d, e = divmod(b, 4)
    f = (b + 8) // 25
    g = (b - f + 1) // 3
    h = (19 * a + b - d - g + 15) % 30
    i, k = divmod(c, 4)
    l = (32 + 2 * e + 2 * i - h - k) % 7
    m = (a + 11 * h + 22 * l) // 451
    month = (h + l - 7 * m + 114) // 31
    day = ((h + l - 7 * m + 114) % 31) + 1
    return date(year, month, day)


def sunday(value: date, *, include: bool) -> date:
    delta = (6 - value.weekday()) % 7
    if not include and delta == 0:
        delta = 7
    return value + timedelta(days=delta)


def resolve_rule(rule: sqlite3.Row, church_year_start: int) -> date | None:
    start = first_advent(church_year_start)
    end = first_advent(church_year_start + 1)
    offset = rule["offsetDays"] or 0
    rule_type = rule["ruleType"]
    if rule_type == "ADVENT_OFFSET":
        return start + timedelta(days=offset)
    if rule_type == "EASTER_OFFSET":
        return easter(church_year_start + 1) + timedelta(days=offset)
    if rule_type == "TRINITY_OFFSET":
        return easter(church_year_start + 1) + timedelta(days=56 + offset)
    if rule_type == "CHURCH_YEAR_END_OFFSET":
        return end - timedelta(days=7) + timedelta(days=offset)
    if rule_type == "EPIPHANY_PATTERN":
        calendar_year = church_year_start + 1
        last = date(calendar_year, 2, 2) - timedelta(
            days=(date(calendar_year, 2, 2).weekday() - 6) % 7
        )
        if rule["ordinal"] == 0:
            return last
        candidate = sunday(date(calendar_year, 1, 6), include=False) + timedelta(
            weeks=(rule["ordinal"] or 1) - 1
        )
        return candidate if candidate < last else None
    if rule_type in {"FIXED_DATE", "SUNDAY_AFTER_FIXED_DATE", "SUNDAY_ON_OR_AFTER_FIXED_DATE"}:
        fixed = next(
            (
                date(year, rule["month"], rule["dayOfMonth"])
                for year in (church_year_start, church_year_start + 1)
                if start <= date(year, rule["month"], rule["dayOfMonth"]) < end
            ),
            None,
        )
        if fixed is None or rule_type == "FIXED_DATE":
            return fixed
        resolved = sunday(fixed, include=rule_type == "SUNDAY_ON_OR_AFTER_FIXED_DATE")
        return resolved + timedelta(weeks=(rule["ordinal"] or 1) - 1)
    raise AssertionError(f"Unhandled rule type: {rule_type}")


def selected_occasion(connection: sqlite3.Connection, value: date) -> str:
    church_year_start = value.year if value >= first_advent(value.year) else value.year - 1
    rows = connection.execute(
        """
        SELECT o.name, o.priority, r.*
        FROM liturgical_occasions o
        JOIN liturgical_date_rules r ON r.occasionId = o.id
        ORDER BY o.priority DESC, o.name
        """
    ).fetchall()
    return next(
        row["name"] for row in rows
        if resolve_rule(row, church_year_start) == value
    )


def scalar(connection: sqlite3.Connection, sql: str, parameters: tuple = ()) -> int | str:
    row = connection.execute(sql, parameters).fetchone()
    if row is None:
        raise AssertionError(f"Query returned no rows: {sql}")
    return row[0]


def main() -> None:
    connection = sqlite3.connect(DATABASE)
    connection.row_factory = sqlite3.Row
    try:
        connection.execute("PRAGMA foreign_keys=ON")
        assert scalar(connection, "PRAGMA integrity_check") == "ok"
        assert connection.execute("PRAGMA foreign_key_check").fetchall() == []
        assert scalar(connection, "SELECT COUNT(*) FROM melodies") == 150
        assert scalar(connection, "SELECT COUNT(*) FROM musical_works") == 98
        assert scalar(connection, "SELECT COUNT(*) FROM hymn_melodies") == 193
        assert scalar(connection, "SELECT COUNT(*) FROM work_melodies") == 98
        assert scalar(connection, "SELECT COUNT(*) FROM occasion_hymn_links") == 172
        assert scalar(connection, "SELECT COUNT(*) FROM hymn_melodies WHERE sourceId IS NULL") == 0
        assert scalar(connection, "SELECT COUNT(*) FROM work_melodies WHERE sourceId IS NULL") == 0
        assert scalar(connection, "SELECT COUNT(*) FROM hymn_melodies WHERE confidence != 'VERIFIED'") == 0
        assert scalar(connection, "SELECT COUNT(*) FROM work_melodies WHERE confidence != 'VERIFIED'") == 0

        relation_sql = """
            SELECT w.catalogNumber
            FROM hymns h
            JOIN hymn_melodies hm ON hm.hymnId = h.id
            JOIN work_melodies wm ON wm.melodyId = hm.melodyId
            JOIN musical_works w ON w.id = wm.workId
            WHERE h.numberLabel = ?
        """
        for number, expected in EXPECTED_BY_EG.items():
            actual = {row[0] for row in connection.execute(relation_sql, (number,))}
            assert actual == expected, f"EG {number}: expected {expected}, got {actual}"

        for number in MAPPED_WITHOUT_WORKS:
            actual = {row[0] for row in connection.execute(relation_sql, (number,))}
            assert actual == set(), f"EG {number}: expected no verified works, got {actual}"

        # Every mapped hymn has exactly one primary relation, even if multiple
        # tune variants are present.
        assert connection.execute(
            """
            SELECT hm.hymnId
            FROM hymn_melodies hm
            GROUP BY hm.hymnId
            HAVING SUM(CASE WHEN hm.isPrimary = 1 THEN 1 ELSE 0 END) != 1
            """
        ).fetchall() == []

        # Liederdatenbank regressions: similar text titles must not merge
        # historically distinct tunes, and alternatives stay separate.
        melody_sql = """
            SELECT hm.melodyId, hm.isPrimary
            FROM hymn_melodies hm
            JOIN hymns h ON h.id = hm.hymnId
            WHERE h.numberLabel = ?
        """
        expected_tunes = {
            "165": {("tune:wunderbarer-koenig-neander-1680", 1)},
            "255": {("tune:eg1993-366-primary", 1)},
            "316": {("tune:lobe-den-herren-stralsund-1665", 1)},
            "317": {("tune:lobe-den-herren-stralsund-1665", 1)},
            "327": {("tune:wunderbarer-koenig-neander-1680", 1)},
            "366": {("tune:eg1993-366-primary", 1)},
            "74": {
                ("tune:eg1993-442-primary", 1),
                ("tune:eg1993-300-primary", 0),
            },
            "107": {("tune:eg1993-106-primary", 1)},
            "358": {
                ("tune:eg1993-357-primary", 1),
                ("tune:eg1993-349-primary", 0),
            },
        }
        for number, expected in expected_tunes.items():
            actual = {tuple(row) for row in connection.execute(melody_sql, (number,))}
            assert actual == expected, f"EG {number}: expected tunes {expected}, got {actual}"

        # EG 107 must never inherit the EG 79 organ works merely because both
        # text titles begin with ‘Wir danken dir, Herr Jesu Christ’.
        assert connection.execute(relation_sql, ("107",)).fetchall() == []

        # Ambiguous shared title prefixes and EG sub-items must never inherit an anchor relation.
        for number in ("102", "215", "178.2"):
            assert connection.execute(relation_sql, (number,)).fetchall() == [], number

        # EG 299 prints two distinct melodies. Only Luther's melody is currently
        # linked to the verified Bach settings; Dachstein must not inherit them.
        assert scalar(
            connection,
            "SELECT COUNT(*) FROM hymn_melodies hm JOIN hymns h ON h.id = hm.hymnId WHERE h.numberLabel = '299'",
        ) == 2
        assert scalar(
            connection,
            "SELECT COUNT(*) FROM work_melodies WHERE melodyId = 'tune:eg1993-299-dachstein-1524'",
        ) == 0

        # EG 495 also prints two distinct melodies. Both remain metadata-only
        # until an organ-work source resolves which one a setting actually uses.
        assert scalar(
            connection,
            "SELECT COUNT(*) FROM hymn_melodies hm JOIN hymns h ON h.id = hm.hymnId WHERE h.numberLabel = '495'",
        ) == 2
        assert scalar(
            connection,
            """
            SELECT COUNT(*)
            FROM work_melodies
            WHERE melodyId IN (
                'tune:eg1993-495-braunschweig-1648',
                'tune:eg1993-495-regensburg-1675'
            )
            """,
        ) == 0

        for number in ("64", "358", "360", "533"):
            assert scalar(
                connection,
                "SELECT COUNT(*) FROM hymn_melodies hm JOIN hymns h ON h.id = hm.hymnId WHERE h.numberLabel = ?",
                (number,),
            ) == 2

        # Regression for the corrected Reger catalogue entry: Op. 67 No. 2
        # belongs to ‘Alles ist an Gottes Segen’, never to EG 81/91.
        assert scalar(
            connection,
            "SELECT title FROM musical_works WHERE catalogNumber = 'Op. 67 Nr. 2'",
        ) == "Alles ist an Gottes Segen"

        expected_spruch = {
            "1. Sonntag im Advent": "Sach 9,9a",
            "Letzter Sonntag nach Epiphanias": "Jes 60,2",
            "12. Sonntag nach Trinitatis": "Jes 42,3a",
            "13. Sonntag nach Trinitatis": "Mt 25,40b",
        }
        for occasion_name, expected_reference in expected_spruch.items():
            actual_reference = scalar(
                connection,
                """
                SELECT l.weeklyVerseReference
                FROM lectionaries l
                JOIN liturgical_occasions o ON o.id = l.occasionId
                WHERE o.name = ?
                """,
                (occasion_name,),
            )
            assert actual_reference == expected_reference, (
                occasion_name, expected_reference, actual_reference
            )

        expected_offsets = {
            "10. So. nach Trinitatis – Israelsonntag: Kirche und Israel": 70,
            "10. So. nach Trinitatis – Israelsonntag: Gedenktag der Zerstörung Jerusalems": 70,
            "11. Sonntag nach Trinitatis": 77,
            "12. Sonntag nach Trinitatis": 84,
            "13. Sonntag nach Trinitatis": 91,
            "24. Sonntag nach Trinitatis": 168,
        }
        for occasion_name, expected_offset in expected_offsets.items():
            actual_offset = scalar(
                connection,
                """
                SELECT r.offsetDays
                FROM liturgical_date_rules r
                JOIN liturgical_occasions o ON o.id = r.occasionId
                WHERE o.name = ?
                """,
                (occasion_name,),
            )
            assert actual_offset == expected_offset, (
                occasion_name, expected_offset, actual_offset
            )

        for number_label in ("262/263", "147/535"):
            assert scalar(
                connection,
                """
                SELECT COUNT(*)
                FROM occasion_hymn_links link
                JOIN occasion_hymns oh ON oh.id = link.occasionHymnId
                WHERE oh.numberLabel = ?
                """,
                (number_label,),
            ) == 2

        expected_2026 = {
            date(2026, 2, 1): "Letzter Sonntag nach Epiphanias",
            date(2026, 8, 9): "10. So. nach Trinitatis – Israelsonntag: Kirche und Israel",
            date(2026, 8, 16): "11. Sonntag nach Trinitatis",
            date(2026, 8, 23): "12. Sonntag nach Trinitatis",
            date(2026, 8, 30): "13. Sonntag nach Trinitatis",
            date(2026, 11, 8): "Drittletzter Sonntag des Kirchenjahres",
            date(2026, 11, 15): "Vorletzter Sonntag des Kirchenjahres",
            date(2026, 11, 22): "Letzter Sonntag des Kirchenjahres: Ewigkeitssonntag",
        }
        for calendar_date, expected_name in expected_2026.items():
            actual_name = selected_occasion(connection, calendar_date)
            assert actual_name == expected_name, (calendar_date, expected_name, actual_name)

        first_advent_sql = """
            SELECT DISTINCT w.catalogNumber
            FROM liturgical_occasions o
            JOIN occasion_hymns oh ON oh.occasionId = o.id
            JOIN occasion_hymn_links ohl ON ohl.occasionHymnId = oh.id
            JOIN hymn_melodies hm ON hm.hymnId = ohl.hymnId
            JOIN work_melodies wm ON wm.melodyId = hm.melodyId
            JOIN musical_works w ON w.id = wm.workId
            WHERE o.name = '1. Sonntag im Advent'
              AND hm.confidence = 'VERIFIED'
              AND wm.confidence = 'VERIFIED'
        """
        first_advent = {row[0] for row in connection.execute(first_advent_sql)}
        assert EXPECTED_BY_EG["4"].issubset(first_advent), first_advent

        print(f"Catalog: {DATABASE}")
        print("Integrity and foreign keys: OK")
        print(
            "Reviewed hymn-to-tune relations: "
            f"{scalar(connection, 'SELECT COUNT(*) FROM hymn_melodies')} across "
            f"{scalar(connection, 'SELECT COUNT(DISTINCT hymnId) FROM hymn_melodies')} EG entries"
        )
        print(f"Reviewed tune identities: {scalar(connection, 'SELECT COUNT(*) FROM melodies')}")
        print(f"Verified public-domain organ works: {scalar(connection, 'SELECT COUNT(*) FROM musical_works')}")
        print("OGTL Spruch/date-rule anchors, 2026 calendar and multi-number EG links: OK")
        print(f"1. Advent verified recommendations: {', '.join(sorted(first_advent))}")
    finally:
        connection.close()


if __name__ == "__main__":
    main()
