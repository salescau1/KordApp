import os, csv, sqlite3, sys
sys.stdout.reconfigure(encoding='utf-8')

batches_dir = r'C:\KordApp-AGY\tools\catalog\batches'
dbs_dir = r'C:\KordApp-AGY\tools\catalog\db_batches'

print("=== STATUS AUDIT 15 BATCH KORDAPP ===")
total_all_songs = 0
completed_batches = []
partial_batches = []
unstarted_batches = []

for i in range(1, 17):
    csv_f = os.path.join(batches_dir, f'batch_{i:02d}.csv')
    db_f = os.path.join(dbs_dir, f'batch_{i:02d}.db')
    
    target_artists = []
    if os.path.exists(csv_f):
        with open(csv_f, 'r', encoding='utf-8') as f:
            target_artists = [r['artist'].strip().lower() for r in csv.DictReader(f) if r.get('artist')]
    
    song_cnt = 0
    scraped_artists_set = set()
    sources = []
    if os.path.exists(db_f):
        try:
            con = sqlite3.connect(db_f)
            song_cnt = con.execute('SELECT count(*) FROM songs').fetchone()[0]
            for r in con.execute('SELECT DISTINCT lower(trim(artist)) FROM songs'):
                if r[0]:
                    scraped_artists_set.add(r[0])
            sources = con.execute('SELECT source, count(*) FROM songs GROUP BY source').fetchall()
            con.close()
        except:
            pass
            
    total_all_songs += song_cnt
    total_target = len(target_artists)
    
    # Check how many of the target artists for this batch have songs in the DB
    matched_targets = sum(1 for t in target_artists if t in scraped_artists_set)
    
    src_summary = ", ".join(f"{s[0]}: {s[1]}" for s in sources)
    
    if matched_targets >= total_target and total_target > 0:
        status_label = "[SUKSES SELESAI]"
        completed_batches.append(i)
    elif song_cnt > 0:
        status_label = f"[SEBAGIAN ({matched_targets}/{total_target})]"
        partial_batches.append((i, matched_targets, total_target, song_cnt))
    else:
        status_label = "[BELUM DIMULAI]"
        unstarted_batches.append(i)
        
    print(f"Batch {i:02d}: {status_label:<25} | {song_cnt:>5} lagu | {matched_targets:>2}/{total_target:>2} artis target | {src_summary}")

print(f"\nTOTAL LAGU DI SELURUH BATCH: {total_all_songs} lagu")
print(f"Batch Selesai: {completed_batches}")
print(f"Batch Sebagian: {[p[0] for p in partial_batches]}")
print(f"Batch Belum Dimulai: {unstarted_batches}")
