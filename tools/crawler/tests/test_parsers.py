import os
import sys

# Ensure tools is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))

from crawler import fetch_html, parse_song


def test_gt_parsing():
    print("\n" + "=" * 60)
    print("TESTING GUITARTUNA (GT) PARSER")
    print("=" * 60)
    url = "https://guitartuna.com/chords/we-are-the-champions-queen-easy-guitar-chords-61321003372a58d4696d0489"
    print(f"Fetching: {url}")
    html = fetch_html(url)
    assert html is not None, "Failed to fetch GuitarTuna HTML"

    song = parse_song("GT", html, url, artist_hint="Queen", title_hint="We Are The Champions")
    assert song is not None, "GT parser returned None"
    assert "Queen" in song.artist, f"Unexpected artist: {song.artist}"
    assert "We Are The Champions" in song.title, f"Unexpected title: {song.title}"
    assert len(song.chords) > 0, "No chords extracted"
    
    # Critical GT verification: Lirik must not be broken into separate syllable lines ("No es", "Look", etc.)
    lines = song.content.splitlines()
    assert not any(l.strip() == "No es" for l in lines), "Corrupted GT: 'No es' token detected as separate line!"
    assert not any(l.strip() == "Look" for l in lines), "Corrupted GT: 'Look' token detected as separate line!"

    print(f"GT OK! Title: {song.title} | Artist: {song.artist} | Chords: {len(song.chords)} ({', '.join(song.chords[:8])})")
    print("Preview first 15 lines:\n")
    print("\n".join(lines[:15]))
    return song


def test_ug_parsing():
    print("\n" + "=" * 60)
    print("TESTING ULTIMATE GUITAR (UG) PARSER")
    print("=" * 60)
    url = "https://tabs.ultimate-guitar.com/tab/queen/bohemian-rhapsody-chords-1716013"
    print(f"Fetching: {url}")
    html = fetch_html(url)
    assert html is not None, "Failed to fetch Ultimate Guitar HTML"

    song = parse_song("UG", html, url, artist_hint="Queen", title_hint="Bohemian Rhapsody")
    assert song is not None, "UG parser returned None"
    assert "Queen" in song.artist, f"Unexpected artist: {song.artist}"
    assert "Bohemian Rhapsody" in song.title, f"Unexpected title: {song.title}"
    assert len(song.chords) > 0, "No chords extracted"

    # Critical UG verification: No [tab] or [/tab] tags
    assert "[tab]" not in song.content.lower(), "Corrupted UG: [tab] tag detected in content!"
    assert "[/tab]" not in song.content.lower(), "Corrupted UG: [/tab] tag detected in content!"
    assert "tab" not in [c.lower() for c in song.chords], "Corrupted UG: 'tab' extracted as chord!"

    print(f"UG OK! Title: {song.title} | Artist: {song.artist} | Chords: {len(song.chords)} ({', '.join(song.chords[:8])})")
    print("Preview first 20 lines:\n")
    print("\n".join(song.content.splitlines()[:20]))
    return song


def test_chordtela_parsing():
    print("\n" + "=" * 60)
    print("TESTING CHORDTELA (INDO) PARSER")
    print("=" * 60)
    url = "https://www.chordtela.com/2013/08/dewa-19-kangen.html"
    print(f"Fetching: {url}")
    html = fetch_html(url)
    if not html:
        print("[SKIP] ChordTela might be unreachable or rate limited")
        return None

    song = parse_song("ChordTela", html, url)
    if song:
        print(f"ChordTela OK! Title: {song.title} | Artist: {song.artist} | Chords: {len(song.chords)}")
        print("Preview first 15 lines:\n")
        print("\n".join(song.content.splitlines()[:15]))
    return song


if __name__ == "__main__":
    try:
        test_gt_parsing()
        test_ug_parsing()
        print("\n" + "=" * 60)
        print("ALL GT & UG PARSER TESTS PASSED PERFECTLY!")
        print("=" * 60)
    except Exception as exc:
        print(f"\nTEST FAILED: {exc}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
