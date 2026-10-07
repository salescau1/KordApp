import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

WIDTH = 1080
HEIGHT = 2340

# Colors
BG_COLOR = (14, 17, 23)           # #0E1117 Stage Dark
CARD_BG = (22, 27, 34)            # #161B22
BORDER_FROST = (70, 85, 115, 220) # Bright crisp glass border
GOLD = (229, 169, 60)             # #E5A93C Brand Gold
GOLD_BG = (42, 33, 12)            # #2A210C
CHORD_COLOR = (255, 183, 77)      # #FFB74D High-visibility Warm Amber
TEXT_PRIMARY = (245, 248, 255)    # #F5F8FF
TEXT_MUTED = (142, 154, 168)      # #8E9AA8
HEART_ACTIVE = (255, 77, 109)     # #FF4D6D Active Heart

FONT_DIR = "C:/Windows/Fonts"
font_title = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 32)
font_ver = ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 22)
font_subtitle = ImageFont.truetype(f"{FONT_DIR}/segoeui.ttf", 22)
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

# 1. BASE CANVAS
im = Image.new("RGBA", (WIDTH, HEIGHT), BG_COLOR)
draw = ImageDraw.Draw(im)

# Status bar
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

# Draw sheet lyrics passing all the way from Y=90 to Y=2300 (directly behind top and bottom bars!)
y_sheet = 95
all_lines = [
    ("      F             Bb", "[Intro Song Verse Behind Top Bar]"),
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
    ("Bb            Gm", "Mama, just killed a man"),
    ("      Cm", "Put a gun against his head"),
    ("                    F", "Pulled my trigger, now he's dead"),
    ("Bb            Gm", "Mama, life had just begun"),
    ("      Cm", "But now I've gone and thrown it all away"),
    ("Eb          Ab   Eb   F", "Mama, ooh, didn't mean to make you cry"),
    ("                    Bb", "If I'm not back again this time tomorrow"),
    ("      Eb", "Carry on, carry on as if nothing really matters"),
    ("Fm          C", "Too late, my time has come"),
    ("      G                 C", "Sends shivers down my spine, body's aching all the time"),
    ("C           Am", "Goodbye, everybody, I've got to go"),
    ("      Dm              G", "Gotta leave you all behind and face the truth"),
]

for chord, lyric in all_lines:
    draw.text((60, y_sheet), chord, fill=CHORD_COLOR, font=font_chord)
    y_sheet += 42
    draw.text((60, y_sheet), lyric, fill=TEXT_PRIMARY, font=font_lyric)
    y_sheet += 56

# =========================================================================
# 2. GRADIENT FADE SCRIM (TOP & BOTTOM) - Native Blur & Dissolve Zone
# =========================================================================
# Top scrim: Y=0..260 fades from Solid Background to Transparent
scrim_top = Image.new("RGBA", (WIDTH, 260), (0, 0, 0, 0))
st_draw = ImageDraw.Draw(scrim_top)
for y in range(260):
    # Alpha 255 at top (y=0) to 0 at bottom (y=260)
    alpha = int(255 * (1.0 - (y / 260.0) ** 1.5))
    st_draw.line((0, y, WIDTH, y), fill=(14, 17, 23, alpha))
im = Image.alpha_composite(im, Image.new("RGBA", (WIDTH, HEIGHT), (0,0,0,0)))
im.paste(scrim_top, (0, 0), scrim_top)

# Bottom scrim: Y=2050..2340 fades from Transparent to Solid Background
scrim_bot = Image.new("RGBA", (WIDTH, 290), (0, 0, 0, 0))
sb_draw = ImageDraw.Draw(scrim_bot)
for y in range(290):
    alpha = int(255 * ((y / 290.0) ** 1.3))
    sb_draw.line((0, y, WIDTH, y), fill=(14, 17, 23, alpha))
im.paste(scrim_bot, (0, 2050), scrim_bot)

# Gaussian blur the composite to simulate real frosted glass backdrop behind the buttons
blurred_canvas = im.filter(ImageFilter.GaussianBlur(radius=28))

# =========================================================================
# 3. TOP SEPARATED FLOATING BUTTONS WITH CLEAR FROSTED GLASS
# =========================================================================
top_y = 95
btn_size = 86
cy = top_y + btn_size // 2

def draw_frosted_circle(center_x, center_y, radius, border_color=BORDER_FROST, bg_tint=(22, 27, 34, 160)):
    global im
    crop_box = (center_x - radius, center_y - radius, center_x + radius, center_y + radius)
    crop = blurred_canvas.crop(crop_box)
    mask = Image.new("L", (radius * 2, radius * 2), 0)
    mdraw = ImageDraw.Draw(mask)
    mdraw.ellipse((0, 0, radius * 2, radius * 2), fill=255)
    im.paste(crop, (center_x - radius, center_y - radius), mask)
    
    # Translucent glass tint + border
    overlay = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    odraw = ImageDraw.Draw(overlay)
    odraw.ellipse((center_x - radius, center_y - radius, center_x + radius, center_y + radius), fill=bg_tint, outline=border_color, width=2)
    im = Image.alpha_composite(im, overlay)

# A. BACK BUTTON BULET (<)
back_cx = 40 + btn_size // 2
draw_frosted_circle(back_cx, cy, btn_size // 2)
draw = ImageDraw.Draw(im)
draw.line((back_cx - 12, cy, back_cx + 12, cy), fill=TEXT_PRIMARY, width=4)
draw.line((back_cx - 12, cy, back_cx, cy - 12), fill=TEXT_PRIMARY, width=4)
draw.line((back_cx - 12, cy, back_cx, cy + 12), fill=TEXT_PRIMARY, width=4)

# B. SETLIST BUTTON BULET (➕)
setlist_cx = WIDTH - 40 - btn_size // 2
draw_frosted_circle(setlist_cx, cy, btn_size // 2, border_color=GOLD, bg_tint=(22, 27, 34, 160))
draw = ImageDraw.Draw(im)
draw.line((setlist_cx - 12, cy - 8, setlist_cx + 12, cy - 8), fill=GOLD, width=3)
draw.line((setlist_cx - 12, cy, setlist_cx + 8, cy), fill=GOLD, width=3)
draw.line((setlist_cx - 12, cy + 8, setlist_cx + 4, cy + 8), fill=GOLD, width=3)
draw.line((setlist_cx + 10, cy + 2, setlist_cx + 10, cy + 14), fill=GOLD, width=3)
draw.line((setlist_cx + 4, cy + 8, setlist_cx + 16, cy + 8), fill=GOLD, width=3)

# C. LOVE BUTTON BULET (❤️)
love_cx = setlist_cx - btn_size - 18
draw_frosted_circle(love_cx, cy, btn_size // 2)
draw = ImageDraw.Draw(im)
draw.polygon([(love_cx, cy-8), (love_cx-12, cy-20), (love_cx-22, cy-12), (love_cx-22, cy+2), (love_cx, cy+22), 
              (love_cx+22, cy+2), (love_cx+22, cy-12), (love_cx+12, cy-20)], fill=HEART_ACTIVE)
draw.ellipse((love_cx-22, cy-20, love_cx, cy), fill=HEART_ACTIVE)
draw.ellipse((love_cx, cy-20, love_cx+22, cy), fill=HEART_ACTIVE)

# D. KAPSUL TITLE TENGAH
title_left = back_cx + btn_size // 2 + 18
title_right = love_cx - btn_size // 2 - 18
title_h = btn_size
title_rad = title_h // 2

t_crop = blurred_canvas.crop((title_left, top_y, title_right, top_y + title_h))
t_mask = Image.new("L", (title_right - title_left, title_h), 0)
t_mdraw = ImageDraw.Draw(t_mask)
t_mdraw.rounded_rectangle((0, 0, title_right - title_left, title_h), radius=title_rad, fill=255)
im.paste(t_crop, (title_left, top_y), t_mask)

t_overlay = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
t_odraw = ImageDraw.Draw(t_overlay)
t_odraw.rounded_rectangle((title_left, top_y, title_right, top_y + title_h), radius=title_rad, fill=(22, 27, 34, 160), outline=BORDER_FROST, width=2)
im = Image.alpha_composite(im, t_overlay)
draw = ImageDraw.Draw(im)

title_text = "Bohemian Rhapsody"
ver_text = "UG v1"
t_bbox = draw.textbbox((0, 0), title_text, font=font_title)
v_bbox = draw.textbbox((0, 0), ver_text, font=font_ver)
t_w = t_bbox[2] - t_bbox[0]
v_w = v_bbox[2] - v_bbox[0]
pill_w = v_w + 28
total_w = t_w + 14 + pill_w

tx_start = (title_left + title_right - total_w) // 2
draw.text((tx_start, top_y + 12), title_text, fill=TEXT_PRIMARY, font=font_title)

pill_x = tx_start + t_w + 12
draw.rounded_rectangle((pill_x, top_y + 14, pill_x + pill_w, top_y + 44), radius=8, fill=GOLD_BG, outline=GOLD, width=1)
draw.text((pill_x + 8, top_y + 17), ver_text, fill=GOLD, font=font_ver)
draw_down_triangle(draw, pill_x + pill_w - 9, top_y + 30, 4, GOLD)

sub_text = "Queen • Key: Bb"
s_bbox = draw.textbbox((0, 0), sub_text, font=font_subtitle)
s_w = s_bbox[2] - s_bbox[0]
draw.text(((title_left + title_right - s_w) // 2, top_y + 50), sub_text, fill=TEXT_MUTED, font=font_subtitle)


# =========================================================================
# 4. BOTTOM FLOATING CAPSULE DOCK WITH VISIBLE FROSTED GLASS
# =========================================================================
dock_left = 40
dock_right = WIDTH - 40
dock_top = 2125
dock_bottom = 2245
dock_radius = 40

bot_crop = blurred_canvas.crop((dock_left, dock_top, dock_right, dock_bottom))
bot_mask = Image.new("L", (dock_right - dock_left, dock_bottom - dock_top), 0)
bot_mask_draw = ImageDraw.Draw(bot_mask)
bot_mask_draw.rounded_rectangle((0, 0, dock_right - dock_left, dock_bottom - dock_top), radius=dock_radius, fill=255)
im.paste(bot_crop, (dock_left, dock_top), bot_mask)

glass_bot = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
gb_draw = ImageDraw.Draw(glass_bot)
gb_draw.rounded_rectangle((dock_left, dock_top, dock_right, dock_bottom), radius=dock_radius, fill=(20, 25, 34, 160), outline=BORDER_FROST, width=2)
im = Image.alpha_composite(im, glass_bot)
draw = ImageDraw.Draw(im)

btn_y = dock_top + 15
btn_h = 90
btn_rad = 22

# 1. SCROLL
b1_x = dock_left + 16
b1_w = 115
draw.rounded_rectangle((b1_x, btn_y, b1_x + b1_w, btn_y + btn_h), radius=btn_rad, fill=GOLD)
draw_play_icon(draw, b1_x + b1_w // 2, btn_y + btn_h // 2, 28, BG_COLOR)

# 2. FLIP
b2_x = b1_x + b1_w + 14
b2_w = 115
draw.rounded_rectangle((b2_x, btn_y, b2_x + b2_w, btn_y + btn_h), radius=btn_rad, fill=(30, 38, 52, 200), outline=BORDER_FROST, width=1)
draw_book_icon(draw, b2_x + b2_w // 2, btn_y + btn_h // 2, 16, 26, TEXT_PRIMARY)

# 3. TRANSPOSE [ − | 0 | + ]
b3_x = b2_x + b2_w + 16
b3_w = 280
draw.rounded_rectangle((b3_x, btn_y, b3_x + b3_w, btn_y + btn_h), radius=btn_rad, fill=(30, 38, 52, 200), outline=BORDER_FROST, width=1)
draw.text((b3_x + 30, btn_y + 24), "−", fill=TEXT_PRIMARY, font=font_btn_symbol)
draw.line((b3_x + 90, btn_y + 8, b3_x + 90, btn_y + btn_h - 8), fill=BORDER_FROST, width=1)
draw.text((b3_x + 130, btn_y + 28), "0", fill=GOLD, font=font_btn_val)
draw.line((b3_x + 190, btn_y + 8, b3_x + 190, btn_y + btn_h - 8), fill=BORDER_FROST, width=1)
draw.text((b3_x + 230, btn_y + 24), "+", fill=TEXT_PRIMARY, font=font_btn_symbol)

# 4. SIZE [ a | 0 | A ]
b4_x = b3_x + b3_w + 16
b4_w = 280
draw.rounded_rectangle((b4_x, btn_y, b4_x + b4_w, btn_y + btn_h), radius=btn_rad, fill=(30, 38, 52, 200), outline=BORDER_FROST, width=1)
draw.text((b4_x + 35, btn_y + 28), "a", fill=TEXT_PRIMARY, font=ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 26))
draw.line((b4_x + 90, btn_y + 8, b4_x + 90, btn_y + btn_h - 8), fill=BORDER_FROST, width=1)
draw.text((b4_x + 130, btn_y + 28), "0", fill=GOLD, font=font_btn_val)
draw.line((b4_x + 190, btn_y + 8, b4_x + 190, btn_y + btn_h - 8), fill=BORDER_FROST, width=1)
draw.text((b4_x + 228, btn_y + 25), "A", fill=TEXT_PRIMARY, font=ImageFont.truetype(f"{FONT_DIR}/segoeuib.ttf", 30))

# 5. LAMPU
b5_x = b4_x + b4_w + 14
b5_w = 115
draw.rounded_rectangle((b5_x, btn_y, b5_x + b5_w, btn_y + btn_h), radius=btn_rad, fill=(30, 38, 52, 200), outline=GOLD, width=2)
draw_bulb_icon(draw, b5_x + b5_w // 2, btn_y + btn_h // 2, 28, GOLD)

# Android Nav Pill
nav_pill_y = 2295
draw.rounded_rectangle((WIDTH // 2 - 120, nav_pill_y, WIDTH // 2 + 120, nav_pill_y + 8), radius=4, fill=(180, 190, 205, 180))

out_path = "C:/KordApp-AGY/branding/preview_player_floating_dock.png"
im.save(out_path, "PNG")
print(f"Generated clear frosted glass blur preview: {out_path}")
