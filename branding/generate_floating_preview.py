import os
from PIL import Image, ImageDraw, ImageFont

WIDTH = 1080
HEIGHT = 2340

# Colors
BG_COLOR = (14, 17, 23)           # #0E1117 Stage Dark
CARD_BG = (22, 27, 34)            # #161B22
DOCK_BG = (22, 27, 34, 240)       # Floating Dock Surface (translucent dark)
RAISED_BG = (28, 35, 48)          # #1C2330
BORDER_COLOR = (42, 52, 72)       # #2A3448
GOLD = (229, 169, 60)             # #E5A93C Brand Gold
GOLD_BG = (42, 33, 12)            # #2A210C
CHORD_COLOR = (255, 183, 77)      # #FFB74D High-visibility Warm Amber
TEXT_PRIMARY = (245, 248, 255)    # #F5F8FF
TEXT_MUTED = (142, 154, 168)      # #8E9AA8
HEART_ACTIVE = (255, 77, 109)     # #FF4D6D Active Heart

FONT_DIR = "C:/Windows/Fonts"
font_title = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 38)
font_subtitle = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 24)
font_meta = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 24)
font_section = ImageFont.truetype(f"{FONT_DIR}/consolab.ttf", 36)
font_chord = ImageFont.truetype(f"{FONT_DIR}/consolab.ttf", 35)
font_lyric = ImageFont.truetype(f"{FONT_DIR}/consola.ttf", 33)
font_btn_symbol = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 28)
font_btn_val = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 24)

def draw_play_icon(draw, cx, cy, size, color):
    pts = [(cx - size // 2 + 2, cy - size // 2), (cx - size // 2 + 2, cy + size // 2), (cx + size // 2 + 3, cy)]
    draw.polygon(pts, fill=color)

def draw_book_icon(draw, cx, cy, w, h, color):
    draw.line((cx, cy - h // 2, cx, cy + h // 2), fill=color, width=3)
    draw.rounded_rectangle((cx - w, cy - h // 2, cx - 2, cy + h // 2), radius=3, outline=color, width=2)
    draw.rounded_rectangle((cx + 2, cy - h // 2, cx + w, cy + h // 2), radius=3, outline=color, width=2)

def draw_bulb_icon(draw, cx, cy, size, color):
    draw.ellipse((cx - size // 2, cy - size // 2 - 4, cx + size // 2, cy + size // 2 - 4), outline=color, width=3)
    draw.rectangle((cx - 5, cy + size // 2 - 4, cx + 5, cy + size // 2 + 4), fill=color)

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

# 2. TOP BAR: (<) (Title - Queen) (Love) (Add Setlist)
draw.rectangle((0, 95, WIDTH, 215), fill=CARD_BG)
draw.line((0, 215, WIDTH, 215), fill=BORDER_COLOR, width=1)

# Back Arrow (<)
draw.line((50, 155, 85, 155), fill=TEXT_PRIMARY, width=4)
draw.line((50, 155, 65, 140), fill=TEXT_PRIMARY, width=4)
draw.line((50, 155, 65, 170), fill=TEXT_PRIMARY, width=4)

# Center Title & Artist
title = "Bohemian Rhapsody"
artist = "Queen"
t_bbox = draw.textbbox((0, 0), title, font=font_title)
t_w = t_bbox[2] - t_bbox[0]
draw.text(((WIDTH - t_w) // 2, 118), title, fill=TEXT_PRIMARY, font=font_title)

a_bbox = draw.textbbox((0, 0), artist, font=font_subtitle)
a_w = a_bbox[2] - a_bbox[0]
draw.text(((WIDTH - a_w) // 2, 168), artist, fill=TEXT_MUTED, font=font_subtitle)

# Favorite Heart (Love)
hx, hy = 890, 155
draw.polygon([(hx, hy-8), (hx-14, hy-22), (hx-26, hy-14), (hx-26, hy+2), (hx, hy+26), 
              (hx+26, hy+2), (hx+26, hy-14), (hx+14, hy-22)], fill=HEART_ACTIVE)
draw.ellipse((hx-26, hy-22, hx, hy), fill=HEART_ACTIVE)
draw.ellipse((hx, hy-22, hx+26, hy), fill=HEART_ACTIVE)

# Add to Setlist Gold Circle
sx, sy = 990, 155
draw.ellipse((sx-28, sy-28, sx+28, sy+28), fill=GOLD)
draw.line((sx-12, sy-10, sx+12, sy-10), fill=BG_COLOR, width=3)
draw.line((sx-12, sy-2, sx+8, sy-2), fill=BG_COLOR, width=3)
draw.line((sx-12, sy+6, sx+4, sy+6), fill=BG_COLOR, width=3)
draw.line((sx+10, sy+2, sx+10, sy+14), fill=BG_COLOR, width=3)
draw.line((sx+4, sy+8, sx+16, sy+8), fill=BG_COLOR, width=3)

# 3. METADATA STRIP (Clean & Subtle)
draw.rounded_rectangle((50, 230, 165, 266), radius=6, fill=GOLD_BG, outline=GOLD, width=1)
draw.text((64, 234), "Key: Bb", fill=GOLD, font=font_meta)

draw.text((195, 236), "Beginner", fill=TEXT_MUTED, font=font_subtitle)

draw.rounded_rectangle((915, 230, 1030, 266), radius=8, fill=CARD_BG, outline=BORDER_COLOR, width=1)
draw.text((930, 234), "v1/2", fill=TEXT_PRIMARY, font=font_meta)
draw_down_triangle(draw, 1005, 249, 5, TEXT_PRIMARY)

# 4. CHORD SHEET (100% Bersih & Mengalir)
y_sheet = 310
draw.text((50, y_sheet), "[Intro 1]", fill=GOLD, font=font_section)
y_sheet += 60

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
y_sheet += 60
verse_lines = [
    ("Bb            Gm", "Mama, just killed a man"),
    ("      Cm", "Put a gun against his head"),
    ("                    F", "Pulled my trigger, now he's dead"),
    ("Bb            Gm", "Mama, life had just begun"),
    ("      Cm", "But now I've gone and thrown it all away"),
    ("Eb          Ab   Eb   F", "Mama, ooh, didn't mean to make you cry"),
    ("                    Bb", "If I'm not back again this time tomorrow"),
]
for chord, lyric in verse_lines:
    draw.text((50, y_sheet), chord, fill=CHORD_COLOR, font=font_chord)
    y_sheet += 42
    draw.text((50, y_sheet), lyric, fill=TEXT_PRIMARY, font=font_lyric)
    y_sheet += 56


# 5. FLOATING CAPSULE BAR (MELAYANG MIRIP TAB BAR CAPSULE)
# x: 40..1040 (width 1000px, height 110px), y: 2130..2240
dock_left = 40
dock_right = WIDTH - 40
dock_top = 2125
dock_bottom = 2245
dock_radius = 40

# Soft shadow under floating dock
shadow_dock = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
sd_draw = ImageDraw.Draw(shadow_dock)
sd_draw.rounded_rectangle((dock_left - 8, dock_top - 4, dock_right + 8, dock_bottom + 12), radius=dock_radius + 6, fill=(0, 0, 0, 180))
im = Image.alpha_composite(im, shadow_dock)
draw = ImageDraw.Draw(im)

# Floating capsule container
dock_surface = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
ds_draw = ImageDraw.Draw(dock_surface)
ds_draw.rounded_rectangle((dock_left, dock_top, dock_right, dock_bottom), radius=dock_radius, fill=DOCK_BG, outline=BORDER_COLOR, width=2)
im = Image.alpha_composite(im, dock_surface)
draw = ImageDraw.Draw(im)

# Buttons inside floating capsule:
# (scroll) (flip) (transpose) (size) (lampu)
btn_y = dock_top + 15
btn_h = 90
btn_rad = 22

# 1. SCROLL (Icon Play) - Lebar 115px
b1_x = dock_left + 16
b1_w = 115
draw.rounded_rectangle((b1_x, btn_y, b1_x + b1_w, btn_y + btn_h), radius=btn_rad, fill=GOLD)
draw_play_icon(draw, b1_x + b1_w // 2, btn_y + btn_h // 2, 28, BG_COLOR)

# 2. FLIP (Icon Book) - Lebar 115px
b2_x = b1_x + b1_w + 14
b2_w = 115
draw.rounded_rectangle((b2_x, btn_y, b2_x + b2_w, btn_y + btn_h), radius=btn_rad, fill=RAISED_BG, outline=BORDER_COLOR, width=1)
draw_book_icon(draw, b2_x + b2_w // 2, btn_y + btn_h // 2, 16, 26, TEXT_PRIMARY)

# 3. TRANSPOSE [ − | 0 | + ] - Lebar 280px
b3_x = b2_x + b2_w + 16
b3_w = 280
draw.rounded_rectangle((b3_x, btn_y, b3_x + b3_w, btn_y + btn_h), radius=btn_rad, fill=RAISED_BG, outline=BORDER_COLOR, width=1)
# Minus
draw.text((b3_x + 30, btn_y + 24), "−", fill=TEXT_PRIMARY, font=font_btn_symbol)
draw.line((b3_x + 90, btn_y + 8, b3_x + 90, btn_y + btn_h - 8), fill=BORDER_COLOR, width=1)
# 0 (tap to reset)
draw.text((b3_x + 130, btn_y + 28), "0", fill=GOLD, font=font_btn_val)
draw.line((b3_x + 190, btn_y + 8, b3_x + 190, btn_y + btn_h - 8), fill=BORDER_COLOR, width=1)
# Plus
draw.text((b3_x + 230, btn_y + 24), "+", fill=TEXT_PRIMARY, font=font_btn_symbol)

# 4. SIZE [ a | 0 | A ] - Lebar 280px
b4_x = b3_x + b3_w + 16
b4_w = 280
draw.rounded_rectangle((b4_x, btn_y, b4_x + b4_w, btn_y + btn_h), radius=btn_rad, fill=RAISED_BG, outline=BORDER_COLOR, width=1)
# a kecil
draw.text((b4_x + 35, btn_y + 28), "a", fill=TEXT_PRIMARY, font=ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 26))
draw.line((b4_x + 90, btn_y + 8, b4_x + 90, btn_y + btn_h - 8), fill=BORDER_COLOR, width=1)
# 0 (tap to reset)
draw.text((b4_x + 130, btn_y + 28), "0", fill=GOLD, font=font_btn_val)
draw.line((b4_x + 190, btn_y + 8, b4_x + 190, btn_y + btn_h - 8), fill=BORDER_COLOR, width=1)
# A besar
draw.text((b4_x + 228, btn_y + 25), "A", fill=TEXT_PRIMARY, font=ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 30))

# 5. LAMPU (Keep Screen) - Lebar 115px
b5_x = b4_x + b4_w + 14
b5_w = 115
draw.rounded_rectangle((b5_x, btn_y, b5_x + b5_w, btn_y + btn_h), radius=btn_rad, fill=RAISED_BG, outline=GOLD, width=2)
draw_bulb_icon(draw, b5_x + b5_w // 2, btn_y + btn_h // 2, 28, GOLD)

# Android System Nav Indicator
nav_pill_y = 2295
draw.rounded_rectangle((WIDTH // 2 - 120, nav_pill_y, WIDTH // 2 + 120, nav_pill_y + 8), radius=4, fill=(180, 190, 205, 180))

out_path = "C:/KordApp-AGY/branding/preview_player_floating_dock.png"
im.save(out_path, "PNG")
print(f"Generated floating dock preview: {out_path}")
