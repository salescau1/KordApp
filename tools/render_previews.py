import math
from PIL import Image, ImageDraw, ImageFont

# Theme Colors (KordApp Dark Theme)
BG = (14, 17, 23)                # #0E1117 (Dark background)
CARD_SURFACE = (22, 27, 34)      # #161B22
RAISED_SURFACE = (28, 35, 48)    # #1C2330
BORDER = (45, 55, 72)            # #2D3748
BORDER_LIGHT = (70, 84, 108)
ACCENT_GOLD = (229, 169, 60)     # #E5A93C
SPOTIFY_GREEN = (30, 215, 96)    # Spotify green
YOUTUBE_RED = (255, 68, 68)      # YouTube red
PRIMARY_TEXT = (245, 248, 255)   # #F5F8FF
MUTED_TEXT = (142, 154, 168)     # #8E9AA8

W, H = 1080, 2400

font_title = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 44)
font_section = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 28)
font_subtitle = ImageFont.truetype(r'C:\Windows\Fonts\segoeui.ttf', 26)
font_body = ImageFont.truetype(r'C:\Windows\Fonts\segoeui.ttf', 30)
font_body_b = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 32)
font_small = ImageFont.truetype(r'C:\Windows\Fonts\segoeui.ttf', 22)
font_small_b = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 24)
font_grid = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 36)
font_rank = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 34)
font_status = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 26)
font_symbol = ImageFont.truetype(r'C:\Windows\Fonts\arial.ttf', 38)

def draw_search_icon(draw, cx, cy, color=ACCENT_GOLD):
    # Magnifying glass
    r = 14
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=4)
    draw.line([cx + 10, cy + 10, cx + 22, cy + 22], fill=color, width=4)

def draw_star_icon(draw, cx, cy, size=18, color=ACCENT_GOLD):
    # 5-pointed star
    points = []
    for i in range(10):
        r = size if i % 2 == 0 else size * 0.45
        angle = i * math.pi / 5 - math.pi / 2
        points.append((cx + r * math.cos(angle), cy + r * math.sin(angle)))
    draw.polygon(points, fill=color)

def draw_phone_chrome(draw):
    draw.text((70, 48), "9:41", font=font_status, fill=PRIMARY_TEXT)
    draw.text((920, 48), "5G  100%", font=font_status, fill=PRIMARY_TEXT)

def draw_nav_icon(draw, tab_name, cx, cy, color):
    if tab_name == "Home":
        # House
        draw.polygon([(cx, cy - 18), (cx - 18, cy), (cx + 18, cy)], fill=color)
        draw.rectangle([cx - 12, cy, cx + 12, cy + 16], fill=color)
        draw.rectangle([cx - 4, cy + 6, cx + 4, cy + 16], fill=CARD_SURFACE)
    elif tab_name == "Explore":
        # Compass circle with needle
        draw.ellipse([cx - 16, cy - 16, cx + 16, cy + 16], outline=color, width=3)
        draw.polygon([(cx - 4, cy), (cx + 4, cy), (cx + 10, cy - 10)], fill=color)
        draw.polygon([(cx - 4, cy), (cx + 4, cy), (cx - 10, cy + 10)], fill=MUTED_TEXT)
    elif tab_name == "Setlists":
        # List lines
        draw.rectangle([cx - 14, cy - 16, cx + 14, cy + 16], outline=color, width=3)
        draw.line([cx - 8, cy - 8, cx + 8, cy - 8], fill=color, width=2)
        draw.line([cx - 8, cy, cx + 8, cy], fill=color, width=2)
        draw.line([cx - 8, cy + 8, cx + 4, cy + 8], fill=color, width=2)
    elif tab_name == "Favorites":
        # Heart / Star
        draw_star_icon(draw, cx, cy, size=15, color=color)
    elif tab_name == "Settings":
        # Gear circle
        draw.ellipse([cx - 14, cy - 14, cx + 14, cy + 14], outline=color, width=4)
        draw.ellipse([cx - 5, cy - 5, cx + 5, cy + 5], fill=color)

def draw_floating_bottom_nav(img, active_tab="home"):
    nav_w = 980
    nav_h = 130
    nav_x = (W - nav_w) // 2
    nav_y = H - nav_h - 48

    nav_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    nav_draw = ImageDraw.Draw(nav_layer)
    nav_draw.rounded_rectangle([nav_x, nav_y, nav_x + nav_w, nav_y + nav_h], radius=40, fill=(22, 27, 34, 245), outline=BORDER, width=2)
    img.alpha_composite(nav_layer)

    draw = ImageDraw.Draw(img)
    tabs = ["Home", "Explore", "Setlists", "Favorites", "Settings"]
    tab_slot = nav_w // len(tabs)
    for i, name in enumerate(tabs):
        cx = nav_x + i * tab_slot + tab_slot // 2
        cy = nav_y + nav_h // 2
        is_active = (name.lower() == active_tab.lower())
        color = ACCENT_GOLD if is_active else MUTED_TEXT

        if is_active:
            draw.rounded_rectangle([cx - 50, cy - 46, cx + 50, cy + 10], radius=22, fill=(229, 169, 60, 45), outline=ACCENT_GOLD, width=1)
        
        draw_nav_icon(draw, name, cx, cy - 18, color)
        draw.text((cx - draw.textlength(name, font=font_small_b)//2, cy + 14), name, font=font_small_b, fill=color)

# ==========================================
# 1. RENDER HOME SCREEN
# ==========================================
def render_home():
    img = Image.new("RGBA", (W, H), BG + (255,))
    draw = ImageDraw.Draw(img)

    top_songs = [
        ("1", "You're Beautiful", "James Blunt", ["UG", "GT"]),
        ("2", "Kemesraan", "Iwan Fals", ["UG", "ChordTela"]),
        ("3", "Pupus", "Dewa 19", ["UG", "GT", "ChordTela"]),
        ("4", "Perfect", "Ed Sheeran", ["UG", "GT"]),
        ("5", "Dan...", "Sheila on 7", ["UG", "ChordTela"]),
        ("6", "Hampa", "Ari Lasso", ["UG", "ChordTela"]),
        ("7", "Bohemian Rhapsody", "Queen", ["UG", "GT"]),
        ("8", "Menghapus Jejakmu", "Peterpan", ["UG", "GT", "ChordTela"]),
        ("9", "Someone You Loved", "Lewis Capaldi", ["UG", "GT"]),
        ("10", "Ruang Rindu", "Letto", ["UG", "ChordTela"]),
        ("11", "Hotel California", "Eagles", ["UG", "GT"]),
        ("12", "Cinta Luar Biasa", "Andmesh", ["UG", "ChordTela"]),
        ("13", "Fix You", "Coldplay", ["UG", "GT"]),
        ("14", "Bento", "Iwan Fals", ["UG", "ChordTela"]),
        ("15", "Yellow", "Coldplay", ["UG", "GT"]),
        ("16", "Separuh Aku", "NOAH", ["UG", "GT", "ChordTela"]),
        ("17", "Asmalibrasi", "Soegi Bornean", ["UG", "ChordTela"]),
        ("18", "Akad", "Payung Teduh", ["UG", "ChordTela"]),
    ]

    # Draw Top List rows underneath
    row_y = 660
    for rank, title, artist, sources in top_songs:
        draw.rounded_rectangle([44, row_y, W - 44, row_y + 116], radius=24, fill=CARD_SURFACE, outline=BORDER, width=1)
        r_color = ACCENT_GOLD if int(rank) <= 3 else MUTED_TEXT
        draw.text((76, row_y + 36), rank, font=font_rank, fill=r_color)
        draw.text((150, row_y + 22), title, font=font_body_b, fill=PRIMARY_TEXT)
        draw.text((150, row_y + 64), artist, font=font_subtitle, fill=MUTED_TEXT)
        src_text = " • ".join(sources)
        draw.text((W - 80 - draw.textlength(src_text, font=font_small), row_y + 44), src_text, font=font_small, fill=ACCENT_GOLD)
        row_y += 132

    # Draw Top Frosted Blur Background Header Scrim
    for y in range(0, 680):
        alpha = int(255 * min(1.0, max(0.0, (680 - y) / 90)))
        fade = Image.new("RGBA", (W, 1), BG + (alpha,))
        img.alpha_composite(fade, (0, y))

    draw = ImageDraw.Draw(img)
    draw_phone_chrome(draw)

    # 1. Floating Search Bar Pill: (search)
    search_y = 120
    draw.rounded_rectangle([44, search_y, W - 44, search_y + 88], radius=44, fill=CARD_SURFACE, outline=BORDER, width=2)
    draw_search_icon(draw, 88, search_y + 44, ACCENT_GOLD)
    draw.text((130, search_y + 26), "Cari lagu, artis, lirik...", font=font_body, fill=MUTED_TEXT)

    # 2. Floating Recently Played Split Pill:
    # (recently | You're Beautiful )
    # (Played   | James Blunt      )
    rec_y = 232
    draw.rounded_rectangle([44, rec_y, W - 44, rec_y + 120], radius=32, fill=CARD_SURFACE, outline=ACCENT_GOLD, width=2)
    draw.text((75, rec_y + 28), "RECENTLY", font=font_small_b, fill=ACCENT_GOLD)
    draw.text((88, rec_y + 60), "PLAYED", font=font_small_b, fill=ACCENT_GOLD)
    draw.line([225, rec_y + 20, 225, rec_y + 100], fill=BORDER_LIGHT, width=2)
    draw.text((255, rec_y + 26), "You're Beautiful", font=font_body_b, fill=PRIMARY_TEXT)
    draw.text((255, rec_y + 66), "James Blunt", font=font_subtitle, fill=MUTED_TEXT)
    draw.text((W - 88, rec_y + 38), "›", font=font_rank, fill=ACCENT_GOLD)

    # 3. Platform Switcher Pills: (top 50 Spotify) (top 50 YouTube)
    plat_y = 376
    # Spotify Pill (Active)
    draw.rounded_rectangle([44, plat_y, 450, plat_y + 76], radius=38, fill=CARD_SURFACE, outline=ACCENT_GOLD, width=2)
    # Spotify green dot
    draw.ellipse([70, plat_y + 26, 94, plat_y + 50], fill=SPOTIFY_GREEN)
    draw.text((110, plat_y + 19), "Top 50 Spotify", font=font_body_b, fill=ACCENT_GOLD)

    # YouTube Pill (Inactive / Display)
    draw.rounded_rectangle([470, plat_y, 876, plat_y + 76], radius=38, fill=CARD_SURFACE, outline=BORDER, width=2)
    # YouTube red play triangle
    draw.polygon([(496, plat_y + 26), (496, plat_y + 50), (518, plat_y + 38)], fill=YOUTUBE_RED)
    draw.text((534, plat_y + 19), "Top 50 YouTube", font=font_body_b, fill=MUTED_TEXT)

    # Section title
    draw.text((50, 480), "TOP 50 LAGU TERPOPULER", font=font_section, fill=PRIMARY_TEXT)
    draw.text((50, 518), "Disesuaikan otomatis dengan ketersediaan kunci gitar lokal", font=font_small, fill=MUTED_TEXT)

    # Bottom Scrim Fade
    for y in range(H - 300, H):
        factor = (y - (H - 300)) / 300.0
        alpha = int(255 * factor)
        scrim = Image.new("RGBA", (W, 1), BG + (alpha,))
        img.alpha_composite(scrim, (0, y))

    draw_floating_bottom_nav(img, active_tab="home")
    img.convert("RGB").save("preview_home.png", "PNG")
    print("Saved preview_home.png")

# ==========================================
# 2. RENDER EXPLORE SCREEN (Option A: Genres + 7x4 Grid)
# ==========================================
def render_explore():
    img = Image.new("RGBA", (W, H), BG + (255,))
    draw = ImageDraw.Draw(img)

    draw_phone_chrome(draw)

    # Search Bar
    search_y = 120
    draw.rounded_rectangle([44, search_y, W - 44, search_y + 88], radius=44, fill=CARD_SURFACE, outline=BORDER, width=2)
    draw_search_icon(draw, 88, search_y + 44, ACCENT_GOLD)
    draw.text((130, search_y + 26), "Cari artis, genre, lagu...", font=font_body, fill=MUTED_TEXT)

    # SECTION 1: GENRE & KATEGORI (Top Section)
    sec1_y = 236
    draw.text((48, sec1_y), "JELAJAH GENRE & KATEGORI", font=font_section, fill=ACCENT_GOLD)

    genres = [
        ("Rock 90s", "101 Artis • Nirvana, Dewa 19, Sheila on 7", ACCENT_GOLD),
        ("OST & Soundtrack", "380+ Lagu Film & Sinetron Terpopuler", (80, 180, 240)),
        ("Pop Indo", "213 Artis • Judika, Glenn Fredly, Tulus", (240, 140, 180)),
        ("Dangdut & Koplo", "43 Artis • Rhoma Irama, Denny Caknan", (240, 180, 80)),
        ("Classic Rock", "140 Artis • Queen, Led Zeppelin, Iwan Fals", (220, 100, 100)),
        ("Indie & Akustik", "251 Artis • Payung Teduh, Fourtwnty", (120, 220, 140)),
    ]

    card_w = (W - 44 * 2 - 20) // 2
    card_h = 136
    gx = 44
    gy = sec1_y + 44

    for i, (title, desc, tag_color) in enumerate(genres):
        col = i % 2
        row = i // 2
        cx = 44 + col * (card_w + 20)
        cy = gy + row * (card_h + 16)

        draw.rounded_rectangle([cx, cy, cx + card_w, cy + card_h], radius=24, fill=CARD_SURFACE, outline=BORDER, width=2)
        # Category indicator bar
        draw.rounded_rectangle([cx + 20, cy + 26, cx + 26, cy + 62], radius=3, fill=tag_color)
        draw.text((cx + 38, cy + 24), title, font=font_body_b, fill=PRIMARY_TEXT)
        draw.text((cx + 20, cy + 74), desc, font=font_small, fill=MUTED_TEXT)

    # SECTION 2: KATALOG ABJAD (# — Z) (Exact 7 columns x 4 rows = 28 cells)
    sec2_y = gy + 3 * (card_h + 16) + 36
    draw.text((48, sec2_y), "KATALOG ABJAD (# — Z)", font=font_section, fill=ACCENT_GOLD)
    draw.text((draw.textlength("KATALOG ABJAD (# — Z)", font=font_section) + 68, sec2_y + 6), "• 28 Kategori Kompak (7 x 4)", font=font_small, fill=MUTED_TEXT)

    grid_items = [
        "#", "A", "B", "C", "D", "E", "F",
        "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T",
        "U", "V", "W", "X", "Y", "Z", "★"
    ]

    cols = 7
    rows = 4
    pad = 12
    total_grid_w = W - 44 * 2
    cell_w = (total_grid_w - (cols - 1) * pad) // cols
    cell_h = 96
    grid_start_y = sec2_y + 50

    for idx, item in enumerate(grid_items):
        r = idx // cols
        c = idx % cols
        cell_x = 44 + c * (cell_w + pad)
        cell_y = grid_start_y + r * (cell_h + pad)

        is_star = (item == "★")
        is_hash = (item == "#")
        is_selected = (item == "D")

        fill_col = RAISED_SURFACE if (is_star or is_hash) else CARD_SURFACE
        outline_col = ACCENT_GOLD if (is_star or is_selected) else BORDER

        draw.rounded_rectangle([cell_x, cell_y, cell_x + cell_w, cell_y + cell_h], radius=20, fill=fill_col, outline=outline_col, width=2)

        if is_star:
            draw_star_icon(draw, cell_x + cell_w // 2, cell_y + cell_h // 2, size=18, color=ACCENT_GOLD)
        else:
            tw = draw.textlength(item, font=font_grid)
            text_color = ACCENT_GOLD if is_selected else PRIMARY_TEXT
            draw.text((cell_x + (cell_w - tw) // 2, cell_y + 20), item, font=font_grid, fill=text_color)

    # Bottom list: Artists for letter 'D'
    list_y = grid_start_y + rows * (cell_h + pad) + 36
    draw.text((48, list_y), "CONTOH ARTIS HURUF 'D' (189 Artis)", font=font_section, fill=PRIMARY_TEXT)

    d_artists = [
        ("Dewa 19", "47 Lagu • Pop Rock / Balad"),
        ("D'Masiv", "32 Lagu • Pop Alternatif"),
        ("Danilla Riyadi", "18 Lagu • Indie Jazz"),
        ("Denny Caknan", "24 Lagu • Pop Jawa / Koplo"),
        ("Doel Sumbang", "38 Lagu • Pop Sunda / Balada"),
    ]

    ay = list_y + 50
    for name, info in d_artists:
        draw.rounded_rectangle([44, ay, W - 44, ay + 96], radius=20, fill=CARD_SURFACE, outline=BORDER, width=1)
        draw.rounded_rectangle([64, ay + 18, 124, ay + 78], radius=16, fill=RAISED_SURFACE)
        draw.text((82, ay + 24), "D", font=font_body_b, fill=ACCENT_GOLD)
        draw.text((144, ay + 18), name, font=font_body_b, fill=PRIMARY_TEXT)
        draw.text((144, ay + 56), info, font=font_small, fill=MUTED_TEXT)
        draw.text((W - 84, ay + 30), "›", font=font_rank, fill=MUTED_TEXT)
        ay += 110

    draw_floating_bottom_nav(img, active_tab="explore")
    img.convert("RGB").save("preview_explore.png", "PNG")
    print("Saved preview_explore.png")

if __name__ == "__main__":
    render_home()
    render_explore()
