from __future__ import annotations

import hashlib
import json
import re
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any


def slugify(text: str) -> str:
    """Generate URL-safe slug from text (e.g. 'Someone Like You' -> 'someone-like-you')."""
    text = (text or "").lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_-]+", "-", text)
    return text.strip("-")


@dataclass
class ParsedSong:
    source: str                     # "GT", "UG", "ChordTela"
    source_url: str
    title: str
    artist: str
    title_slug: str = ""
    artist_slug: str = ""
    version_name: str | None = None
    rating: float | None = None
    votes: int | None = None
    key_base: str | None = None
    difficulty: str | None = None
    category: str | None = None      # "BARAT", "INDO", "JAPAN", "KOREA", etc.
    region: str = "BARAT"
    chords: list[str] = field(default_factory=list)
    content: str = ""
    chord_count: int = 0
    line_count: int = 0
    content_hash: str = ""
    scraped_at: str = ""

    def __post_init__(self):
        if not self.title_slug:
            self.title_slug = slugify(self.title)
        if not self.artist_slug:
            self.artist_slug = slugify(self.artist)
        if not self.scraped_at:
            self.scraped_at = datetime.now(timezone.utc).isoformat()
        if self.content:
            # Normalize line count and hash
            lines = [l for l in self.content.splitlines() if l.strip()]
            self.line_count = len(lines)
            self.chord_count = len(self.chords)
            self.content_hash = hashlib.sha256(self.content.encode("utf-8")).hexdigest()

    def to_db_row(self) -> dict[str, Any]:
        """Convert to dict matching SQLite `songs` table schema."""
        return {
            "source": self.source,
            "source_url": self.source_url,
            "title": self.title,
            "artist": self.artist,
            "title_slug": self.title_slug,
            "artist_slug": self.artist_slug,
            "version_name": self.version_name,
            "rating": self.rating,
            "votes": self.votes,
            "key_base": self.key_base,
            "difficulty": self.difficulty,
            "category": self.category,
            "region": self.region,
            "chords": json.dumps(self.chords),
            "chord_count": self.chord_count,
            "line_count": self.line_count,
            "content": self.content,
            "content_hash": self.content_hash,
            "scraped_at": self.scraped_at,
        }
