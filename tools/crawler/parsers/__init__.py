from __future__ import annotations

from ..models import ParsedSong
from .guitartuna import parse_gt_html
from .ultimate_guitar import parse_ug_html
from .chordtela import parse_chordtela_html


def parse_song(
    source: str,
    html_text: str,
    url: str,
    artist_hint: str = "",
    title_hint: str = ""
) -> ParsedSong | None:
    """
    Unified entry point for song HTML parsing.
    source: 'GT' | 'UG' | 'ChordTela'
    """
    src = source.upper()
    if src == "GT" or "guitartuna" in src.lower():
        return parse_gt_html(html_text, url, artist_hint=artist_hint, title_hint=title_hint)
    elif src == "UG" or "ultimate" in src.lower():
        return parse_ug_html(html_text, url, artist_hint=artist_hint, title_hint=title_hint)
    elif src in ("CHORDTELA", "CT"):
        return parse_chordtela_html(html_text, url, artist_hint=artist_hint, title_hint=title_hint)
    else:
        raise ValueError(f"Unknown source parser: {source}")


__all__ = [
    "parse_song",
    "parse_gt_html",
    "parse_ug_html",
    "parse_chordtela_html",
]
