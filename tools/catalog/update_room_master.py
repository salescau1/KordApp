import sqlite3

conn = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = conn.cursor()

c.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
c.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'a69e31afaf7d1570cec628f49411725a')")
c.execute("PRAGMA user_version = 6")
conn.commit()

hash_val = c.execute("SELECT identity_hash FROM room_master_table WHERE id = 42").fetchone()[0]
user_ver = c.execute("PRAGMA user_version").fetchone()[0]
print(f"Room Master Table identity_hash: {hash_val}")
print(f"Database user_version: {user_ver}")

conn.close()
