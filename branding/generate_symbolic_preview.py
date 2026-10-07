import os
from PIL import Image, ImageDraw, ImageFont

WIDTH = 1080
HEIGHT = 2340

# Colors
BG_COLOR = (14, 17, 23)           # #0E1117 Stage Dark
CARD_BG = (22, 27, 34)            # #161B22
RAISED_BG = (26, 32, 44)          # #1A202C
BORDER_COLOR = (36, 44, 61)       # #242C3D
GOLD = (229, 169, 60)             # #E5A93C Brand Gold
GOLD_BG = (42, 33, 12)            # #2A210C
CHORD_COLOR = (255, 183, 77)      # #FFB74D High-visibility Warm Amber
TEXT_PRIMARY = (245, 248, 255)    # #F5F8FF
TEXT_MUTED = (142, 154, 168)      # #8E9AA8
HEART_ACTIVE = (255, 77, 109)     # #FF4D6D Active Heart

FONT_DIR = "C:/Windows/Fonts"
font_title = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 40)
font_subtitle = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 26)
font_meta = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 26)
font_section = ImageFont.truetype(f"{FONT_DIR}/consolab.ttf", 36)
font_chord = ImageFont.truetype(f"{FONT_DIR}/consolab.ttf", 35)
font_lyric = ImageFont.truetype(f"{FONT_DIR}/consola.ttf", 33)
font_btn_symbol = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 30)
font_btn_val = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 26)

def draw_play_icon(draw, cx, cy, size, color):
    pts = [(cx - size // 2 + 2, cy - size // 2), (cx - size // 2 + 2, cy + size // 2), (cx + size // 2 + 3, cy)]
    draw.polygon(pts, fill=color)

def draw_book_icon(draw, cx, cy, w, h, color):
    # Two pages open book
    draw.line((cx, cy - h // 2, cx, cy + h // 2), fill=color, width=3) # Spine
    draw.rounded_rectangle((cx - w, cy - h // 2, cx - 2, cy + h // 2), radius=3, outline=color, width=2)
    draw.rounded_rectangle((cx + 2, cy - h // 2, cx + w, cy + h // 2), radius=3, outline=color, width=2)

def draw_bulb_icon(draw, cx, cy, size, color):
    # Lightbulb
    draw.ellipse((cx - size // 2, cy - size // 2 - 4, cx + size // 2, cy + size // 2 - 4), outline=color, width=3)
    draw.rectangle((cx - 6, cy + size // 2 - 4, cx + 6, cy + size // 2 + 4), fill=color)

def draw_down_triangle(draw, cx, cy, size, color):
    pts = [(cx - size, cy - size // 2), (cx + size, cy - size // 2), (cx, cy + size // 2 + 1)]
    draw.polygon(pts, fill=color)

im = Image.new("RGBA", (WIDTH, HEIGHT), BG_COLOR)
draw = ImageDraw.Draw(im)

# 1. STATUS BAR
draw.text((60, 32), "12:52", fill=TEXT_PRIMARY, font=font_subtitle)
draw.arc((880, 36, 915, 70), start=200, end=340, fill=TEXT_PRIMARY, width=3)
draw.arc((886, 42, 909, 65), start=200, end=340, fill=TEXT_PRIMARY, width=3)
draw.point((897, 56), fill=TEXT_PRIMARY)
for i in range(4):
    bx = 935 + i * 8
    by = 56 - i * 5
    draw.rectangle((bx, by, bx + 5, 66), fill=TEXT_PRIMARY)
draw.rounded_rectangle((980, 38, 1030, 64), radius=6, outline=TEXT_PRIMARY, width=2)
draw.rectangle((984, 42, 1022, 60), fill=(34, 197, 94))
draw.rectangle((1030, 46, 1034, 56), fill=TEXT_PRIMARY)

# 2. TOP APP BAR
draw.rectangle((0, 95, WIDTH, 220), fill=CARD_BG)
draw.line((0, 220, WIDTH, 220), fill=BORDER_COLOR, width=1)
draw.line((50, 157, 85, 157), fill=TEXT_PRIMARY, width=4)
draw.line((50, 157, 65, 142), fill=TEXT_PRIMARY, width=4)
draw.line((50, 157, 65, 172), fill=TEXT_PRIMARY, width=4)

title = "Bohemian Rhapsody"
artist = "Queen"
t_bbox = draw.textbbox((0, 0), title, font=font_title)
t_w = t_bbox[2] - t_bbox[0]
draw.text(((WIDTH - t_w) // 2, 120), title, fill=TEXT_PRIMARY, font=font_title)
a_bbox = draw.textbbox((0, 0), artist, font=font_subtitle)
a_w = a_bbox[2] - a_bbox[0]
draw.text(((WIDTH - a_w) // 2, 170), artist, fill=TEXT_MUTED, font=font_subtitle)

# Favorite Heart & Add to Setlist
hx, hy = 890, 157
draw.polygon([(hx, hy-8), (hx-14, hy-22), (hx-26, hy-14), (hx-26, hy+2), (hx, hy+26), 
              (hx+26, hy+2), (hx+26, hy-14), (hx+14, hy-22)], fill=HEART_ACTIVE)
draw.ellipse((hx-26, hy-22, hx, hy), fill=HEART_ACTIVE)
draw.ellipse((hx, hy-22, hx+26, hy), fill=HEART_ACTIVE)

sx, sy = 990, 157
draw.ellipse((sx-28, sy-28, sx+28, sy+28), fill=GOLD)
draw.line((sx-12, sy-10, sx+12, sy-10), fill=BG_COLOR, width=3)
draw.line((sx-12, sy-2, sx+8, sy-2), fill=BG_COLOR, width=3)
draw.line((sx-12, sy+6, sx+4, sy+6), fill=BG_COLOR, width=3)
draw.line((sx+10, sy+2, sx+10, sy+14), fill=BG_COLOR, width=3)
draw.line((sx+4, sy+8, sx+16, sy+8), fill=BG_COLOR, width=3)

# 3. METADATA STRIP (CLEAN)
draw.rounded_rectangle((50, 235, 175, 272), radius=6, fill=GOLD_BG, outline=GOLD, width=1)
draw.text((66, 239), "Key: Bb", fill=GOLD, font=font_meta)

draw.text((205, 241), "Beginner", fill=TEXT_MUTED, font=font_subtitle)

draw.rounded_rectangle((910, 235, 1030, 272), radius=8, fill=CARD_BG, outline=BORDER_COLOR, width=1)
draw.text((926, 239), "v1/2", fill=TEXT_PRIMARY, font=font_meta)
draw_down_triangle(draw, 1005, 256, 5, TEXT_PRIMARY)

# 4. CHORD SHEET (100% BERSIH, LEGA, TANPA POPUP MENUTUPI)
y_sheet = 320
draw.text((50, y_sheet), "[Intro 1]", fill=GOLD, font=font_section)
y_sheet += 62

lines_data = [
    ("Bb", "Is this the real life?"),
    ("C", "Is this just fantasy?"),
    ("F", "Caught in a landslide"),
    ("      Bb", "No escape from reality"),
    ("Gm", "Open your eyes"),
    ("      Bb              Eb", "Look up to the skies and see"),
    ("Cm", "I'm just a poor boy"),
    ("F", "I need no sympathy"),
    ("      B    Bb     A    Bb", "Because I'm easy come, easy go"),
    ("B     Bb    A     Bb", "Little high, little low"),
    ("Eb          Bb", "Any way the wind blows"),
    ("Dbdim       F              Bb  Bb", "doesn't really matter to me, to me"),
]

for chord, lyric in lines_data:
    draw.text((50, y_sheet), chord, fill=CHORD_COLOR, font=font_chord)
    y_sheet += 42
    draw.text((50, y_sheet), lyric, fill=TEXT_PRIMARY, font=font_lyric)
    y_sheet += 56

y_sheet += 25
draw.text((50, y_sheet), "[Verse 1]", fill=GOLD, font=font_section)
y_sheet += 62
verse_lines = [
    ("Bb            Gm", "Mama, just killed a man"),
    ("      Cm", "Put a gun against his head"),
    ("                    F", "Pulled my trigger, now he's dead"),
    ("Bb            Gm", "Mama, life had just begun"),
    ("      Cm", "But now I've gone and thrown it all away"),
    ("Eb          Ab   Eb   F", "Mama, ooh, didn't mean to make you cry"),
]
for chord, lyric in verse_lines:
    draw.text((50, y_sheet), chord, fill=CHORD_COLOR, font=font_chord)
    y_sheet += 42
    draw.text((50, y_sheet), lyric, fill=TEXT_PRIMARY, font=font_lyric)
    y_sheet += 56


# 5. NEW: FULL-SYMBOL SINGLE SLEEK TOOLBAR (HANYA 1 BARIS RAMPING & PROPORSIONAL)
# Height ~150px (y: 2110..2260)
tb_top = 2110
tb_bottom = 2260

draw.rectangle((0, tb_top, WIDTH, HEIGHT), fill=CARD_BG)
draw.line((0, tb_top, WIDTH, tb_top), fill=BORDER_COLOR, width=2)

btn_y = 2140
btn_h = 92
corner = 18

# ELEMEN 1: AUTO-SCROLL (Icon Play) - Lebar 120px
# Active Gold State
btn1_x = 40
btn1_w = 125
draw.rounded_rectangle((btn1_x, btn_y, btn1_x + btn1_w, btn_y + btn_h), radius=corner, fill=GOLD)
draw_play_icon(draw, btn1_x + btn1_w // 2, btn_y + btn_h // 2, 28, BG_COLOR)

# ELEMEN 2: PAGE FLIP (Icon Book / Slides) - Lebar 120px
btn2_x = btn1_x + btn1_w + 18
btn2_w = 125
draw.rounded_rectangle((btn2_x, btn_y, btn2_x + btn2_w, btn_y + btn_h), radius=corner, fill=RAISED_BG, outline=BORDER_COLOR, width=2)
draw_book_icon(draw, btn2_x + btn2_w // 2, btn_y + btn_h // 2, 16, 26, TEXT_PRIMARY)

# ELEMEN 3: TRANSPOSE SEGMENTED PILL [ − | 0 | + ] - Lebar 280px
btn3_x = btn2_x + btn2_w + 20
btn3_w = 280
draw.rounded_rectangle((btn3_x, btn_y, btn3_x + btn3_w, btn_y + btn_h), radius=corner, fill=RAISED_BG, outline=BORDER_COLOR, width=2)
# Minus
draw.text((btn3_x + 30, btn_y + 24), "−", fill=TEXT_PRIMARY, font=font_btn_symbol)
draw.line((btn3_x + 90, btn_y, btn3_x + 90, btn_y + btn_h), fill=BORDER_COLOR, width=2)
# Value / Reset (0)
draw.text((btn3_x + 130, btn_y + 28), "0", fill=GOLD, font=font_btn_val)
draw.line((btn3_x + 190, btn_y, btn3_x + 190, btn_y + btn_h), fill=BORDER_COLOR, width=2)
# Plus
draw.text((btn3_x + 230, btn_y + 24), "+", fill=TEXT_PRIMARY, font=font_btn_symbol)

# ELEMEN 4: FONT SIZE SEGMENTED PILL [ a | 0 | A ] - Lebar 280px
btn4_x = btn3_x + btn3_w + 20
btn4_w = 280
draw.rounded_rectangle((btn4_x, btn_y, btn4_x + btn4_w, btn_y + btn_h), radius=corner, fill=RAISED_BG, outline=BORDER_COLOR, width=2)
# a kecil (Decrease)
draw.text((btn4_x + 35, btn_y + 28), "a", fill=TEXT_PRIMARY, font=ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 26))
draw.line((btn4_x + 90, btn_y, btn4_x + 90, btn_y + btn_h), fill=BORDER_COLOR, width=2)
# Value / Reset (0)
draw.text((btn4_x + 130, btn_y + 28), "0", fill=GOLD, font=font_btn_val)
draw.line((btn4_x + 190, btn_y, btn4_x + 190, btn_y + btn_h), fill=BORDER_COLOR, width=2)
# A besar (Increase)
draw.text((btn4_x + 228, btn_y + 26), "A", fill=TEXT_PRIMARY, font=ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 30))

# ELEMEN 5: KEEP SCREEN (Icon Lampu) - Lebar 125px
btn5_x = btn4_x + btn4_w + 18
btn5_w = 125
draw.rounded_rectangle((btn5_x, btn_y, btn5_x + btn5_w, btn_y + btn_h), radius=corner, fill=RAISED_BG, outline=GOLD, width=2)
draw_bulb_icon(draw, btn5_x + btn5_w // 2, btn_y + btn_h // 2, 28, GOLD)


# Android System Nav Indicator
nav_pill_y = 2290
draw.rounded_rectangle((WIDTH // 2 - 120, nav_pill_y, WIDTH // 2 + 120, nav_pill_y + 8), radius=4, fill=(180, 190, 205, 180))

out_path = "C:/KordApp-AGY/branding/preview_player_symbolic.png"
im.save(out_path, "PNG")
print(f"Generated symbolic clean preview: {out_path}")
