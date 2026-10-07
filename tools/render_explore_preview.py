import sqlite3
from PIL import Image, ImageDraw, ImageFont

# Colors from KordApp Theme
BG = (14, 17, 23)           # #0E1117
CARD_SURFACE = (22, 27, 34) # #161B22
RAISED_SURFACE = (26, 32, 44)# #1A202C
BORDER = (36, 44, 61)       # #242C3D
BORDER_MUTED = (25, 30, 42)
ACCENT_GOLD = (229, 169, 60)# #E5A93C
PRIMARY_TEXT = (245, 248, 255)# #F5F8FF
MUTED_TEXT = (142, 154, 168)# #8E9AA8

# Load catalog data
conn = sqlite3.connect(r'tools\catalog\full_merged_catalog.db')
c = conn.cursor()
c.execute("SELECT count(DISTINCT artist) FROM songs")
total_artists = c.fetchone()[0]

c.execute("""
    SELECT substr(upper(artist), 1, 1) as letter, count(DISTINCT artist) 
    FROM songs 
    WHERE substr(upper(artist), 1, 1) BETWEEN 'A' AND 'Z' 
    GROUP BY 1
""")
letter_counts = dict(c.fetchall())

# Fetch top artists for letter 'D'
c.execute("""
    SELECT artist, count(*) as cnt 
    FROM songs 
    WHERE upper(artist) LIKE 'D%' 
    GROUP BY artist 
    ORDER BY cnt DESC 
    LIMIT 8
""")
d_artists = c.fetchall()

# Fonts
font_title = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 44)
font_subtitle = ImageFont.truetype(r'C:\Windows\Fonts\segoeui.ttf', 26)
font_search = ImageFont.truetype(r'C:\Windows\Fonts\segoeui.ttf', 30)
font_badge = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 24)
font_grid_letter = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 52)
font_grid_count = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 22)
font_artist_name = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 34)
font_artist_count = ImageFont.truetype(r'C:\Windows\Fonts\segoeui.ttf', 24)
font_initial = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 40)
font_chip = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 26)
font_status = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 26)

W, H = 1080, 2400

def draw_status_and_bottom_nav(img, draw, active_nav="explore"):
    # Status bar
    draw.text((70, 48), "8:45", font=font_status, fill=PRIMARY_TEXT)
    # Wifi & battery
    draw.text((950, 48), "5G  85%", font=font_status, fill=PRIMARY_TEXT)

    # Bottom Nav Bar
    nav_h = 180
    nav_y = H - nav_h
    draw.rectangle([0, nav_y, W, H], fill=CARD_SURFACE)
    draw.line([0, nav_y, W, nav_y], fill=BORDER, width=2)

    nav_items = [("Home", "🏠"), ("Explore", "🧭"), ("Setlists", "🎵"), ("Favorites", "❤️"), ("Settings", "⚙️")]
    tab_w = W // len(nav_items)
    for i, (name, icon) in enumerate(nav_items):
        cx = i * tab_w + tab_w // 2
        cy = nav_y + 65
        is_active = (name.lower() == active_nav.lower())
        if is_active:
            # Gold pill
            draw.rounded_rectangle([cx - 55, cy - 35, cx + 55, cy + 15], radius=25, fill=ACCENT_GOLD)
            draw.text((cx - 15, cy - 32), icon, font=font_subtitle, fill=BG)
            draw.text((cx - draw.textlength(name, font=font_badge)//2, cy + 30), name, font=font_badge, fill=ACCENT_GOLD)
        else:
            draw.text((cx - 15, cy - 25), icon, font=font_subtitle, fill=MUTED_TEXT)
            draw.text((cx - draw.textlength(name, font=font_badge)//2, cy + 30), name, font=font_badge, fill=MUTED_TEXT)

    # Navigation bar pill at very bottom
    draw.rounded_rectangle([W//2 - 140, H - 24, W//2 + 140, H - 16], radius=4, fill=(80, 85, 95))

# ==========================================
# 1. RENDER GRID VIEW
# ==========================================
img_grid = Image.new("RGB", (W, H), BG)
draw_grid = ImageDraw.Draw(img_grid)

# Header
draw_grid.text((50, 130), "EXPLORE", font=font_title, fill=PRIMARY_TEXT)
draw_grid.text((50, 185), "Katalog Artis & Musisi", font=font_subtitle, fill=MUTED_TEXT)

# Total badge
badge_txt = f"{total_artists:,} Artis"
bw = draw_grid.textlength(badge_txt, font=font_badge) + 36
bx = W - 50 - bw
draw_grid.rounded_rectangle([bx, 140, W - 50, 195], radius=16, fill=RAISED_SURFACE, outline=BORDER, width=2)
draw_grid.text((bx + 18, 152), badge_txt, font=font_badge, fill=ACCENT_GOLD)

# Search Bar
sb_y = 235
draw_grid.rounded_rectangle([50, sb_y, W - 50, sb_y + 90], radius=24, fill=CARD_SURFACE, outline=BORDER, width=2)
draw_grid.text((80, sb_y + 26), "🔍", font=font_search, fill=ACCENT_GOLD)
draw_grid.text((140, sb_y + 26), "Cari nama artis atau band...", font=font_search, fill=MUTED_TEXT)

# Section title
sec_y = 355
draw_grid.text((50, sec_y), "KATALOG HURUF (A–Z)", font=font_badge, fill=MUTED_TEXT)
draw_grid.text((W - 190, sec_y), "Pilih huruf", font=font_subtitle, fill=MUTED_TEXT)

# 5-column Grid
grid_y = 410
col_w = (W - 100 - 4 * 20) // 5
card_h = col_w

letters = [chr(c) for c in range(ord('A'), ord('Z') + 1)]
for idx, letter in enumerate(letters):
    row = idx // 5
    col = idx % 5
    x = 50 + col * (col_w + 20)
    y = grid_y + row * (card_h + 20)

    cnt = letter_counts.get(letter, 0)
    enabled = cnt > 0

    if enabled:
        draw_grid.rounded_rectangle([x, y, x + col_w, y + card_h], radius=22, fill=RAISED_SURFACE, outline=BORDER, width=2)
        # Big letter
        lw = draw_grid.textlength(letter, font=font_grid_letter)
        draw_grid.text((x + (col_w - lw)//2, y + 25), letter, font=font_grid_letter, fill=PRIMARY_TEXT)
        # Count below
        cw = draw_grid.textlength(str(cnt), font=font_grid_count)
        draw_grid.text((x + (col_w - cw)//2, y + 102), str(cnt), font=font_grid_count, fill=ACCENT_GOLD)
    else:
        draw_grid.rounded_rectangle([x, y, x + col_w, y + card_h], radius=22, fill=BG, outline=BORDER_MUTED, width=1)
        lw = draw_grid.textlength(letter, font=font_grid_letter)
        draw_grid.text((x + (col_w - lw)//2, y + 35), letter, font=font_grid_letter, fill=(50, 55, 65))

draw_status_and_bottom_nav(img_grid, draw_grid)
img_grid.save(r'C:\KordApp-AGY\preview_explore_grid.png')

# ==========================================
# 2. RENDER ARTIST LIST VIEW (Letter D)
# ==========================================
img_list = Image.new("RGB", (W, H), BG)
draw_list = ImageDraw.Draw(img_list)

# Header
draw_list.text((50, 130), "EXPLORE", font=font_title, fill=PRIMARY_TEXT)
draw_list.text((50, 185), "Katalog Artis & Musisi", font=font_subtitle, fill=MUTED_TEXT)
draw_list.rounded_rectangle([bx, 140, W - 50, 195], radius=16, fill=RAISED_SURFACE, outline=BORDER, width=2)
draw_list.text((bx + 18, 152), badge_txt, font=font_badge, fill=ACCENT_GOLD)

# Search Bar
draw_list.rounded_rectangle([50, sb_y, W - 50, sb_y + 90], radius=24, fill=CARD_SURFACE, outline=BORDER, width=2)
draw_list.text((80, sb_y + 26), "🔍", font=font_search, fill=ACCENT_GOLD)
draw_list.text((140, sb_y + 26), "Cari nama artis atau band...", font=font_search, fill=MUTED_TEXT)

# Horizontal Alphabet Quick Scroller
chip_y = 350
chip_x = 50

# 'Semua Huruf' chip
chip_all_txt = "‹ Semua Huruf"
chip_all_w = draw_list.textlength(chip_all_txt, font=font_chip) + 36
draw_list.rounded_rectangle([chip_x, chip_y, chip_x + chip_all_w, chip_y + 60], radius=16, fill=RAISED_SURFACE, outline=BORDER, width=2)
draw_list.text((chip_x + 18, chip_y + 14), chip_all_txt, font=font_chip, fill=ACCENT_GOLD)
chip_x += chip_all_w + 16

sample_letters = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J']
for l in sample_letters:
    is_sel = (l == 'D')
    cw = draw_list.textlength(l, font=font_chip) + 40
    if is_sel:
        draw_list.rounded_rectangle([chip_x, chip_y, chip_x + cw, chip_y + 60], radius=16, fill=ACCENT_GOLD)
        draw_list.text((chip_x + 20, chip_y + 14), l, font=font_chip, fill=BG)
    else:
        draw_list.rounded_rectangle([chip_x, chip_y, chip_x + cw, chip_y + 60], radius=16, fill=RAISED_SURFACE, outline=BORDER, width=2)
        draw_list.text((chip_x + 20, chip_y + 14), l, font=font_chip, fill=PRIMARY_TEXT)
    chip_x += cw + 12

# Section title
sec2_y = 440
draw_list.text((50, sec2_y), "ARTIS D", font=font_badge, fill=ACCENT_GOLD)
d_count = letter_counts.get('D', len(d_artists))
draw_list.text((W - 250, sec2_y), f"{d_count} artis ditemukan", font=font_subtitle, fill=MUTED_TEXT)

# Artist Cards
list_y = 495
card_h_artist = 125
for artist_name, song_count in d_artists:
    if list_y + card_h_artist > H - 200:
        break
    draw_list.rounded_rectangle([50, list_y, W - 50, list_y + card_h_artist], radius=22, fill=RAISED_SURFACE, outline=BORDER, width=2)

    # Initial Avatar Box
    av_x = 75
    av_y = list_y + 20
    draw_list.rounded_rectangle([av_x, av_y, av_x + 85, av_y + 85], radius=18, fill=CARD_SURFACE, outline=BORDER, width=1)
    init_txt = artist_name[0].upper()
    iw = draw_list.textlength(init_txt, font=font_initial)
    draw_list.text((av_x + (85 - iw)//2, av_y + 14), init_txt, font=font_initial, fill=ACCENT_GOLD)

    # Name and songs
    draw_list.text((av_x + 110, list_y + 24), artist_name, font=font_artist_name, fill=PRIMARY_TEXT)
    draw_list.text((av_x + 110, list_y + 70), f"🎵 {song_count} lagu", font=font_artist_count, fill=MUTED_TEXT)

    # Trailing Chevron button
    btn_x = W - 120
    btn_y = list_y + 40
    draw_list.rounded_rectangle([btn_x, btn_y, btn_x + 45, btn_y + 45], radius=22, fill=CARD_SURFACE)
    draw_list.text((btn_x + 16, btn_y + 5), "›", font=font_title, fill=ACCENT_GOLD)

    list_y += card_h_artist + 18

draw_status_and_bottom_nav(img_list, draw_list)
img_list.save(r'C:\KordApp-AGY\preview_explore_artists.png')

# ==========================================
# 3. SIDE-BY-SIDE PRESENTATION SHOWCASE
# ==========================================
showcase_w = 2300
showcase_h = 2600
img_showcase = Image.new("RGB", (showcase_w, showcase_h), (10, 12, 16))
draw_show = ImageDraw.Draw(img_showcase)

# Showcase Title
font_banner_title = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 64)
font_banner_sub = ImageFont.truetype(r'C:\Windows\Fonts\segoeui.ttf', 36)

draw_show.text((100, 80), "KordApp — Desain Baru Explore Screen", font=font_banner_title, fill=PRIMARY_TEXT)
draw_show.text((100, 165), "Grid Katalog A–Z dengan Angka Riil & Pita Navigasi Cepat (Quick Alphabet Scroller)", font=font_banner_sub, fill=MUTED_TEXT)

# Paste scaled devices
target_h = 2200
target_w = int(W * (target_h / H))

grid_resized = img_grid.resize((target_w, target_h), Image.Resampling.LANCZOS)
list_resized = img_list.resize((target_w, target_h), Image.Resampling.LANCZOS)

# Outer glow/border frame for mockup 1
x1 = 100
y1 = 250
draw_show.rounded_rectangle([x1 - 8, y1 - 8, x1 + target_w + 8, y1 + target_h + 8], radius=32, fill=(28, 36, 50))
img_showcase.paste(grid_resized, (x1, y1))
draw_show.text((x1 + 30, y1 + target_h + 20), "1. Tampilan Utama: Grid 26 Huruf A-Z dengan Jumlah Artis Riil", font=font_badge, fill=ACCENT_GOLD)

# Outer glow/border frame for mockup 2
x2 = x1 + target_w + 120
draw_show.rounded_rectangle([x2 - 8, y1 - 8, x2 + target_w + 8, y1 + target_h + 8], radius=32, fill=(28, 36, 50))
img_showcase.paste(list_resized, (x2, y1))
draw_show.text((x2 + 30, y1 + target_h + 20), "2. Tampilan Terpilih (Huruf D): Quick Alphabet Bar & Kartu Artis Ber-Avatar", font=font_badge, fill=ACCENT_GOLD)

img_showcase.save(r'C:\KordApp-AGY\preview_explore.png')
print("Saved preview images successfully!")
