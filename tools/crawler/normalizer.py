from __future__ import annotations

import html
import re

# Regex for chords inside brackets: e.g. [C], [Am7], [F#/G#], [Bbmaj7], [Cdim], [Dsus4]
CHORD_BRACKET_RE = re.compile(
    r"\[([A-G][b#]?(?:m|maj7|maj9|maj|min|7|9|11|13|sus2|sus4|sus|dim7|dim|aug|add9|m7b5|m7|mM7)?(?:/[A-G][b#]?)?)\]",
    re.IGNORECASE
)

# Standalone chord token in chord-only lines
CHORD_STANDALONE_RE = re.compile(
    r"(?<![A-Za-z0-9])([A-G][b#]?(?:m|maj7|maj9|maj|min|7|9|11|13|sus2|sus4|sus|dim7|dim|aug|add9|m7b5|m7|mM7)?(?:/[A-G][b#]?)?)(?![A-Za-z0-9])"
)

UNICODE_REPLACEMENTS = [
    ("\u2018", "'"),  # Left single quotation mark
    ("\u2019", "'"),  # Right single quotation mark
    ("\u201c", '"'),  # Left double quotation mark
    ("\u201d", '"'),  # Right double quotation mark
    ("\u2013", "-"),  # En dash
    ("\u2014", "--"), # Em dash
    ("\u00a0", " "),  # Non-breaking space
    ("\r\n", "\n"),   # CRLF
    ("\r", "\n"),     # CR
]


def clean_text(text: str) -> str:
    """Normalize typography, unescape HTML entities, and standardize line breaks."""
    if not text:
        return ""
    text = html.unescape(text)
    for src, target in UNICODE_REPLACEMENTS:
        text = text.replace(src, target)
    return text


def ensure_chord_spacing(line: str) -> str:
    """
    Ensure chords touching with ZERO spaces are separated.
    Example: '[C][Am]' -> '[C] [Am]'
    Never collapses existing multiple spaces (which are indentation/alignment columns).
    """
    return re.sub(r"\]\[", "] [", line)


def extract_unique_chords(content: str) -> list[str]:
    """Find all unique chord names enclosed in [brackets] in document order."""
    seen: set[str] = set()
    result: list[str] = []
    for match in CHORD_BRACKET_RE.finditer(content):
        c = match.group(1).strip()
        # Clean canonical capitalization: e.g. 'c' -> 'C', 'am' -> 'Am'
        if c:
            canonical = c[0].upper() + c[1:]
            if canonical not in seen:
                seen.add(canonical)
                result.append(canonical)
    return result


def normalize_sheet(content: str) -> str:
    """
    Normalize sheet lines:
    - Strips trailing whitespace per line.
    - Ensures maximum of 1 consecutive blank line.
    - Standardizes chord bracket spacing.
    """
    content = clean_text(content)
    raw_lines = content.split("\n")
    processed: list[str] = []
    last_blank = False

    for line in raw_lines:
        line = line.rstrip()
        is_blank = (len(line.strip()) == 0)

        if is_blank:
            if not last_blank and len(processed) > 0:
                processed.append("")
                last_blank = True
        else:
            line = ensure_chord_spacing(line)
            processed.append(line)
            last_blank = False

    return "\n".join(processed).strip()
