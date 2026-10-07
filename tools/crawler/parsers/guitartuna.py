from __future__ import annotations

import re
from bs4 import BeautifulSoup

from ..models import ParsedSong
from ..normalizer import clean_text, ensure_chord_spacing, extract_unique_chords, normalize_sheet

TITLE_RE = re.compile(r"^(.*?)\s+Chords\s+by\s+(.*?)(?:\s+-\s+|\s*\|\s*GuitarTuna)", re.IGNORECASE)
VERSION_SUFFIX_RE = re.compile(r"\s*\(\s*(?:official|original|easy)\s+version\s*\)", re.IGNORECASE)


def parse_gt_html(html_text: str, url: str, artist_hint: str = "", title_hint: str = "") -> ParsedSong | None:
    """
    Parse a GuitarTuna HTML page using robust DOM extraction.
    Replaces .sr-only chord position markers inside lyric lines with inline [Chord].
    """
    if not html_text:
        return None

    soup = BeautifulSoup(html_text, "html.parser")

    # 1. Title and Artist extraction
    title = title_hint
    artist = artist_hint

    title_elem = soup.find("title")
    if title_elem and (not title or not artist):
        raw_title = clean_text(title_elem.get_text())
        m = TITLE_RE.search(raw_title)
        if m:
            if not title:
                title = m.group(1).strip()
            if not artist:
                artist = m.group(2).strip()

    # Clean artist version leaks like "Queen (Official Version)"
    if artist:
        artist = VERSION_SUFFIX_RE.sub("", artist).strip()

    if not title:
        title = "Unknown Title"
    if not artist:
        artist = "Unknown Artist"

    # Version name inference from URL or title
    version_name = "Chords"
    url_lower = url.lower()
    if "easy" in url_lower:
        version_name = "Easy Chords"
    elif "official" in url_lower or "original" in url_lower:
        version_name = "Original Version"

    # 2. DOM Sheet extraction
    elements = soup.select(".part_type, .line")
    if not elements:
        return None

    out: list[str] = []

    for el in elements:
        classes = el.get("class", [])
        if "part_type" in classes:
            sec_name = el.get_text(strip=True)
            if sec_name:
                if out and out[-1] != "":
                    out.append("")
                # Ensure section is bracketed e.g. [Intro] or [Chorus 1]
                clean_sec = sec_name.strip("[]")
                out.append(f"[{clean_sec}]")

        elif "line" in classes:
            # Extract chords in this line
            line_chords = []
            for c_el in el.select("[data-chord]"):
                c = c_el.get_text(strip=True)
                if c:
                    line_chords.append(c)

            # Find lyric container div (excludes beat_info)
            lyric_div = None
            for d in el.find_all("div", recursive=False):
                d_classes = d.get("class", [])
                if not d_classes or "beat_info" not in d_classes:
                    lyric_div = d
                    break

            sr_spans = lyric_div.select(".sr-only") if lyric_div else []

            # Check if there is actual lyric text without the sr-only tags
            text_without_sr = ""
            if lyric_div:
                cloned = BeautifulSoup(str(lyric_div), "html.parser")
                for s in cloned.select(".sr-only"):
                    s.decompose()
                text_without_sr = cloned.get_text(strip=True)

            if not text_without_sr and line_chords:
                # Instrumental / Intro line without lyrics
                chord_line = "   ".join(f"[{c}]" for c in line_chords)
                out.append(chord_line)
            elif lyric_div and text_without_sr:
                # Replace each sr-only span with [Chord] directly into DOM tree
                for idx, span in enumerate(sr_spans):
                    c = line_chords[idx] if idx < len(line_chords) else ""
                    span.replace_with(f"[{c}]" if c else "")

                raw_text = lyric_div.get_text()
                spaced_line = ensure_chord_spacing(raw_text.strip())
                out.append(spaced_line)

    sheet_content = normalize_sheet("\n".join(out))
    if not sheet_content:
        return None

    unique_chords = extract_unique_chords(sheet_content)
    if not unique_chords:
        return None

    return ParsedSong(
        source="GT",
        source_url=url,
        title=title,
        artist=artist,
        version_name=version_name,
        difficulty="Beginner" if version_name == "Easy Chords" else "Intermediate",
        content=sheet_content,
        chords=unique_chords,
        region="BARAT",
    )
