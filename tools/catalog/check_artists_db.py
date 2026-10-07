import sqlite3
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

conn = sqlite3.connect("tools/catalog/full_merged_catalog.db")
c = conn.cursor()

artists = [
    'maudy-ayunda', 'christina-perri', 'paramore', 'ronan-keating', 
    'elvis-costello', 'wiz-khalifa', 'charlie-puth', 'dewa-19', 'sheila-on-7',
    'bunga-citra-lestari', 'once-mekel', 'derby-romero', 'peabo-bryson',
    'bryan-adams', 'elton-john', 'whitney-houston', 'shakira', 'linkin-park'
]

for a in artists:
    c.execute("SELECT COUNT(*), artist FROM songs WHERE artist_slug = ? GROUP BY artist", (a,))
    res = c.fetchone()
    if res:
        print(f"[IN DB] {a}: {res[0]} versions ({res[1]})")
    else:
        print(f"[NOT IN DB] {a}")
