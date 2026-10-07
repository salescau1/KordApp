# ChordApp Architecture

## Goal
Offline-first, deterministic, testable, with catalog content separated from mutable user data.

## Layers
UI (Jetpack Compose)
→ ViewModel / UI state
→ Repository / use-case layer
→ Catalog + Userdata
→ Android platform services

## Catalog
Sources:
- Ultimate Guitar (UG)
- GuitarTuna (GT)
- ChordTela

Every source/version record is retained. Cross-source matching is for grouping/version comparison, not deletion.

Runtime catalog is read-only.

## User Data
`userdata.db` stores mutable user state such as favorites, songbooks, setlists, transpose/capo preferences, notes, and approved settings.

## Identity
Never use `songs.id` as persistent user-data identity.

Version resolution:
1. exact `source_url`
2. `(source, content_hash)`
3. another version with `(artist_slug, title_slug)`
4. same song from another source, with user notification

Song-level grouping uses `(artist_slug, title_slug)`.

## Source Metadata
The audited databases differ:
- UG: rating, votes, key, difficulty, version.
- GT: version, difficulty; no rating/votes/key fields.
- ChordTela: category column exists in schema, but current audited rows have NULL category.
- UG regions: `en`, `jp`; GT: `en`; ChordTela: `id`, `en`.

Never fabricate missing metadata.

## Catalog Packaging
A build step may create a packaged read-only catalog representation, but it must preserve all source/version records. Original source databases remain master inputs and must not be modified.

Exact packaging format is an implementation decision to be finalized by the catalog contract.

## Navigation
Five persistent destinations:
Home | Explore | Setlists | Favorites | Settings

Detail/player screens may hide bottom navigation according to their specs.

## Offline
Core browsing, search, artist browsing, versions, player, favorites, songbooks and setlists use local data. Do not introduce network dependencies unless explicitly specified.

## Testing
Keep catalog queries, grouping, and user-data resolution independently testable from Compose UI.
