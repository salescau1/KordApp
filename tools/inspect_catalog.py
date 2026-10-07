import sqlite3

db_path = r"C:\KordApp-AGY\tools\catalog\full_merged_catalog.db"
conn = sqlite3.connect(db_path)
cur = conn.cursor()

tables = [r[0] for r in cur.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()]
indexes = [r[0] for r in cur.execute("SELECT name FROM sqlite_master WHERE type='index'").fetchall()]
song_count = cur.execute("SELECT COUNT(*) FROM songs").fetchone()[0]

print("Tables:", tables)
print("Indexes:", indexes)
print("Song count:", song_count)
