import sqlite3
import re
from pathlib import Path

DB_PATH = Path(r"C:\KordApp-AGY\tools\catalog\full_merged_catalog.db")

KNOWN_BANDS = {
    "SIMON & GARFUNKEL", "SIMON AND GARFUNKEL",
    "BROOKS & DUNN", "BROOKS AND DUNN",
    "HALL & OATES", "DARYL HALL & JOHN OATES", "DARYL HALL AND JOHN OATES",
    "EARTH, WIND & FIRE", "EARTH, WIND AND FIRE",
    "KOOL & THE GANG", "KOOL AND THE GANG",
    "TOM PETTY AND THE HEARTBREAKERS", "TOM PETTY & THE HEARTBREAKERS",
    "BOB MARLEY AND THE WAILERS", "BOB MARLEY & THE WAILERS",
    "HUEY LEWIS AND THE NEWS", "HUEY LEWIS & THE NEWS",
    "JOAN JETT AND THE BLACKHEARTS", "JOAN JETT & THE BLACKHEARTS",
    "KC AND THE SUNSHINE BAND", "KC & THE SUNSHINE BAND",
    "DEREK AND THE DOMINOS",
    "HOOTIE AND THE BLOWFISH",
    "ME FIRST AND THE GIMME GIMMES",
    "THE MAMAS AND THE PAPAS", "THE MAMAS & THE PAPAS",
    "OF MONSTERS AND MEN",
    "FLORENCE + THE MACHINE", "FLORENCE AND THE MACHINE",
    "IRON & WINE", "IRON AND WINE",
    "BONDAN PRAKOSO AND FADE2BLACK", "BONDAN PRAKOSO & FADE2BLACK",
    "CROSBY, STILLS & NASH", "CROSBY, STILLS, NASH & YOUNG",
    "EMERSON, LAKE & PALMER",
    "BLOOD, SWEAT & TEARS",
    "LADY A", "DAN + SHAY",
    "ALICE IN CHAINS", "BOYS LIKE GIRLS",
    "MUMFORD & SONS", "MUMFORD AND SONS",
    "THE BLACK EYED PEAS", "BLACK EYED PEAS",
    "PETER, PAUL AND MARY", "PETER, PAUL & MARY",
    "TONY! TONI! TONÉ!", "TONY! TONI! TONE!",
    "BELL BIV DEVOE"
}

def slugify(text: str) -> str:
    return re.sub(r"[^a-zA-Z0-9]+", "-", text.lower()).strip("-")

def main():
    print(f"Connecting to {DB_PATH}...")
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()

    # Build canonical map from UG/ChordTela
    c.execute("SELECT DISTINCT artist FROM songs WHERE source IN ('UG', 'ChordTela')")
    canonical_map = {}
    for (art,) in c.fetchall():
        if art:
            canonical_map[art.strip().lower()] = art.strip()

    def smart_title_case(name: str) -> str:
        if not name:
            return ""
        name_clean = name.strip()
        low = name_clean.lower()
        if low in canonical_map:
            return canonical_map[low]
        
        words = name_clean.split()
        cased_words = []
        for w in words:
            if w.upper() in {"A$AP", "B.B.", "MGMT", "AC/DC", "U2", "INXS", "REM", "R.E.M.", "UB40", "O.A.R.", "D.R.A.M."}:
                cased_words.append(w.upper())
            elif len(w) > 1 and w[0] == '(' and w[1:].upper() in {"A$AP", "B.B."}:
                cased_words.append(w)
            else:
                cased_words.append(w.capitalize())
        return " ".join(cased_words)

    c.execute("SELECT id, artist, title, artist_slug, title_slug FROM songs WHERE source = 'GT'")
    rows = c.fetchall()
    print(f"Inspecting {len(rows)} GT songs...")

    updated_collabs = 0
    updated_solos = 0

    conn.execute("BEGIN TRANSACTION")

    for id_, artist, title, a_slug, t_slug in rows:
        art_clean = artist.strip()
        tit_clean = title.strip()
        art_upper = art_clean.upper()

        if any(kb == art_upper or f" {kb} " in f" {art_upper} " for kb in KNOWN_BANDS):
            continue

        if "," in art_clean:
            parts = [p.strip() for p in art_clean.split(",") if p.strip()]
            if len(parts) >= 2 and parts[0].lower() == parts[1].lower():
                art_clean = parts[0]

        has_feat_in_title = bool(re.search(r"\b(feat\.?|ft\.?|featuring)\b", tit_clean, re.I))
        lead_art = None
        feat_arts = []

        m_feat = re.search(r"^(.*?)\s+(?:feat\.?|ft\.?|featuring)\s+(.*)$", art_clean, re.I)
        if m_feat:
            lead_art = m_feat.group(1).strip()
            feat_arts = [m_feat.group(2).strip()]
        elif "," in art_clean:
            parts = [p.strip() for p in art_clean.split(",") if p.strip()]
            if len(parts) >= 2:
                lead_art = parts[0]
                feat_arts = parts[1:]
        elif " AND " in art_clean.upper():
            parts = re.split(r"\s+(?:AND|and)\s+", art_clean)
            if len(parts) >= 2:
                lead_art = parts[0].strip()
                feat_arts = [p.strip() for p in parts[1:] if p.strip()]

        if lead_art and feat_arts:
            clean_lead = smart_title_case(lead_art)
            clean_feats = [smart_title_case(f) for f in feat_arts if f]
            feat_str = ", ".join(clean_feats[:-1]) + (" & " if len(clean_feats) > 1 else "") + clean_feats[-1] if clean_feats else ""
            new_title = f"{tit_clean} (feat. {feat_str})" if (not has_feat_in_title and feat_str) else tit_clean
            new_a_slug = slugify(clean_lead)
            new_t_slug = slugify(new_title)

            c.execute("""
                UPDATE songs
                SET artist = ?, title = ?, artist_slug = ?, title_slug = ?
                WHERE id = ?
            """, (clean_lead, new_title, new_a_slug, new_t_slug, id_))
            updated_collabs += 1

        elif art_clean.lower() in canonical_map and canonical_map[art_clean.lower()] != artist:
            new_art = canonical_map[art_clean.lower()]
            c.execute("UPDATE songs SET artist = ? WHERE id = ?", (new_art, id_))
            updated_solos += 1

    conn.commit()
    print(f"Database successfully updated!")
    print(f" - Collabs cleaned: {updated_collabs}")
    print(f" - Solo artists cased: {updated_solos}")

    # Verification queries
    print("\n--- Verifying target songs in full_merged_catalog.db ---")
    c.execute("""
        SELECT id, source, artist, title, artist_slug, title_slug 
        FROM songs 
        WHERE id IN (103941, 103942, 21141, 58402)
    """)
    for r in c.fetchall():
        print(" ", r)

    conn.close()

if __name__ == "__main__":
    main()
