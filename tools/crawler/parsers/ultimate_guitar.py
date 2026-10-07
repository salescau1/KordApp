from __future__ import annotations

import html
import json
import re

from ..models import ParsedSong
from ..normalizer import clean_text, ensure_chord_spacing, extract_unique_chords, normalize_sheet

JS_STORE_RE = re.compile(r'class="js-store"\s+data-content="([^"]+)"')
CHORD_TAG_RE = re.compile(r"\[ch\](.*?)\[/ch\]", re.IGNORECASE)
TAB_TAG_RE = re.compile(r"\[/?tab\]", re.IGNORECASE)


def parse_ug_html(html_text: str, url: str, artist_hint: str = "", title_hint: str = "") -> ParsedSong | None:
    """
    Parse an Ultimate Guitar HTML page.
    Extracts the wiki_tab content from js-store, strips [tab] BBCode tags,
    and converts [ch]Chord[/ch] to [Chord].
    """
    if not html_text:
        return None

    match = JS_STORE_RE.search(html_text)
    if not match:
        return None

    try:
        raw_json = html.unescape(match.group(1))
        data = json.loads(raw_json)
    except (json.JSONDecodeError, ValueError):
        return None

    page_data = data.get("store", {}).get("page", {}).get("data", {})
    tab_view = page_data.get("tab_view", {})
    raw_content = tab_view.get("wiki_tab", {}).get("content", "")
    if not raw_content:
        return None

    tab = page_data.get("tab", {})
    title = (tab.get("song_name") or title_hint or "").strip()
    artist = (tab.get("artist_name") or artist_hint or "").strip()
    if not title or not artist:
        return None

    version_num = tab.get("version", 1)
    version_name = f"Ver {version_num}"
    rating = tab.get("rating")
    votes = tab.get("votes")
    key_base = tab.get("tonality_name")
    difficulty = tab.get("difficulty")

    # Clean BBCode tags:
    # 1. Strip [tab] and [/tab]
    cleaned = TAB_TAG_RE.sub("", raw_content)
    # 2. Convert [ch]Chord[/ch] -> [Chord]
    cleaned = CHORD_TAG_RE.sub(r"[\1]", cleaned)

    sheet_content = normalize_sheet(cleaned)
    if not sheet_content:
        return None

    unique_chords = extract_unique_chords(sheet_content)
    if not unique_chords:
        return None

    return ParsedSong(
        source="UG",
        source_url=url,
        title=title,
        artist=artist,
        version_name=version_name,
        rating=float(rating) if rating is not None else None,
        votes=int(votes) if votes is not None else None,
        key_base=key_base,
        difficulty=difficulty,
        content=sheet_content,
        chords=unique_chords,
        region="BARAT",
    )
