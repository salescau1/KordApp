import sqlite3, csv
from pathlib import Path

db_dir = Path('C:/KordApp-AGY/tools/catalog/db_batches')
csv_dir = Path('C:/KordApp-AGY/tools/catalog/batches')

for i in range(1, 16):
    csv_file = csv_dir / f'batch_{i:02d}.csv'
    db_file = db_dir / f'batch_{i:02d}.db'
    if not csv_file.exists(): 
        continue
    with open(csv_file, encoding='utf-8') as f:
        targets = [r['artist'].strip() for r in csv.DictReader(f) if r.get('artist')]
    if not db_file.exists():
        print(f"Batch {i:02d}: 0/{len(targets)} (0%) - Belum dimulai")
        continue
    con = sqlite3.connect(str(db_file))
    cur = con.cursor()
    song_count = cur.execute('SELECT count(*) FROM songs').fetchone()[0]
    db_artists_lower = {row[0].strip().lower() for row in cur.execute('SELECT DISTINCT artist FROM songs').fetchall() if row[0]}
    has_bp = cur.execute("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='batch_progress'").fetchone()[0]
    bp_artists = set()
    if has_bp:
        bp_artists = {row[0].strip().lower() for row in cur.execute("SELECT artist FROM batch_progress").fetchall() if row[0]}
    
    completed_targets = []
    for t in targets:
        tl = t.lower()
        if tl in bp_artists or any(tl in da or da in tl for da in db_artists_lower):
            completed_targets.append(t)
    
    pct = round(len(completed_targets)/len(targets)*100)
    status_label = "SUKSES SELESAI" if len(completed_targets) == len(targets) else "SEBAGIAN"
    print(f"Batch {i:02d}: {len(completed_targets)}/{len(targets)} ({pct}%) [{status_label}] - {song_count} lagu")
    con.close()
