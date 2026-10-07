# ChordApp Data Rules

## Audited Catalog
| Source | Rows | Unique artist+title | Unique artists |
|---|---:|---:|---:|
| UG | 8,121 | 6,282 | 1,950 |
| GT | 19,753 | 19,749 | 4,666 |
| ChordTela | 41,973 | 41,973 | 15,663 |

Total source rows: 69,847.

Distinct song overlap:
- UG ↔ GT: 1,970
- UG ↔ ChordTela: 425
- GT ↔ ChordTela: 214

These overlaps are intentional.

## Source Schemas
UG songs:
`id, source_url, title, artist, title_slug, artist_slug, version_name, rating, votes, key_base, difficulty, region, chords, chord_count, line_count, content, scraped_at`

GT songs:
`id, source_url, title, artist, title_slug, artist_slug, version_name, difficulty, region, chords, chord_count, line_count, content, scraped_at`

ChordTela songs:
`id, source_url, title, artist, title_slug, artist_slug, category, region, chords, chord_count, line_count, content, scraped_at`

All three also contain `crawl_queue` and `songs`, with artist/title slug indexes.

## Rules
- Preserve every row and source identity.
- Preserve source-specific versions.
- Never use `songs.id` as cross-source identity.
- Group songs by `artist_slug + title_slug` when song-level grouping is needed.
- Prefer `source_url` for version references.
- Missing metadata remains NULL/absent.
- Do not invent rating, votes, key, tempo/BPM, duration, genre, album, tags, reviews, or Songsterr data.
- Never log chord content.
- Catalog is read-only.
- Userdata timestamps must follow the user-data specification.

## Userdata Baseline
`songbooks(id, name, created_at, updated_at)`

`setlists(id, songbook_id NULL, name, note, created_at, updated_at)`

`setlist_items(id, setlist_id, position, source_url, artist_slug, title_slug, source, content_hash, transpose, capo, note)`

`favorite_songs(id, added_at, source_url, artist_slug, title_slug, source, content_hash, transpose, capo)`

`favorite_artists(id, artist_slug, added_at)`

Do not expand these schemas without an accepted requirement.

## Resolution Cascade
1. Exact `source_url`
2. `(source, content_hash)`
3. Another version of same song
4. Same song from another source, notifying the user

Preserve user preferences during substitution.

## Explore
Current ChordTela `category` is NULL for all 41,973 audited rows. Do not derive categories without approved additional data.

Accepted chord-count tiers:
- 4 chords: 1,471
- 5–6 chords: 4,691
- 7+ chords: 17,807

These tiers are based on distinct chord count, not difficulty.
