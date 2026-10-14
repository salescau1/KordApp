import os
from PIL import Image, ImageDraw, ImageFont

WIDTH = 1080
HEIGHT = 2340

# Colors (Stage Dark Minimalist Palette)
BG_COLOR = (12, 14, 18, 255)            # #0C0E12 Ultra deep sleek obsidian
CARD_BG = (19, 23, 30, 255)             # #13171E Minimalist card surface
RAISED_BG = (26, 32, 42, 255)           # #1A202A
BORDER_COLOR = (34, 43, 56, 255)        # #222B38 Delicate subtle border
BORDER_SUBTLE = (26, 33, 44, 255)
GOLD = (229, 169, 60, 255)              # #E5A93C Brand Gold
GOLD_BG = (42, 33, 12, 255)             # #2A210C
TEXT_PRIMARY = (245, 248, 255, 255)     # #F5F8FF Off-white
TEXT_MUTED = (125, 138, 153, 255)       # #7D8A99 Sleek muted
GREEN_ACCENT = (34, 197, 94, 255)       # #22C55E
RED_ACCENT = (239, 68, 68, 255)         # #EF4444 Delete/Danger
RED_BG = (45, 20, 20, 255)

FONT_DIR = "C:/Windows/Fonts"
font_title = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 44)
font_section_label = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 20)
font_row_label = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 28)
font_row_label_b = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 28)
font_row_value = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 25)
font_row_value_b = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 25)
font_badge = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 21)
font_sub = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 20)
font_nav = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 20)
font_nav_active = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 20)
font_chevron = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 30)

im = Image.new("RGBA", (WIDTH, HEIGHT), BG_COLOR)
draw = ImageDraw.Draw(im)

def draw_rounded_rect(draw_obj, bounds, radius, fill=None, outline=None, width=1):
    draw_obj.rounded_rectangle(bounds, radius=radius, fill=fill, outline=outline, width=width)

# 1. STATUS BAR
draw.text((64, 34), "09:41", fill=TEXT_PRIMARY, font=ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 22))
draw.arc((875, 38, 910, 72), start=200, end=340, fill=TEXT_PRIMARY, width=3)
draw.arc((882, 44, 903, 67), start=200, end=340, fill=TEXT_PRIMARY, width=3)
draw.point((892, 56), fill=TEXT_PRIMARY)
for i in range(4):
    bx = 930 + i * 8
    by = 58 - i * 5
    draw.rectangle((bx, by, bx + 5, 68), fill=TEXT_PRIMARY)
draw_rounded_rect(draw, (974, 40, 1022, 66), radius=6, outline=TEXT_PRIMARY, width=2)
draw.rectangle((978, 44, 1016, 62), fill=GREEN_ACCENT)
draw.rectangle((1022, 48, 1026, 58), fill=TEXT_PRIMARY)

# 2. SCREEN TITLE
draw.text((64, 98), "Pengaturan", fill=TEXT_PRIMARY, font=font_title)

def draw_card_container(top_y, height):
    draw_rounded_rect(draw, (48, top_y, WIDTH - 48, top_y + height), radius=22, fill=CARD_BG, outline=BORDER_COLOR, width=1)

def draw_row(top_y, height, title, right_widget=None, subtitle=None, show_divider=True):
    offset_y = 6 if subtitle else 0
    draw.text((76, top_y + (height - 32) // 2 - offset_y), title, fill=TEXT_PRIMARY, font=font_row_label)
    if subtitle:
        draw.text((76, top_y + (height - 32) // 2 + 26), subtitle, fill=TEXT_MUTED, font=font_sub)
    if right_widget:
        right_widget(top_y, height)
    if show_divider:
        draw.line((76, top_y + height, WIDTH - 76, top_y + height), fill=BORDER_SUBTLE, width=1)


# =========================================================================
# 1. MODE: [ Server | Normal | Client ]
# Normal: HAPUS SUBTITLE TOTAL (Bersih tanpa teks deskripsi)
# =========================================================================
y_curr = 175
draw.text((64, y_curr), "MODE", fill=TEXT_MUTED, font=font_section_label)
y_curr += 34

g1_h = 86
draw_card_container(y_curr, g1_h)

seg_w = WIDTH - 48 * 2 - 20
seg_h = 62
seg_x = 48 + 10
seg_y = y_curr + 12
draw_rounded_rect(draw, (seg_x, seg_y, seg_x + seg_w, seg_y + seg_h), radius=16, fill=RAISED_BG, outline=BORDER_SUBTLE, width=1)

modes = ["Server", "Normal", "Client"]
active_idx = 1 # Normal is active
part_w = (seg_w - 8) // 3

for i, m in enumerate(modes):
    px = seg_x + 4 + i * part_w
    py = seg_y + 4
    if i == active_idx:
        draw_rounded_rect(draw, (px, py, px + part_w, py + seg_h - 8), radius=12, fill=GOLD)
        draw.text((px + part_w // 2 - 36, py + 12), m, fill=BG_COLOR, font=font_row_label_b)
    else:
        draw.text((px + part_w // 2 - 34, py + 12), m, fill=TEXT_MUTED, font=font_row_label)


# =========================================================================
# 2. APPEARANCE (Tema 5 circle swatches & Language [ID|EN])
# =========================================================================
y_curr = y_curr + g1_h + 30
draw.text((64, y_curr), "APPEARANCE", fill=TEXT_MUTED, font=font_section_label)
y_curr += 34

g2_row_h = 92
g2_h = g2_row_h * 2
draw_card_container(y_curr, g2_h)

# Row 1: Theme
def render_theme_swatches(row_y, row_h):
    themes = [
        {"bg": (14, 17, 23), "active": True},
        {"bg": (245, 246, 248), "active": False},
        {"bg": (244, 239, 230), "active": False},
        {"bg": (28, 34, 40), "active": False},
        {"bg": (20, 38, 28), "active": False}
    ]
    sw_radius = 18
    sw_gap = 18
    total_w = len(themes) * (sw_radius * 2) + (len(themes) - 1) * sw_gap
    start_x = WIDTH - 76 - total_w
    cy = row_y + row_h // 2

    for t in themes:
        cx = start_x + sw_radius
        if t["active"]:
            draw.ellipse((cx - sw_radius - 4, cy - sw_radius - 4, cx + sw_radius + 4, cy + sw_radius + 4), outline=GOLD, width=3)
            draw.ellipse((cx - sw_radius, cy - sw_radius, cx + sw_radius, cy + sw_radius), fill=t["bg"])
            draw.ellipse((cx - 4, cy - 4, cx + 4, cy + 4), fill=GOLD)
        else:
            draw.ellipse((cx - sw_radius, cy - sw_radius, cx + sw_radius, cy + sw_radius), fill=t["bg"], outline=BORDER_COLOR, width=1)
        start_x += sw_radius * 2 + sw_gap

draw_row(y_curr, g2_row_h, "Theme", right_widget=render_theme_swatches)

# Row 2: Language
def render_lang_segment(row_y, row_h):
    cap_w = 160
    cap_h = 48
    cap_x = WIDTH - 76 - cap_w
    cap_y = row_y + (row_h - cap_h) // 2
    draw_rounded_rect(draw, (cap_x, cap_y, cap_x + cap_w, cap_y + cap_h), radius=24, fill=RAISED_BG, outline=BORDER_COLOR, width=1)
    half = cap_w // 2
    draw_rounded_rect(draw, (cap_x + 3, cap_y + 3, cap_x + half, cap_y + cap_h - 3), radius=21, fill=GOLD)
    draw.text((cap_x + 22, cap_y + 11), "ID", fill=BG_COLOR, font=font_badge)
    draw.text((cap_x + half + 20, cap_y + 11), "EN", fill=TEXT_MUTED, font=font_badge)

draw_row(y_curr + g2_row_h, g2_row_h, "Language", right_widget=render_lang_segment, show_divider=False)


# =========================================================================
# 3. MUSIC & CATALOG
# Bersih tanpa subtitle:
#   Artists         3.328
#   Songs         157.842
#   Catalog Size    80 MB
# Download Catalog: [ Paket A ] [ Paket B ] [ Paket C ]
# (Terpasang ada tombol "Hapus", belum ada "Unduh")
# =========================================================================
y_curr = y_curr + g2_h + 30
draw.text((64, y_curr), "MUSIC & CATALOG", fill=TEXT_MUTED, font=font_section_label)
y_curr += 34

g3_h = 440
draw_card_container(y_curr, g3_h)

# Row 1: Artists (clean, no subtitle)
def render_artists(row_y, row_h):
    cy = row_y + row_h // 2
    draw.text((WIDTH - 160, cy - 14), "3.328", fill=GOLD, font=font_row_value_b)

draw_row(y_curr, 74, "Artists", right_widget=render_artists)

# Row 2: Songs (clean, no subtitle)
def render_songs(row_y, row_h):
    cy = row_y + row_h // 2
    draw.text((WIDTH - 180, cy - 14), "157.842", fill=GOLD, font=font_row_value_b)

draw_row(y_curr + 74, 74, "Songs", right_widget=render_songs)

# Row 3: Catalog Size (clean, no subtitle)
def render_cat_size(row_y, row_h):
    cy = row_y + row_h // 2
    draw.text((WIDTH - 170, cy - 14), "80 MB", fill=TEXT_PRIMARY, font=font_row_value_b)

draw_row(y_curr + 148, 74, "Catalog Size", right_widget=render_cat_size)

# Download Catalog Section with Paket A, Paket B, Paket C
cat_tier_y = y_curr + 236
draw.text((76, cat_tier_y), "Download Catalog", fill=TEXT_PRIMARY, font=font_row_label_b)

tier_w = (WIDTH - 48 * 2 - 24 * 2 - 16 * 2) // 3
tier_h = 136
tier_y = cat_tier_y + 44

tiers = [
    {"name": "Paket A", "songs": "5k Lagu", "size": "8 MB", "status": "download"},
    {"name": "Paket B", "songs": "40k Lagu", "size": "28 MB", "status": "download"},
    {"name": "Paket C", "songs": "157k Lagu", "size": "80 MB", "status": "installed"}
]

for i, t in enumerate(tiers):
    tx = 72 + i * (tier_w + 16)
    is_installed = t["status"] == "installed"
    box_border = GOLD if is_installed else BORDER_COLOR
    draw_rounded_rect(draw, (tx, tier_y, tx + tier_w, tier_y + tier_h), radius=16, fill=RAISED_BG, outline=box_border, width=2 if is_installed else 1)

    draw.text((tx + 14, tier_y + 12), t["name"], fill=GOLD if is_installed else TEXT_PRIMARY, font=font_badge)
    draw.text((tx + 14, tier_y + 40), t["songs"], fill=TEXT_PRIMARY, font=font_sub)
    draw.text((tx + 14, tier_y + 64), t["size"], fill=TEXT_MUTED, font=font_sub)

    btn_h = 32
    btn_y = tier_y + tier_h - btn_h - 12
    if is_installed:
        # Tombol Hapus merah
        draw_rounded_rect(draw, (tx + 12, btn_y, tx + tier_w - 12, btn_y + btn_h), radius=8, fill=RED_BG, outline=RED_ACCENT, width=1)
        draw.text((tx + tier_w // 2 - 24, btn_y + 5), "Hapus", fill=RED_ACCENT, font=font_sub)
    else:
        # Tombol Unduh
        draw_rounded_rect(draw, (tx + 12, btn_y, tx + tier_w - 12, btn_y + btn_h), radius=8, fill=RAISED_BG, outline=BORDER_COLOR, width=1)
        draw.text((tx + tier_w // 2 - 24, btn_y + 5), "Unduh", fill=TEXT_MUTED, font=font_sub)


# =========================================================================
# 4. LICENSE & DEVICE
# - HAPUS BARIS DEVICE ID TOTAL!
# - Kode lisensi tersensor: KORD-••••-•••• 👁️
# - Transfer lisensi: Proteksi kunci layar/sidik jari + tombol Salin Kode
# =========================================================================
y_curr = y_curr + g3_h + 30
draw.text((64, y_curr), "LICENSE & DEVICE", fill=TEXT_MUTED, font=font_section_label)
y_curr += 34

g4_row_h = 92
g4_h = g4_row_h * 3  # Only 3 rows now! (Device ID row REMOVED)
draw_card_container(y_curr, g4_h)

# Row 1: License Status
def render_lic_status(row_y, row_h):
    cy = row_y + row_h // 2
    bw = 130
    bh = 38
    bx = WIDTH - 80 - bw
    draw_rounded_rect(draw, (bx, cy - bh // 2, bx + bw, cy + bh // 2), radius=10, fill=GOLD_BG, outline=GOLD, width=1)
    text = "PRO AKTIF"
    tb = draw.textbbox((0, 0), text, font=font_badge)
    tw = tb[2] - tb[0]
    draw.text((bx + (bw - tw) // 2, cy - bh // 2 + 7), text, fill=GOLD, font=font_badge)

draw_row(y_curr, g4_row_h, "License Status", subtitle="Lisensi permanen pro", right_widget=render_lic_status)

# Row 2: License Key (Masked: KORD-••••-•••• + [Lihat] button)
def render_lic_key(row_y, row_h):
    cy = row_y + row_h // 2
    draw.text((WIDTH - 370, cy - 14), "KORD-••••-••••", fill=TEXT_PRIMARY, font=ImageFont.truetype(f"{FONT_DIR}/consola.ttf", 23))
    # Native clean button [Lihat]
    ew = 80
    eh = 38
    ex = WIDTH - 80 - ew
    draw_rounded_rect(draw, (ex, cy - eh // 2, ex + ew, cy + eh // 2), radius=10, fill=RAISED_BG, outline=BORDER_COLOR, width=1)
    draw.text((ex + 18, cy - eh // 2 + 8), "Lihat", fill=TEXT_MUTED, font=font_sub)

draw_row(y_curr + g4_row_h, g4_row_h, "License Key", right_widget=render_lic_key)

# Row 3: Transfer License (Biometrik / Kunci Layar + Salin Kode button)
def render_transfer(row_y, row_h):
    cy = row_y + row_h // 2
    draw.text((WIDTH - 300, cy - 14), "Kunci Layar", fill=TEXT_MUTED, font=font_sub)
    # Salin Kode button
    cw = 120
    ch = 40
    cx = WIDTH - 80 - cw
    draw_rounded_rect(draw, (cx, cy - ch // 2, cx + cw, cy + ch // 2), radius=10, fill=RAISED_BG, outline=GOLD, width=1)
    draw.text((cx + 15, cy - ch // 2 + 8), "Salin Kode", fill=GOLD, font=font_sub)

draw_row(y_curr + g4_row_h * 2, g4_row_h, "Transfer License", subtitle="Proteksi kunci layar / sidik jari", right_widget=render_transfer, show_divider=False)


# =========================================================================
# 5. APPLICATION (Version, APK Size, Check Update, About)
# =========================================================================
y_curr = y_curr + g4_h + 30
draw.text((64, y_curr), "APPLICATION", fill=TEXT_MUTED, font=font_section_label)
y_curr += 34

g5_row_h = 82
g5_h = g5_row_h * 4
draw_card_container(y_curr, g5_h)

# Row 1: Version
def render_app_ver(row_y, row_h):
    cy = row_y + row_h // 2
    draw.text((WIDTH - 170, cy - 14), "v1.0.0", fill=TEXT_MUTED, font=font_row_value)

draw_row(y_curr, g5_row_h, "Version", right_widget=render_app_ver)

# Row 2: APK Size
def render_apk_size(row_y, row_h):
    cy = row_y + row_h // 2
    draw.text((WIDTH - 200, cy - 14), "24.8 MB", fill=TEXT_MUTED, font=font_row_value)

draw_row(y_curr + g5_row_h, g5_row_h, "APK Size", right_widget=render_apk_size)

# Row 3: Check Update
def render_chk_upd(row_y, row_h):
    cy = row_y + row_h // 2
    draw.text((WIDTH - 220, cy - 14), "Cek Versi ›", fill=GOLD, font=font_sub)

draw_row(y_curr + g5_row_h * 2, g5_row_h, "Check Update", right_widget=render_chk_upd)

# Row 4: About
def render_about_row(row_y, row_h):
    cy = row_y + row_h // 2
    draw.text((WIDTH - 95, cy - 17), "›", fill=TEXT_MUTED, font=font_chevron)

draw_row(y_curr + g5_row_h * 3, g5_row_h, "About", right_widget=render_about_row, show_divider=False)


# =========================================================================
# 6. BOTTOM NAVIGATION BAR
# =========================================================================
nav_y = 2170
nav_h = 170
draw_rounded_rect(draw, (0, nav_y, WIDTH, HEIGHT), radius=0, fill=CARD_BG)
draw.line((0, nav_y, WIDTH, nav_y), fill=BORDER_COLOR, width=1)

nav_items = [
    {"label": "Home", "icon": "🏠", "active": False},
    {"label": "Explore", "icon": "🧭", "active": False},
    {"label": "Setlist", "icon": "📑", "active": False},
    {"label": "Favorit", "icon": "♥", "active": False},
    {"label": "Pengaturan", "icon": "⚙", "active": True}
]

item_w = WIDTH // len(nav_items)
for i, item in enumerate(nav_items):
    ix = i * item_w
    center_x = ix + item_w // 2

    icon_color = GOLD if item["active"] else TEXT_MUTED
    label_color = GOLD if item["active"] else TEXT_MUTED
    label_font = font_nav_active if item["active"] else font_nav

    if item["active"]:
        draw_rounded_rect(draw, (center_x - 30, nav_y + 14, center_x + 30, nav_y + 48), radius=16, fill=GOLD_BG)

    draw.text((center_x - 14, nav_y + 16), item["icon"], fill=icon_color, font=font_row_label_b)
    text_bbox = draw.textbbox((0, 0), item["label"], font=label_font)
    tw = text_bbox[2] - text_bbox[0]
    draw.text((center_x - tw // 2, nav_y + 58), item["label"], fill=label_color, font=label_font)

# Gesture pill
draw_rounded_rect(draw, (WIDTH // 2 - 120, 2308, WIDTH // 2 + 120, 2316), radius=4, fill=(80, 90, 105, 255))

# Save Image
out_path = r"C:\KordApp-AGY\Preview\settings_revamp_preview.png"
os.makedirs(os.path.dirname(out_path), exist_ok=True)
im.save(out_path, "PNG")
print(f"Boss detailed specifications preview image successfully generated at: {out_path}")
