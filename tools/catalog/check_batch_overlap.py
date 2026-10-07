import csv
import glob

all_artists = set()
for f in sorted(glob.glob("tools/catalog/batches/batch_*.csv")):
    with open(f, encoding="utf-8") as fp:
        for r in csv.DictReader(fp):
            all_artists.add(r['artist'].strip().lower())

print(f"Total artists across batch 1-17: {len(all_artists)}")

test_names = [
    'akeboshi', 'sherina', 'sherina munaf', 'acha septriasa', 'irwansyah', 
    'the panasdalam bank', 'anda perdana', 'ikimonogakari', 'koji wada',
    'field of view', 'linked horizon', 'yui', 'tk from ling tosite sigure',
    'ai higuchi', 'hironobu kageyama', 'do as infinity', 'orange range',
    'motohiro hata', '7!!', 'chanyeol', 'punch', 'crush', 'ailee',
    'davichi', 'gummy', 'lee juck', 'gaho', 'idina menzel', 'keala settle',
    'loren allred', 'naomi scott', 'ronan keating', 'elvis costello', 'wiz khalifa',
    'chad kroeger', 'peabo bryson', 'kenny loggins', 'aerosmith'
]

for t in test_names:
    print(f"{t}: in existing batches = {t in all_artists}")
