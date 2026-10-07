from __future__ import annotations

import html
import re
from bs4 import BeautifulSoup, Tag

from ..models import ParsedSong
from ..normalizer import clean_text, ensure_chord_spacing, extract_unique_chords, normalize_sheet, CHORD_STANDALONE_RE

BAD_RE = re.compile(
    r"(?:datalayer|window\.|javascript|googletag|adsbygoogle|google-analytics|doubleclick|<script)",
    re.I,
)


def _extract_text_preserving_breaks(node: Tag) -> str:
    for bad in node.select(".tbi-tooltip span, script, style, noscript"):
        bad.decompose()

    for br in node.find_all("br"):
        br.replace_with("\n")

    text = node.get_text("", strip=False)
    text = html.unescape(text).replace("\xa0", " ")
    text = re.sub(r"[ \t]+\n", "\n", text)
    text = re.sub(r"\n[ \t]+", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def parse_chordtela_html(html_text: str, url: str, artist_hint: str = "", title_hint: str = "") -> ParsedSong | None:
    """
    Parse a ChordTela HTML page.
    Locates .telabox, preserves linebreaks, converts unbracketed chords to [Chord].
    """
    if not html_text:
        return None

    soup = BeautifulSoup(html_text, "html.parser")
    box = soup.select_one(".telabox") or soup.select_one("div.telabox")
    if box is None:
        return None

    raw = _extract_text_preserving_breaks(box)
    if not raw or BAD_RE.search(raw):
        return None

    # Title & Artist detection
    title = title_hint
    artist = artist_hint

    if not title or not artist:
        raw_title = ""
        h1 = soup.select_one("h1")
        if h1:
            raw_title = h1.get_text(" ", strip=True)
        elif soup.title:
            raw_title = soup.title.get_text(" ", strip=True)
            raw_title = re.sub(r"\s*[-|].*$", "", raw_title).strip()

        cleaned = re.sub(r"^(?:Kunci Gitar|Chord Dasar|Chord Gitar|Lirik Lagu)\s+", "", raw_title, flags=re.I).strip()
        cleaned = re.sub(r"\s+(?:Chord Dasar|Kunci Dasar|Chord Gitar|Kunci Gitar)$", "", cleaned, flags=re.I).strip()

        if " - " in cleaned:
            parts = [x.strip() for x in cleaned.split(" - ", 1)]
            if not artist:
                artist = parts[0]
            if not title:
                title = parts[1]
        elif not title:
            title = cleaned

    if not title:
        title = "Unknown Title"
    if not artist:
        artist = "Unknown Artist"

    # Convert standalone chords to [Chord]
    # In ChordTela, chords are plain text above lyric lines (or inside bar lines)
    content_bracketed = CHORD_STANDALONE_RE.sub(r"[\1]", raw)
    sheet_content = normalize_sheet(content_bracketed)

    unique_chords = extract_unique_chords(sheet_content)
    if len(unique_chords) < 2:
        return None

    return ParsedSong(
        source="ChordTela",
        source_url=url,
        title=title,
        artist=artist,
        version_name="Original",
        difficulty="Intermediate",
        content=sheet_content,
        chords=unique_chords,
        category="INDO",
        region="INDO",
    )
