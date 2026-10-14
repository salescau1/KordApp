import os
from PIL import Image, ImageDraw, ImageFont

# Canvas: 2200 x 2400 px for side-by-side comparison
WIDTH = 2240
HEIGHT = 2400

# Theme Colors (Stage Dark)
CANVAS_BG = (8, 10, 14, 255)
BG_COLOR = (14, 17, 23, 255)             # #0E1117 Stage Dark
CARD_BG = (22, 27, 34, 255)              # #161B22 Card Surface
RAISED_BG = (28, 34, 44, 255)            # #1C222C
BORDER_COLOR = (40, 50, 69, 255)         # #283245
GOLD = (229, 169, 60, 255)               # #E5A93C Brand Gold
GOLD_BG = (42, 33, 12, 255)              # #2A210C
CHORD_GOLD = (255, 183, 77, 255)         # #FFB74D High-vis Amber Chord
TEXT_PRIMARY = (245, 248, 255, 255)      # #F5F8FF
TEXT_MUTED = (142, 154, 168, 255)        # #8E9AA8
GREEN_ACCENT = (34, 197, 94, 255)        # #22C55E

FONT_DIR = "C:/Windows/Fonts"
font_header_title = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 46)
font_header_sub = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 26)
font_phone_title = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 32)
font_phone_sub = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 22)
font_status = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 20)
font_chord = ImageFont.truetype(f"{FONT_DIR}/consolab.ttf", 26)
font_lyric = ImageFont.truetype(f"{FONT_DIR}/consola.ttf", 24)
font_center_title = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 38)
font_center_sub = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 24)
font_dock_symbol = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 22)

im = Image.new("RGBA", (WIDTH, HEIGHT), CANVAS_BG)
draw = ImageDraw.Draw(im)

def draw_rounded_rect(draw_obj, bounds, radius, fill=None, outline=None, width=1):
    draw_obj.rounded_rectangle(bounds, radius=radius, fill=fill, outline=outline, width=width)

# =========================================================================
# HEADER BANNER (Top)
# =========================================================================
banner_text = "ALUR LAYAR STANDBY PANGGUNG (STAGE PLAYER ATMOSPHERE)"
draw.text((WIDTH // 2 - 580, 50), banner_text, fill=GOLD, font=font_header_title)
sub_text = "Transisi mulus dari mode standby (tanpa bar kontrol) ke mode live sync lirik (bar player otomatis muncul)"
draw.text((WIDTH // 2 - 520, 110), sub_text, fill=TEXT_MUTED, font=font_header_sub)

# Phone frame geometry
phone_w = 980
phone_h = 2100
phone_y = 190
p1_x = 90
p2_x = WIDTH - phone_w - 90

def draw_phone_frame(x, y, label, step_tag):
    # Step badge above phone
    draw.text((x + 20, y - 40), step_tag, fill=GOLD, font=font_status)
    draw.text((x + 170, y - 42), label, fill=TEXT_PRIMARY, font=font_phone_title)

    # Outer phone bezel
    draw_rounded_rect(draw, (x, y, x + phone_w, y + phone_h), radius=54, fill=BG_COLOR, outline=(35, 42, 56, 255), width=8)

    # Dynamic Island / Notch
    notch_w = 140
    notch_h = 26
    nx = x + (phone_w - notch_w) // 2
    draw_rounded_rect(draw, (nx, y + 14, nx + notch_w, y + 14 + notch_h), radius=13, fill=(0, 0, 0, 255))

    # Status Bar
    draw.text((x + 44, y + 36), "09:41", fill=TEXT_PRIMARY, font=font_phone_sub)
    draw_rounded_rect(draw, (x + phone_w - 90, y + 40, x + phone_w - 44, y + 62), radius=5, outline=TEXT_PRIMARY, width=2)
    draw.rectangle((x + phone_w - 86, y + 44, x + phone_w - 52, y + 58), fill=GREEN_ACCENT)

    # Gesture home pill at bottom
    draw_rounded_rect(draw, (x + phone_w // 2 - 120, y + phone_h - 22, x + phone_w // 2 + 120, y + phone_h - 14), radius=4, fill=(80, 90, 105, 255))


def draw_stage_header(px, py, title, sub_title=None):
    # Header Format 3 Kolom Sesuai Arahan Boss:
    # [ Close ] ......... [ Title ] ......... [ Live Sync ]
    header_y = py
    close_size = 64
    close_x = px + 40
    # 1. Kolom Kiri: [ Close ] (Tombol Tutup / Keluar silang ✕)
    draw_rounded_rect(draw, (close_x, header_y, close_x + close_size, header_y + close_size), radius=32, fill=CARD_BG, outline=BORDER_COLOR, width=1)
    cx = close_x + close_size // 2
    cy = header_y + close_size // 2
    cr = 10
    draw.line((cx - cr, cy - cr, cx + cr, cy + cr), fill=TEXT_PRIMARY, width=3)
    draw.line((cx - cr, cy + cr, cx + cr, cy - cr), fill=TEXT_PRIMARY, width=3)

    # 2. Kolom Kanan: [ Live Sync ] (Badge / Pill Hijau Indikator Sinkronisasi Aktif)
    sync_w = 180
    sync_h = 50
    sync_x = px + phone_w - 40 - sync_w
    sync_y = header_y + 7
    draw_rounded_rect(draw, (sync_x, sync_y, sync_x + sync_w, sync_y + sync_h), radius=25, fill=(16, 34, 25, 255), outline=(34, 197, 94, 180), width=1)
    draw.ellipse((sync_x + 20, sync_y + 17, sync_x + 36, sync_y + 33), fill=(34, 197, 94, 60))
    draw.ellipse((sync_x + 23, sync_y + 20, sync_x + 33, sync_y + 30), fill=GREEN_ACCENT)
    draw.text((sync_x + 46, sync_y + 12), "Live Sync", fill=GREEN_ACCENT, font=font_status)

    # 3. Kolom Tengah: [ Title ] (Judul Lagu atau Standby Panggung terpusat horizontal)
    if sub_title:
        tb_t = draw.textbbox((0, 0), title, font=font_phone_title)
        tw_t = tb_t[2] - tb_t[0]
        draw.text((px + (phone_w - tw_t) // 2, header_y + 6), title, fill=TEXT_PRIMARY, font=font_phone_title)
        
        tb_s = draw.textbbox((0, 0), sub_title, font=font_phone_sub)
        tw_s = tb_s[2] - tb_s[0]
        draw.text((px + (phone_w - tw_s) // 2, header_y + 40), sub_title, fill=GOLD, font=font_phone_sub)
    else:
        tb_t = draw.textbbox((0, 0), title, font=font_phone_title)
        tw_t = tb_t[2] - tb_t[0]
        draw.text((px + (phone_w - tw_t) // 2, header_y + 16), title, fill=TEXT_PRIMARY, font=font_phone_title)


# =========================================================================
# 1. PHONE KIRI: KONDISI STANDBY (MENUNGGU LAGU)
# - Header 3 Kolom: [ Close ✕ ] [ Standby Panggung ] [ Live Sync 🟢 ]
# - TIDAK ADA BAR KONTROL DI BAWAH (Layar sangat lega)
# - Di tengah: Animasi resonansi not balok musik ♫ + teks waiting
# =========================================================================
draw_phone_frame(p1_x, phone_y, "Kondisi Standby (Menunggu)", "LANGKAH 1:")

# Top Header Island (Format 3 Kolom: [ Close ] [ Title ] [ Live Sync ])
top_y1 = phone_y + 90
draw_stage_header(p1_x, top_y1, "Standby Panggung")

# Clean Empty Canvas (NO BOTTOM BAR!)
# Center Pulsing Waves & Radar
center_y1 = phone_y + phone_h // 2 - 40
center_x1 = p1_x + phone_w // 2

# Pulsing Glow Rings (Musical Resonance)
for r, op in [(185, 35), (135, 70), (90, 120)]:
    draw.ellipse((center_x1 - r, center_y1 - r, center_x1 + r, center_y1 + r), outline=(229, 169, 60, op), width=2)

# Central Glowing Sphere with Warm Amber Tint
draw.ellipse((center_x1 - 58, center_y1 - 58, center_x1 + 58, center_y1 + 58), fill=GOLD_BG, outline=GOLD, width=3)

# Vector Double Musical Note (♫ / Beamed Eighth Notes) in Warm Gold
# Note 1 (Left): Head & Stem
nx1 = center_x1 - 20
ny1 = center_y1 + 18
draw.ellipse((nx1 - 13, ny1 - 10, nx1 + 13, ny1 + 10), fill=GOLD)
stem1_x = nx1 + 9
stem1_top = center_y1 - 28
draw.line((stem1_x, ny1, stem1_x, stem1_top), fill=GOLD, width=5)

# Note 2 (Right): Head & Stem
nx2 = center_x1 + 18
ny2 = center_y1 + 10
draw.ellipse((nx2 - 13, ny2 - 10, nx2 + 13, ny2 + 10), fill=GOLD)
stem2_x = nx2 + 9
stem2_top = center_y1 - 36
draw.line((stem2_x, ny2, stem2_x, stem2_top), fill=GOLD, width=5)

# Connecting Beam (Angled Bar on Top)
beam_pts = [
    (stem1_x - 2, stem1_top + 4),
    (stem2_x + 3, stem2_top + 4),
    (stem2_x + 3, stem2_top - 7),
    (stem1_x - 2, stem1_top - 7)
]
draw.polygon(beam_pts, fill=GOLD)

# Text beneath radar
t1 = "Connected to Gitar-Leader"
tb1 = draw.textbbox((0, 0), t1, font=font_center_title)
tw1 = tb1[2] - tb1[0]
draw.text((center_x1 - tw1 // 2, center_y1 + 120), t1, fill=TEXT_PRIMARY, font=font_center_title)

t2 = "Waiting for lyrics..."
tb2 = draw.textbbox((0, 0), t2, font=font_center_sub)
tw2 = tb2[2] - tb2[0]
draw.text((center_x1 - tw2 // 2, center_y1 + 175), t2, fill=TEXT_MUTED, font=font_center_sub)

# Clean Status Tag at bottom of screen (No dock, just breathing space!)
draw_rounded_rect(draw, (center_x1 - 180, phone_y + phone_h - 130, center_x1 + 180, phone_y + phone_h - 85), radius=22, fill=CARD_BG, outline=BORDER_COLOR, width=1)
draw.text((center_x1 - 150, phone_y + phone_h - 117), "✓ Standby panggung aktif (Tanpa Dock)", fill=GOLD, font=font_status)


# =========================================================================
# 2. PHONE KANAN: KONDISI LAGU DIMAINKAN (LIVE SYNC)
# - Leader memilih lagu -> Lirik & Akord muncul mengisi layar
# - BAR KONTROL DI BAWAH OTOMATIS MUNCUL (Floating Dock Player)
# =========================================================================
draw_phone_frame(p2_x, phone_y, "Kondisi Lirik Masuk (Player Aktif)", "LANGKAH 2:")

# Top Header Island (Format 3 Kolom: [ Close ] [ Title ] [ Live Sync ])
top_y2 = phone_y + 90
draw_stage_header(p2_x, top_y2, "Bohemian Rhapsody", "Queen")

# Sheet Music (Chords & Lyrics)
sheet_y = phone_y + 200
sample_lines = [
    ("[Intro]", None),
    ("Bb              Gm", "Is this the real life?"),
    ("Cm              F", "Is this just fantasy?"),
    ("Bb", "Caught in a landslide,"),
    ("Cm                  F", "No escape from reality"),
    ("[Verse 1]", None),
    ("Gm", "Open your eyes,"),
    ("Bb               Eb", "Look up to the skies and see"),
    ("Cm             F", "I'm just a poor boy, I need no sympathy,"),
    ("B   Bb   A    Bb", "Because I'm easy come, easy go,"),
    ("B   Bb   A    Bb", "Little high, little low,"),
    ("Eb         Bb", "Any way the wind blows"),
    ("Dbdim      F             Bb", "Doesn't really matter to me..."),
    ("[Verse 2]", None),
    ("Bb              Gm", "Mama, just killed a man,"),
    ("Cm", "Put a gun against his head,"),
    ("F", "Pulled my trigger, now he's dead,"),
    ("Bb              Gm", "Mama, life had just begun,"),
    ("Cm", "But now I've gone and thrown it all away..."),
]

for chord, lyric in sample_lines:
    if lyric is None:
        draw.text((p2_x + 60, sheet_y), chord, fill=GOLD, font=font_chord)
        sheet_y += 46
    else:
        draw.text((p2_x + 60, sheet_y), chord, fill=CHORD_GOLD, font=font_chord)
        sheet_y += 36
        draw.text((p2_x + 60, sheet_y), lyric, fill=TEXT_PRIMARY, font=font_lyric)
        sheet_y += 54

# Bottom Scrim Gradient (Fadeout behind dock)
scrim_h = 240
scrim = Image.new("RGBA", (phone_w - 16, scrim_h), (0, 0, 0, 0))
sdraw = ImageDraw.Draw(scrim)
for sy in range(scrim_h):
    alpha = int(255 * ((sy / scrim_h) ** 1.3))
    sdraw.line((0, sy, phone_w - 16, sy), fill=(14, 17, 23, alpha))
im.paste(scrim, (p2_x + 8, phone_y + phone_h - scrim_h - 10), scrim)

# FLOATING BOTTOM DOCK (MUNCUL OTOMATIS KETIKA LAGU AKTIF)
dock_w = 820
dock_h = 92
dock_x = p2_x + (phone_w - dock_w) // 2
dock_y = phone_y + phone_h - dock_h - 45
draw_rounded_rect(draw, (dock_x, dock_y, dock_x + dock_w, dock_y + dock_h), radius=36, fill=(24, 30, 40, 240), outline=BORDER_COLOR, width=2)

# Dock Controls inside
# Play/Pause auto-scroll
draw.text((dock_x + 36, dock_y + 30), "▶", fill=GOLD, font=font_dock_symbol)
draw.text((dock_x + 80, dock_y + 32), "1.0×", fill=TEXT_PRIMARY, font=font_status)

draw.line((dock_x + 155, dock_y + 24, dock_x + 155, dock_y + 68), fill=BORDER_COLOR, width=1)

# Page Flip Book
draw.text((dock_x + 180, dock_y + 32), "Flip", fill=TEXT_MUTED, font=font_status)

draw.line((dock_x + 240, dock_y + 24, dock_x + 240, dock_y + 68), fill=BORDER_COLOR, width=1)

# Transpose Segment
draw.text((dock_x + 270, dock_y + 30), "−", fill=TEXT_PRIMARY, font=font_dock_symbol)
draw.text((dock_x + 320, dock_y + 32), "0", fill=GOLD, font=font_dock_symbol)
draw.text((dock_x + 365, dock_y + 30), "+", fill=TEXT_PRIMARY, font=font_dock_symbol)

draw.line((dock_x + 415, dock_y + 24, dock_x + 415, dock_y + 68), fill=BORDER_COLOR, width=1)

# Font Size Segment
draw.text((dock_x + 445, dock_y + 32), "a", fill=TEXT_MUTED, font=font_status)
draw.text((dock_x + 490, dock_y + 32), "14", fill=GOLD, font=font_status)
draw.text((dock_x + 535, dock_y + 28), "A", fill=TEXT_PRIMARY, font=font_dock_symbol)

draw.line((dock_x + 585, dock_y + 24, dock_x + 585, dock_y + 68), fill=BORDER_COLOR, width=1)

# Awake Lamp
draw.text((dock_x + 610, dock_y + 32), "Lampu", fill=GOLD, font=font_status)

# Disconnect Small Chip on dock
draw_rounded_rect(draw, (dock_x + 675, dock_y + 20, dock_x + 795, dock_y + 72), radius=16, fill=(45, 20, 20, 255), outline=(239, 68, 68, 255), width=1)
draw.text((dock_x + 692, dock_y + 34), "Putus", fill=(239, 68, 68, 255), font=font_status)


# Save Flow Image
out_path = r"C:\KordApp-AGY\Preview\standby_player_flow_preview.png"
os.makedirs(os.path.dirname(out_path), exist_ok=True)
im.save(out_path, "PNG")
print(f"Standby player flow preview generated successfully at: {out_path}")
