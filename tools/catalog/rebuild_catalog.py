#!/usr/bin/env python3
"""Reacquire source-visible chord sheets and build the packaged catalog.

The three databases in Catalog/ are immutable master inputs. Network responses
are parsed immediately and only the extracted presentation text is cached, so a
run can resume without retaining tens of gigabytes of source HTML.
"""

from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
import hashlib
import html
from html.parser import HTMLParser
import json
from pathlib import Path
import sqlite3
import sys
import time
from typing import Iterable
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MASTER_DIR = PROJECT_ROOT / "Catalog"
DEFAULT_CACHE = Path(__file__).with_name("presentation_cache.db")
DEFAULT_OUTPUT = Path(__file__).with_name("generated") / "catalog.db"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131 Safari/537.36"
)
EXTRACTOR_VERSION = 1
EXPECTED_COUNTS = {"UG": 8121, "GT": 19753, "ChordTela": 41973}

SOURCE_FILES = {
    "UG": "UG.db",
    "GT": "GT.db",
    "ChordTela": "chordtela.db",
}

SCHEMA = """
CREATE TABLE songs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source TEXT NOT NULL,
    source_url TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    title_slug TEXT NOT NULL,
    artist_slug TEXT NOT NULL,
    version_name TEXT,
    rating REAL,
    votes INTEGER,
    key_base TEXT,
    difficulty TEXT,
    category TEXT,
    region TEXT NOT NULL,
    chords TEXT NOT NULL,
    chord_count INTEGER NOT NULL,
    line_count INTEGER NOT NULL,
    content TEXT NOT NULL,
    presentation_content TEXT NOT NULL,
    content_hash TEXT NOT NULL,
    scraped_at TEXT NOT NULL
);
"""

class VisibleTextParser(HTMLParser):
    """Small HTML-to-text parser that preserves explicit source line breaks."""

    BREAK_TAGS = {"br", "p", "div", "pre", "section", "article", "li"}

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.parts: list[str] = []
        self.ignored_depth = 0

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag in {"script", "style", "svg"}:
            self.ignored_depth += 1
        elif not self.ignored_depth and tag in self.BREAK_TAGS:
            self.parts.append("\n")

    def handle_startendtag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if not self.ignored_depth and tag in self.BREAK_TAGS:
            self.parts.append("\n")

    def handle_endtag(self, tag: str) -> None:
        if tag in {"script", "style", "svg"} and self.ignored_depth:
            self.ignored_depth -= 1
        elif not self.ignored_depth and tag in self.BREAK_TAGS:
            self.parts.append("\n")

    def handle_data(self, data: str) -> None:
        if not self.ignored_depth:
            self.parts.append(data)

    def text(self) -> str:
        return normalize_text("".join(self.parts))


def normalize_text(value: str) -> str:
    value = html.unescape(value).replace("\r\n", "\n").replace("\r", "\n")
    lines = [line.expandtabs(4).rstrip().replace("\xa0", " ") for line in value.split("\n")]
    nonblank = [len(line) - len(line.lstrip()) for line in lines if line.strip()]
    margin = min(nonblank, default=0)
    if margin:
        lines = [line[margin:] if line.strip() else "" for line in lines]
    compact: list[str] = []
    for line in lines:
        if not line.strip() and compact and not compact[-1].strip():
            continue
        compact.append(line)
    lines = compact
    while lines and not lines[0].strip():
        lines.pop(0)
    while lines and not lines[-1].strip():
        lines.pop()
    return "\n".join(lines)


def strip_ug_markup(content: str) -> str:
    """Render Ultimate Guitar's semantic tab markup as visible plain text."""
    import re

    rendered = re.sub(r"\[ch\](.*?)\[/ch\]", r"\1", content, flags=re.I | re.S)
    rendered = re.sub(r"\[/?tab\]", "", rendered, flags=re.I)
    rendered = re.sub(r"\[(?:/?(?:b|i|u|s|color|size|url))[^]]*\]", "", rendered, flags=re.I)
    return normalize_text(rendered)


def extract_ug(page: str) -> str:
    """Extract wiki_tab.content from Ultimate Guitar's embedded page payload."""
    import re

    decoded = html.unescape(page)
    markers = list(re.finditer(r'"wiki_tab"\s*:', decoded))
    for marker in markers:
        candidate = decoded[marker.end():].lstrip()
        if not candidate.startswith("{"):
            continue
        try:
            obj, _ = json.JSONDecoder().raw_decode(candidate)
        except json.JSONDecodeError:
            continue
        content = obj.get("content")
        if isinstance(content, str) and "[ch]" in content.lower():
            result = strip_ug_markup(content)
            if result:
                return result

    # The current PHP renderer stores the same semantic tab under store.tab.
    for marker in re.finditer(r'"content"\s*:', decoded):
        candidate = decoded[marker.end():].lstrip()
        if not candidate.startswith('"'):
            continue
        try:
            content, _ = json.JSONDecoder().raw_decode(candidate)
        except json.JSONDecodeError:
            continue
        if isinstance(content, str) and "[ch]" in content.lower():
            result = strip_ug_markup(content)
            if result:
                return result
    raise ValueError("Ultimate Guitar wiki_tab.content was not found")


class ElementCaptureParser(HTMLParser):
    """Capture one provider-specific element, retaining its inner markup."""

    def __init__(self, predicate) -> None:
        super().__init__(convert_charrefs=False)
        self.predicate = predicate
        self.depth = 0
        self.captures: list[str] = []
        self.current: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if self.depth:
            self.depth += 1
            self.current.append(self.get_starttag_text())
        elif self.predicate(tag, dict(attrs)):
            self.depth = 1

    def handle_startendtag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if self.depth:
            self.current.append(self.get_starttag_text())

    def handle_endtag(self, tag: str) -> None:
        if not self.depth:
            return
        self.depth -= 1
        if self.depth:
            self.current.append(f"</{tag}>")
        else:
            self.captures.append("".join(self.current))
            self.current = []

    def handle_data(self, data: str) -> None:
        if self.depth:
            self.current.append(data)

    def handle_entityref(self, name: str) -> None:
        if self.depth:
            self.current.append(f"&{name};")

    def handle_charref(self, name: str) -> None:
        if self.depth:
            self.current.append(f"&#{name};")


def visible_fragment(fragment: str) -> str:
    parser = VisibleTextParser()
    parser.feed(fragment)
    parser.close()
    return parser.text()


def extract_chordtela(page: str) -> str:
    def is_sheet(tag: str, attrs: dict[str, str | None]) -> bool:
        classes = (attrs.get("class") or "").split()
        return tag == "div" and "telabox" in classes

    parser = ElementCaptureParser(is_sheet)
    parser.feed(page)
    candidates = [visible_fragment(item) for item in parser.captures]
    candidates = [item for item in candidates if item and len(item.splitlines()) >= 5]
    if not candidates:
        raise ValueError("ChordTela post-body was not found")
    return max(candidates, key=len)


class GuitarTunaSheetParser(HTMLParser):
    """Serialize GuitarTuna sections/lines from semantic SSR attributes."""

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.section_depth = 0
        self.line_depth = 0
        self.chord_depth = 0
        self.section_parts: list[str] = []
        self.line_text_parts: list[str] = []
        self.line_chords: list[str] = []
        self.lines: list[str] = []
        self.chord_total = 0

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attrs_dict = dict(attrs)
        classes = (attrs_dict.get("class") or "").split()
        if "part_type" in classes:
            self.section_depth = 1
            self.section_parts = []
        elif self.section_depth:
            self.section_depth += 1
        if tag == "div" and "line" in classes and "data-bar" in attrs_dict:
            self.line_depth = 1
            self.line_text_parts = []
            self.line_chords = []
        elif self.line_depth:
            self.line_depth += 1
        if self.line_depth and attrs_dict.get("data-chord") is not None:
            self.chord_depth = 1
            chord = (attrs_dict.get("data-chord") or "").strip()
            if chord:
                self.line_chords.append(chord)
                self.chord_total += 1
        elif self.chord_depth:
            self.chord_depth += 1

    def handle_endtag(self, tag: str) -> None:
        if self.chord_depth:
            self.chord_depth -= 1
        if self.section_depth:
            self.section_depth -= 1
            if not self.section_depth:
                title = "".join(self.section_parts).strip()
                if title:
                    if self.lines and self.lines[-1] != "":
                        self.lines.append("")
                    self.lines.append(f"[{title}]")
        if self.line_depth:
            self.line_depth -= 1
            if not self.line_depth:
                lyrics = "".join(self.line_text_parts).strip()
                if self.line_chords:
                    self.lines.append("   ".join(self.line_chords))
                if lyrics:
                    self.lines.append(lyrics)

    def handle_data(self, data: str) -> None:
        if self.section_depth:
            self.section_parts.append(data)
        elif self.line_depth and not self.chord_depth:
            self.line_text_parts.append(data)

    def text(self) -> str:
        return normalize_text("\n".join(self.lines))


def extract_gt(page: str) -> str:
    """Extract GuitarTuna's server-rendered visible chord sheet."""
    parser = GuitarTunaSheetParser()
    parser.feed(page)
    parser.close()
    result = parser.text()
    if len(result.splitlines()) >= 5 and parser.chord_total:
        return result
    raise ValueError("GuitarTuna chord sheet was not found")


EXTRACTORS = {"UG": extract_ug, "GT": extract_gt, "ChordTela": extract_chordtela}

def open_cache(path: Path) -> sqlite3.Connection:
    path.parent.mkdir(parents=True, exist_ok=True)
    db = sqlite3.connect(path)
    db.execute("PRAGMA journal_mode=WAL")
    db.execute(
        """CREATE TABLE IF NOT EXISTS presentations (
            source TEXT NOT NULL,
            source_url TEXT NOT NULL,
            presentation_content TEXT NOT NULL,
            extractor_version INTEGER NOT NULL,
            fetched_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY(source, source_url)
        )"""
    )
    db.execute(
        """CREATE TABLE IF NOT EXISTS failures (
            source TEXT NOT NULL,
            source_url TEXT NOT NULL,
            error TEXT NOT NULL,
            attempted_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY(source, source_url)
        )"""
    )
    columns = {row[1] for row in db.execute("PRAGMA table_info(presentations)")}
    if "extractor_version" not in columns:
        db.execute("ALTER TABLE presentations ADD COLUMN extractor_version INTEGER NOT NULL DEFAULT 0")
    return db


def fetch_page(url: str, timeout: float, attempts: int) -> str:
    last_error: Exception | None = None
    for attempt in range(attempts):
        try:
            request = Request(url, headers={"User-Agent": USER_AGENT, "Accept": "text/html"})
            with urlopen(request, timeout=timeout) as response:
                charset = response.headers.get_content_charset() or "utf-8"
                return response.read().decode(charset, errors="replace")
        except (HTTPError, URLError, TimeoutError) as exc:
            last_error = exc
            if attempt + 1 < attempts:
                time.sleep(min(2 ** attempt, 8))
    raise RuntimeError(f"fetch failed after {attempts} attempts: {last_error}")


def source_rows(
    master_dir: Path,
    only_sources: set[str] | None = None,
) -> Iterable[tuple[str, sqlite3.Row]]:
    for source, filename in SOURCE_FILES.items():
        if only_sources and source not in only_sources:
            continue
        path = master_dir / filename
        if not path.is_file():
            raise FileNotFoundError(path)
        db = sqlite3.connect(path)
        db.row_factory = sqlite3.Row
        try:
            count = db.execute("SELECT COUNT(*) FROM songs").fetchone()[0]
            if count != EXPECTED_COUNTS[source]:
                raise ValueError(f"{source}: expected {EXPECTED_COUNTS[source]} rows, got {count}")
            for row in db.execute("SELECT * FROM songs ORDER BY id"):
                yield source, row
        finally:
            db.close()


def acquire_one(
    source: str,
    url: str,
    timeout: float,
    attempts: int,
) -> tuple[str, str, str | None, str | None]:
    try:
        page = fetch_page(url, timeout, attempts)
        presentation = EXTRACTORS[source](page)
        if not presentation.strip():
            raise ValueError("extractor returned empty content")
        return source, url, presentation
    except Exception as exc:
        return source, url, None, str(exc)[:1000]


def acquire(args: argparse.Namespace) -> int:
    cache = open_cache(args.cache)
    completed = 0
    failed = 0
    only_sources = set(args.source) if args.source else None
    pending: list[tuple[str, str]] = []
    try:
        for source, row in source_rows(args.master_dir, only_sources):
            url = row["source_url"]
            exists = cache.execute(
                "SELECT 1 FROM presentations WHERE source=? AND source_url=? AND extractor_version=?",
                (source, url, EXTRACTOR_VERSION),
            ).fetchone()
            if exists:
                completed += 1
                continue
            pending.append((source, url))
            if args.limit and completed + len(pending) >= args.limit:
                break

        with ThreadPoolExecutor(max_workers=max(1, args.workers)) as pool:
            futures = {
                pool.submit(acquire_one, source, url, args.timeout, args.attempts): (source, url)
                for source, url in pending
            }
            for future in as_completed(futures):
                result = future.result()
                source, url = result[0], result[1]
                if result[2] is not None:
                    cache.execute(
                        "INSERT OR REPLACE INTO presentations("
                        "source,source_url,presentation_content,extractor_version) VALUES(?,?,?,?)",
                        (source, url, result[2], EXTRACTOR_VERSION),
                    )
                    cache.execute("DELETE FROM failures WHERE source=? AND source_url=?", (source, url))
                    completed += 1
                else:
                    cache.execute(
                        "INSERT OR REPLACE INTO failures(source,source_url,error,attempted_at) "
                        "VALUES(?,?,?,CURRENT_TIMESTAMP)",
                        (source, url, result[3]),
                    )
                    failed += 1
                cache.commit()
                if (completed + failed) % 100 == 0:
                    print(f"acquired={completed} failed={failed}", flush=True)
    finally:
        cache.close()
    print(f"acquired={completed} failed={failed}")
    return 1 if failed else 0


def unified_values(source: str, row: sqlite3.Row, presentation: str) -> tuple:
    names = set(row.keys())
    get = lambda name: row[name] if name in names else None
    content = row["content"]
    return (
        source,
        row["source_url"], row["title"], row["artist"],
        row["title_slug"], row["artist_slug"], get("version_name"),
        get("rating"), get("votes"), get("key_base"), get("difficulty"),
        get("category"), row["region"], row["chords"], row["chord_count"],
        row["line_count"], content, presentation,
        hashlib.sha256(content.encode("utf-8")).hexdigest(), row["scraped_at"],
    )


def build(args: argparse.Namespace) -> int:
    if not args.cache.is_file():
        raise FileNotFoundError(f"presentation cache not found: {args.cache}")
    cache = open_cache(args.cache)
    all_rows = list(source_rows(args.master_dir))
    missing = []
    presentations: dict[tuple[str, str], str] = {}
    for source, row in all_rows:
        cached = cache.execute(
            "SELECT presentation_content FROM presentations "
            "WHERE source=? AND source_url=? AND extractor_version=?",
            (source, row["source_url"], EXTRACTOR_VERSION),
        ).fetchone()
        if cached is None:
            missing.append((source, row["source_url"]))
        else:
            presentations[(source, row["source_url"])] = cached[0]
    cache.close()
    if missing:
        report = args.output.with_suffix(".missing.json")
        report.parent.mkdir(parents=True, exist_ok=True)
        report.write_text(json.dumps(
            [{"source": source, "source_url": url} for source, url in missing], indent=2
        ), encoding="utf-8")
        raise RuntimeError(f"refusing partial catalog: {len(missing)} presentations missing; see {report}")

    temp = args.output.with_suffix(".tmp.db")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if temp.exists():
        temp.unlink()
    output = sqlite3.connect(temp)
    try:
        output.executescript(SCHEMA)
        insert = """INSERT INTO songs(
            source,source_url,title,artist,title_slug,artist_slug,version_name,
            rating,votes,key_base,difficulty,category,region,chords,chord_count,
            line_count,content,presentation_content,content_hash,scraped_at
        ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"""
        output.executemany(
            insert,
            (unified_values(source, row, presentations[(source, row["source_url"])])
             for source, row in all_rows),
        )
        output.executescript("""
            CREATE INDEX idx_artist_slug ON songs(artist_slug);
            CREATE INDEX idx_title_slug ON songs(title_slug);
            CREATE INDEX idx_source ON songs(source);
            PRAGMA user_version=2;
        """)
        output.commit()
        audit_output(output, args.master_dir)
        output.execute("VACUUM")
    finally:
        output.close()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        raise FileExistsError(f"refusing to overwrite existing output: {args.output}")
    temp.rename(args.output)
    print(f"built {args.output} with {len(all_rows)} records")
    return 0


def audit_output(output: sqlite3.Connection, master_dir: Path) -> None:
    total = output.execute("SELECT COUNT(*) FROM songs").fetchone()[0]
    if total != sum(EXPECTED_COUNTS.values()):
        raise ValueError(f"output count mismatch: {total}")
    for source, expected in EXPECTED_COUNTS.items():
        actual = output.execute("SELECT COUNT(*) FROM songs WHERE source=?", (source,)).fetchone()[0]
        if actual != expected:
            raise ValueError(f"{source} output count mismatch: {actual}")
    empty = output.execute(
        "SELECT COUNT(*) FROM songs WHERE trim(presentation_content)=''"
    ).fetchone()[0]
    if empty:
        raise ValueError(f"{empty} records have empty presentation_content")

    # Identity and normalized content must remain byte-for-byte equivalent to masters.
    for source, filename in SOURCE_FILES.items():
        master = sqlite3.connect(master_dir / filename)
        master_rows = dict(master.execute("SELECT source_url,content FROM songs"))
        master.close()
        output_rows = dict(output.execute(
            "SELECT source_url,content FROM songs WHERE source=?", (source,)
        ))
        if master_rows != output_rows:
            raise ValueError(f"{source} source_url/content preservation audit failed")


def fixture_test(args: argparse.Namespace) -> int:
    fixtures = Path(__file__).with_name("fixtures")
    cases = {
        "UG": fixtures / "ultimate_guitar.html",
        "GT": fixtures / "guitartuna.html",
        "ChordTela": fixtures / "chordtela.html",
    }
    for source, path in cases.items():
        if not path.is_file():
            raise FileNotFoundError(path)
        result = EXTRACTORS[source](path.read_text(encoding="utf-8", errors="replace"))
        if not result.strip():
            raise AssertionError(f"{source}: empty fixture extraction")
        if source == "UG":
            if "[ch]" in result.lower() or "[/tab]" in result.lower():
                raise AssertionError("UG semantic markup leaked into presentation")
            if "[Verse 1]" not in result or "G        Em" not in result:
                raise AssertionError("UG fixture lost section or visible bare chords")
        if source == "GT":
            if "[Intro 1]" not in result or "B   B" not in result or "Look at the stars" not in result:
                raise AssertionError("GT fixture lost section, bare chords, or lyrics")
            if "[B]" in result:
                raise AssertionError("GT fixture invented chord brackets")
        if source == "ChordTela":
            if "Intro: Am F Dm E (2x)" not in result or "Am   F" not in result:
                raise AssertionError("ChordTela fixture lost visible bare chords")
            if "[Am]" in result:
                raise AssertionError("ChordTela fixture invented chord brackets")
        print(f"{source}: {len(result.splitlines())} lines")
    return 0


def parser() -> argparse.ArgumentParser:
    value = argparse.ArgumentParser(description=__doc__)
    value.add_argument("--master-dir", type=Path, default=DEFAULT_MASTER_DIR)
    value.add_argument("--cache", type=Path, default=DEFAULT_CACHE)
    sub = value.add_subparsers(dest="command", required=True)
    acquire_cmd = sub.add_parser("acquire", help="fetch and cache source-visible presentations")
    acquire_cmd.add_argument(
        "--source", action="append", choices=tuple(SOURCE_FILES),
        help="acquire only this provider (repeatable)",
    )
    acquire_cmd.add_argument("--timeout", type=float, default=30.0)
    acquire_cmd.add_argument("--attempts", type=int, default=3)
    acquire_cmd.add_argument("--workers", type=int, default=8)
    acquire_cmd.add_argument("--limit", type=int, default=0, help="development-only URL limit")
    acquire_cmd.set_defaults(run=acquire)
    build_cmd = sub.add_parser("build", help="build catalog only when every presentation exists")
    build_cmd.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    build_cmd.set_defaults(run=build)
    fixture_cmd = sub.add_parser("test-fixtures", help="run provider extractors on captured pages")
    fixture_cmd.set_defaults(run=fixture_test)
    return value


def main() -> int:
    args = parser().parse_args()
    try:
        return args.run(args)
    except Exception as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
