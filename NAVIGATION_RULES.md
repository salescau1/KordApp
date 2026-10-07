# ChordApp Navigation Rules

## Bottom Navigation
Five tabs, consistently:
Home | Explore | Setlists | Favorites | Settings

## Search
Dedicated search view with keyboard autofocus.
Bottom navigation remains available.
Bottom-nav tap closes search and navigates.
Strong artist match → Artist Directory.
Title match → Song Versions; if only one version exists → Player.

## Artist Directory
Detail screen; bottom navigation hidden.

## Song Versions
Compare all actual versions across sources.
If only one version exists, skip to Player.
Filters reflect actual source records.

## Chord Player
Fullscreen/core player; no bottom navigation.
Back/Home controls.
When opened from a setlist, Back returns to tracklist and preserves scroll position.
Back from tracklist returns to collection.

## Songbooks / Setlists
Songbook → contained setlists.
Setlist → Tracklist.
Standalone setlists use `songbook_id = NULL`.
Song addition follows the accepted Chord Player add flow.

## Favorites
Song → Player with saved transpose/capo.
Artist → Artist Directory.
Deletion supports undo.

## Open Decisions
Do not implement behavior dependent on an OPEN decision until confirmed.
