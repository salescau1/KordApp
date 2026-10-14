import sqlite3
from pathlib import Path

ROOM_V6_HASH = 'a69e31afaf7d1570cec628f49411725a'

dbs_to_upgrade = [
    Path("app/src/test/resources/catalog_test.db"),
    Path(".data_backups/catalog_full.db"),
    Path("tools/catalog/full_merged_catalog.db")
]

for db_path in dbs_to_upgrade:
    if not db_path.exists():
        print(f"Skipping {db_path} (does not exist)")
        continue
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    
    # Check if tier column exists
    cols = [col[1] for col in c.execute("PRAGMA table_info(songs)").fetchall()]
    if "tier" not in cols:
        print(f"Adding 'tier' to {db_path}...")
        c.execute("ALTER TABLE songs ADD COLUMN tier TEXT NOT NULL DEFAULT 'A'")
    else:
        print(f"'tier' already in {db_path}")
        
    c.execute("CREATE INDEX IF NOT EXISTS idx_tier ON songs (tier)")
    
    # Update room_master_table
    c.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
    c.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)", (ROOM_V6_HASH,))
    c.execute("PRAGMA user_version = 6")
    conn.commit()
    
    ver = c.execute("PRAGMA user_version").fetchone()[0]
    h = c.execute("SELECT identity_hash FROM room_master_table WHERE id = 42").fetchone()[0]
    cnt = c.execute("SELECT count(*) FROM songs").fetchone()[0]
    print(f"Updated {db_path}: version={ver}, hash={h}, songs={cnt}")
    conn.close()

print("All DBs upgraded successfully!")
