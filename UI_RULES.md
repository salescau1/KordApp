# ChordApp UI Rules

## Visual Contract
Accepted mockups are visual references. Specs define data and behavior. Illustrative mockup text is not automatically real data.

## Global Colors
Background `#0E1117`
Card `#161B22`
Raised `#1A202C`
Border `#242C3D`
Gold `#E5A93C`
Muted gold `#2A210C`
Text `#F5F8FF`
Muted `#8E9AA8`

Source badges:
UG `#F59E0B` / `#2A210C`
GuitarTuna `#22C55E` / `#0D281E`
ChordTela `#38BDF8` / `#0C2340`

Favorite active heart `#FF4D6D`.

## Global
- Offline status must be truthful.
- Ads collapse to zero when unavailable/offline.
- Unsupported metadata is omitted.
- No fake duration/BPM/album/tags/review counts/Songsterr.
- A-Z uses `COLLATE NOCASE`.
- Follow five-tab navigation.

## Home
Fixed header; independent Top 20 scroll.
Native ad after rank #3, collapsible.
Unavailable chord content shows the specified unavailable state and disables play.
Online chart/platform behavior must not be represented as offline catalog data.

## Search
Only specified title/artist information in results. No source badge, key, BPM, genre, duration, rating, or unsupported metadata.

## Artist Directory
Aggregate by artist slug. Do not show unsupported album/duration/follow/Songsterr data.

## Song Versions
Use real source/version records. Rating/votes only where votes > 0. Conditional metadata only.

## Chord Player
Chord columns require monospace alignment where column positioning is used.
Transpose changes chord roots without moving lyric alignment.
Preserve transpose and scroll state when switching versions.
Page Flip stops auto-scroll.
Keep screen awake as specified.
Bluetooth pedal status only when actual support/connection exists.

## Songbook / Setlist
Use specified vertical list/card layouts.
No fabricated durations.
User notes are user-entered.
Persist reorder/delete.

## Favorites
White outline heart when not saved; solid `#FF4D6D` when saved.
Saved transpose/capo is applied when opening Player.
