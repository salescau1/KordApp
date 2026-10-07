import os
import textwrap
from PIL import Image, ImageDraw, ImageFont

WIDTH = 1080
HEIGHT = 2340

# Colors
BG_COLOR = (14, 17, 23)           # #0E1117 Stage Dark
CARD_BG = (22, 27, 34)            # #161B22
RAISED_BG = (26, 32, 44)          # #1A202C
BORDER_COLOR = (36, 44, 61)       # #242C3D
BORDER_HIGHLIGHT = (56, 68, 94)
GOLD = (229, 169, 60)             # #E5A93C Brand Gold
GOLD_BG = (42, 33, 12)            # #2A210C
CHORD_COLOR = (255, 183, 77)      # #FFB74D High-visibility Warm Amber Chord
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
font_button_lg = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 26)
font_button_md = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 24)
font_button_sm = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 20)
font_diagram_title = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 34)
font_diagram_sub = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 22)
font_diagram_fret = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 20)

def draw_play_icon(draw, cx, cy, size, color):
    points = [(cx - size // 2, cy - size // 2), (cx - size // 2, cy + size // 2), (cx + size // 2 + 2, cy)]
    draw.polygon(points, fill=color)

def draw_down_triangle(draw, cx, cy, size, color):
    points = [(cx - size, cy - size // 2), (cx + size, cy - size // 2), (cx, cy + size // 2 + 1)]
    draw.polygon(points, fill=color)

def draw_left_arrow(draw, cx, cy, size, color):
    draw.line([(cx + size // 2, cy - size), (cx - size // 2, cy)], fill=color, width=3)
    draw.line([(cx - size // 2, cy), (cx + size // 2, cy + size)], fill=color, width=3)

def draw_right_arrow(draw, cx, cy, size, color):
    draw.line([(cx - size // 2, cy - size), (cx + size // 2, cy)], fill=color, width=3)
    draw.line([(cx + size // 2, cy), (cx - size // 2, cy + size)], fill=color, width=3)

def draw_lightning(draw, cx, cy, size, color):
    pts = [
        (cx + 2, cy - size),
        (cx - size // 2, cy + 1),
        (cx, cy + 1),
        (cx - 2, cy + size),
        (cx + size // 2, cy - 1),
        (cx, cy - 1)
    ]
    draw.polygon(pts, fill=color)

def draw_doc_icon(draw, cx, cy, w, h, color):
    draw.rounded_rectangle((cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2), radius=3, outline=color, width=2)
    draw.line((cx - w // 2 + 4, cy - 3, cx + w // 2 - 4, cy - 3), fill=color, width=2)
    draw.line((cx - w // 2 + 4, cy + 3, cx + w // 2 - 4, cy + 3), fill=color, width=2)

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

# Favorite Heart
hx, hy = 890, 157
draw.polygon([(hx, hy-8), (hx-14, hy-22), (hx-26, hy-14), (hx-26, hy+2), (hx, hy+26), 
              (hx+26, hy+2), (hx+26, hy-14), (hx+14, hy-22)], fill=HEART_ACTIVE)
draw.ellipse((hx-26, hy-22, hx, hy), fill=HEART_ACTIVE)
draw.ellipse((hx, hy-22, hx+26, hy), fill=HEART_ACTIVE)

# Add to Setlist Gold Circle
sx, sy = 990, 157
draw.ellipse((sx-28, sy-28, sx+28, sy+28), fill=GOLD)
draw.line((sx-12, sy-10, sx+12, sy-10), fill=BG_COLOR, width=3)
draw.line((sx-12, sy-2, sx+8, sy-2), fill=BG_COLOR, width=3)
draw.line((sx-12, sy+6, sx+4, sy+6), fill=BG_COLOR, width=3)
draw.line((sx+10, sy+2, sx+10, sy+14), fill=BG_COLOR, width=3)
draw.line((sx+4, sy+8, sx+16, sy+8), fill=BG_COLOR, width=3)

# 3. METADATA STRIP (CAPO REMOVED - CLEAN & SIMPLE)
draw.rounded_rectangle((40, 235, 165, 272), radius=6, fill=GOLD_BG, outline=GOLD, width=1)
draw.text((56, 239), "Key: Bb", fill=GOLD, font=font_meta)

draw.text((195, 241), "Beginner", fill=TEXT_MUTED, font=font_subtitle)

draw.rounded_rectangle((920, 235, 1040, 272), radius=8, fill=CARD_BG, outline=BORDER_COLOR, width=1)
draw.text((936, 239), "v1/2", fill=TEXT_PRIMARY, font=font_meta)
draw_down_triangle(draw, 1015, 256, 5, TEXT_PRIMARY)

# 4. CHORD SHEET
y_sheet = 320
draw.text((40, y_sheet), "[Intro 1]", fill=GOLD, font=font_section)
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
    draw.text((40, y_sheet), chord, fill=CHORD_COLOR, font=font_chord)
    y_sheet += 42
    draw.text((40, y_sheet), lyric, fill=TEXT_PRIMARY, font=font_lyric)
    y_sheet += 54

y_sheet += 25
draw.text((40, y_sheet), "[Verse 1]", fill=GOLD, font=font_section)
y_sheet += 60
verse_lines = [
    ("Bb            Gm", "Mama, just killed a man"),
    ("      Cm", "Put a gun against his head"),
    ("                    F", "Pulled my trigger, now he's dead"),
]
for chord, lyric in verse_lines:
    draw.text((40, y_sheet), chord, fill=CHORD_COLOR, font=font_chord)
    y_sheet += 42
    draw.text((40, y_sheet), lyric, fill=TEXT_PRIMARY, font=font_lyric)
    y_sheet += 54

# 5. CHORD DIAGRAM CARD
diag_left = 630
diag_top = 310
diag_right = 1040
diag_bottom = 830

shadow = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
sdraw = ImageDraw.Draw(shadow)
sdraw.rounded_rectangle((diag_left-6, diag_top-4, diag_right+6, diag_bottom+8), radius=22, fill=(0, 0, 0, 160))
im = Image.alpha_composite(im, shadow)
draw = ImageDraw.Draw(im)

draw.rounded_rectangle((diag_left, diag_top, diag_right, diag_bottom), radius=20, fill=CARD_BG, outline=GOLD, width=2)
draw.text((diag_left + 24, diag_top + 20), "Bb", fill=GOLD, font=font_diagram_title)
draw.text((diag_left + 80, diag_top + 28), "Major", fill=TEXT_MUTED, font=font_diagram_sub)

draw.rounded_rectangle((diag_left + 180, diag_top + 18, diag_left + 300, diag_top + 58), radius=8, fill=RAISED_BG, outline=BORDER_COLOR, width=1)
draw_left_arrow(draw, diag_left + 200, diag_top + 38, 6, TEXT_PRIMARY)
draw.text((diag_left + 220, diag_top + 24), "1 / 3", fill=TEXT_PRIMARY, font=font_diagram_fret)
draw_right_arrow(draw, diag_left + 280, diag_top + 38, 6, TEXT_PRIMARY)

cx_btn = diag_right - 40
cy_btn = diag_top + 38
draw.line((cx_btn-10, cy_btn-10, cx_btn+10, cy_btn+10), fill=TEXT_MUTED, width=3)
draw.line((cx_btn-10, cy_btn+10, cx_btn+10, cy_btn-10), fill=TEXT_MUTED, width=3)

f_left = diag_left + 50
f_top = diag_top + 105
f_width = 310
f_height = 330
f_right = f_left + f_width
f_bottom = f_top + f_height

draw.text((f_left - 36, f_top + 35), "1fr", fill=GOLD, font=font_diagram_fret)
str_spacing = f_width / 5
for s in range(6):
    sx = int(f_left + s * str_spacing)
    s_col = (180, 195, 215) if s >= 3 else (220, 230, 245)
    draw.line((sx, f_top, sx, f_bottom), fill=s_col, width=2 if s < 3 else 3)

fret_spacing = f_height / 4
draw.line((f_left, f_top, f_right, f_top), fill=(245, 248, 255), width=6)
for f in range(1, 5):
    fy = int(f_top + f * fret_spacing)
    draw.line((f_left, fy, f_right, fy), fill=BORDER_HIGHLIGHT, width=2)

barre_y = int(f_top + fret_spacing * 0.5)
draw.rounded_rectangle((f_left - 8, barre_y - 14, f_right + 8, barre_y + 14), radius=12, fill=GOLD)
draw.text((f_left + f_width // 2 - 6, barre_y - 12), "1", fill=BG_COLOR, font=font_diagram_fret)

draw.line((f_left - 8, f_top - 28, f_left + 4, f_top - 16), fill=(239, 68, 68), width=3)
draw.line((f_left - 8, f_top - 16, f_left + 4, f_top - 28), fill=(239, 68, 68), width=3)

dot_f3_y = int(f_top + fret_spacing * 2.5)
for s_idx, finger in [(2, "2"), (3, "3"), (4, "4")]:
    dot_x = int(f_left + s_idx * str_spacing)
    draw.ellipse((dot_x - 16, dot_f3_y - 16, dot_x + 16, dot_f3_y + 16), fill=(255, 215, 0))
    draw.ellipse((dot_x - 16, dot_f3_y - 16, dot_x + 16, dot_f3_y + 16), outline=(40, 30, 10), width=2)
    draw.text((dot_x - 6, dot_f3_y - 12), finger, fill=BG_COLOR, font=font_diagram_fret)

s_names = ["E", "A", "D", "G", "B", "e"]
for idx, name in enumerate(s_names):
    sx = int(f_left + idx * str_spacing) - 6
    draw.text((sx, f_bottom + 12), name, fill=TEXT_MUTED, font=font_diagram_fret)

draw.text((diag_left + 70, diag_bottom - 44), "Sentuh akor untuk posisi fret", fill=TEXT_MUTED, font=ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 18))

# 6. AUTO-SCROLL RAIL
rail_w = 46
rail_h = 560
rail_x = WIDTH - rail_w - 14
rail_y = 960

draw.rounded_rectangle((rail_x, rail_y, rail_x + rail_w, rail_y + rail_h), radius=22, fill=(22, 27, 34, 210), outline=BORDER_COLOR, width=2)
track_x = rail_x + rail_w // 2
draw.line((track_x, rail_y + 35, track_x, rail_y + rail_h - 35), fill=BORDER_HIGHLIGHT, width=4)
thumb_y = rail_y + int(rail_h * 0.42)
draw.line((track_x, thumb_y, track_x, rail_y + rail_h - 35), fill=GOLD, width=4)
draw.ellipse((track_x - 18, thumb_y - 18, track_x + 18, thumb_y + 18), fill=GOLD)
draw.ellipse((track_x - 10, thumb_y - 10, track_x + 10, thumb_y + 10), fill=(255, 255, 255))

speed_tag_w = 125
speed_tag_h = 46
st_x = rail_x - speed_tag_w - 14
st_y = thumb_y - speed_tag_h // 2
draw.rounded_rectangle((st_x, st_y, st_x + speed_tag_w, st_y + speed_tag_h), radius=12, fill=CARD_BG, outline=GOLD, width=2)
draw_lightning(draw, st_x + 22, st_y + 23, 10, GOLD)
draw.text((st_x + 36, st_y + 9), "1.5x", fill=GOLD, font=font_button_md)

# 7. TOOLBAR WITHOUT CAPO (LEBIH LEGA, BERSIH & ERGONOMIS)
tb_top = 1880
draw.rectangle((0, tb_top, WIDTH, HEIGHT), fill=CARD_BG)
draw.line((0, tb_top, WIDTH, tb_top), fill=BORDER_COLOR, width=2)

# TIER 1: TRANSPOSE (LEGA) + FONT SIZE + RESET
t_x = 40
t_y = 1910
draw.text((t_x + 110, t_y - 2), "transpose nada", fill=TEXT_MUTED, font=font_button_sm)

# Transpose group with wide comfortable targets
draw.rounded_rectangle((t_x, t_y + 24, t_x + 320, t_y + 78), radius=10, fill=RAISED_BG, outline=BORDER_COLOR, width=1)
draw.text((t_x + 36, t_y + 32), "−", fill=TEXT_PRIMARY, font=font_button_lg)
draw.line((t_x + 100, t_y + 24, t_x + 100, t_y + 78), fill=BORDER_COLOR, width=1)
draw.text((t_x + 140, t_y + 36), "0", fill=GOLD, font=font_button_md)
draw.line((t_x + 220, t_y + 24, t_x + 220, t_y + 78), fill=BORDER_COLOR, width=1)
draw.text((t_x + 260, t_y + 32), "+", fill=TEXT_PRIMARY, font=font_button_lg)

# Font size group
f_x = 410
draw.text((f_x + 45, t_y - 2), "ukuran font", fill=TEXT_MUTED, font=font_button_sm)
draw.rounded_rectangle((f_x, t_y + 24, f_x + 120, t_y + 78), radius=10, fill=RAISED_BG, outline=BORDER_COLOR, width=1)
draw.text((f_x + 42, t_y + 34), "A−", fill=TEXT_PRIMARY, font=font_button_md)

draw.rounded_rectangle((f_x + 140, t_y + 24, f_x + 260, t_y + 78), radius=10, fill=RAISED_BG, outline=BORDER_COLOR, width=1)
draw.text((f_x + 180, t_y + 34), "A+", fill=TEXT_PRIMARY, font=font_button_md)

# Reset Button
r_x = 720
draw.rounded_rectangle((r_x, t_y + 24, WIDTH - 40, t_y + 78), radius=10, fill=RAISED_BG, outline=BORDER_COLOR, width=1)
draw.text((r_x + 85, t_y + 36), "Reset Transpose", fill=TEXT_MUTED, font=font_button_sm)

# TIER 2: STAGE CONTROLS
b_y = 2025
b_h = 76

as_w = 330
draw.rounded_rectangle((40, b_y, 40 + as_w, b_y + b_h), radius=14, fill=GOLD)
draw_play_icon(draw, 72, b_y + b_h // 2, 16, BG_COLOR)
draw.text((94, b_y + 20), "Auto Scroll  ON", fill=BG_COLOR, font=font_button_lg)

pf_w = 320
draw.rounded_rectangle((395, b_y, 395 + pf_w, b_y + b_h), radius=14, fill=RAISED_BG, outline=BORDER_COLOR, width=2)
draw_doc_icon(draw, 435, b_y + b_h // 2, 16, 20, TEXT_PRIMARY)
draw.text((460, b_y + 22), "Page Flip", fill=TEXT_PRIMARY, font=font_button_lg)

ks_w = 320
draw.rounded_rectangle((740, b_y, WIDTH - 40, b_y + b_h), radius=14, fill=RAISED_BG, outline=GOLD, width=2)
draw.text((780, b_y + 12), "Keep Screen", fill=TEXT_MUTED, font=font_button_sm)
draw.text((825, b_y + 36), "AKTIF", fill=GOLD, font=font_button_lg)

nav_pill_y = 2270
draw.rounded_rectangle((WIDTH // 2 - 120, nav_pill_y, WIDTH // 2 + 120, nav_pill_y + 8), radius=4, fill=(180, 190, 205, 180))

out_path = "C:/KordApp-AGY/branding/preview_player_proposal.png"
im.save(out_path, "PNG")
print(f"Generated clean preview without capo: {out_path}")


