import sqlite3

gt_con = sqlite3.connect(r'C:\ChordApp\guitartuna_crawler\guitartuna_merged.db')
tables = [r[0] for r in gt_con.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()]
print("GT merged tables:", tables)

ug_con = sqlite3.connect(r'C:\ChordApp\ug_crawler\data\ug\ug_catalog.db')
tables_ug = [r[0] for r in ug_con.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()]
print("UG catalog tables:", tables_ug)

# Check crawl_queue in GT
if 'crawl_queue' in tables:
    cols = [c[1] for c in gt_con.execute("PRAGMA table_info(crawl_queue)").fetchall()]
    print("crawl_queue cols:", cols)
    print("crawl_queue count:", gt_con.execute("SELECT count(*) FROM crawl_queue").fetchone()[0])
