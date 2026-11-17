#!/usr/bin/env python3
"""
Draws the game's card deck, card back, table, icons and UI frames in one warm
palette (espresso, cream, oxblood, gold), at high resolution.

The number cards are drawn entirely from vector shapes. The jacks, queens and
kings reuse the face illustrations from the original card sheet
(tools/source/cards.png), enlarged and set into a new frame.

Usage: python3 tools/make_art.py [output dir, default assets/images]
Needs Pillow, NumPy and the DejaVu fonts.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
SS = 4  # supersampling for smooth edges

# palette
CREAM = (243, 232, 210)
CREAM_EDGE = (226, 210, 180)
PAPER_LINE = (184, 160, 122)
RED = (150, 28, 31)
INK = (30, 27, 24)
OXBLOOD = (104, 18, 20)
OXBLOOD_DARK = (64, 10, 12)
GOLD = (210, 168, 82)
GOLD_LIGHT = (240, 206, 128)
GOLD_DARK = (140, 104, 44)
ESPRESSO = (24, 15, 10)

CARD_W, CARD_H = 219, 294  # 3x the original 73x98
RANKS = ["A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"]  # sheet column order
SUITS = ["clubs", "spades", "hearts", "diamonds"]  # sheet row order
SERIF_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"


def downsample(img, w, h):
    return img.resize((w, h), Image.LANCZOS)


# ---------------------------------------------------------------- suit shapes

def suit_shape(draw, suit, cx, cy, size, fill):
    """Draws a suit symbol of height `size` centred on (cx, cy)."""
    s = size

    def P(x, y):
        return (cx + x * s, cy + y * s)

    def circle(x, y, r):
        draw.ellipse([cx + (x - r) * s, cy + (y - r) * s, cx + (x + r) * s, cy + (y + r) * s], fill=fill)

    if suit == "diamonds":
        draw.polygon([P(0, -0.5), P(0.36, 0), P(0, 0.5), P(-0.36, 0)], fill=fill)
    elif suit == "hearts":
        circle(-0.215, -0.2, 0.235)
        circle(0.215, -0.2, 0.235)
        draw.polygon([P(-0.445, -0.14), P(0.445, -0.14), P(0, 0.5)], fill=fill)
        draw.polygon([P(-0.2, -0.3), P(0.2, -0.3), P(0, 0)], fill=fill)
    elif suit == "spades":
        circle(-0.215, 0.08, 0.235)
        circle(0.215, 0.08, 0.235)
        draw.polygon([P(-0.445, 0.02), P(0.445, 0.02), P(0, -0.5)], fill=fill)
        draw.polygon([P(-0.2, 0.18), P(0.2, 0.18), P(0, -0.1)], fill=fill)
        draw.polygon([P(0, 0.12), P(-0.17, 0.5), P(0.17, 0.5)], fill=fill)
    elif suit == "clubs":
        circle(0, -0.24, 0.205)
        circle(-0.235, 0.07, 0.205)
        circle(0.235, 0.07, 0.205)
        circle(0, 0.0, 0.12)
        draw.polygon([P(0, 0.0), P(-0.16, 0.5), P(0.16, 0.5)], fill=fill)


def suit_colour(suit):
    return RED if suit in ("hearts", "diamonds") else INK


# ---------------------------------------------------------------- cards

L, M, R = 0.0, 0.5, 1.0
PIPS = {
    "2": [(M, 0), (M, 1)],
    "3": [(M, 0), (M, 0.5), (M, 1)],
    "4": [(L, 0), (R, 0), (L, 1), (R, 1)],
    "5": [(L, 0), (R, 0), (M, 0.5), (L, 1), (R, 1)],
    "6": [(L, 0), (R, 0), (L, 0.5), (R, 0.5), (L, 1), (R, 1)],
    "7": [(L, 0), (R, 0), (M, 0.25), (L, 0.5), (R, 0.5), (L, 1), (R, 1)],
    "8": [(L, 0), (R, 0), (M, 0.25), (L, 0.5), (R, 0.5), (M, 0.75), (L, 1), (R, 1)],
    "9": [(L, 0), (R, 0), (L, 1 / 3), (R, 1 / 3), (M, 0.5), (L, 2 / 3), (R, 2 / 3), (L, 1), (R, 1)],
    "10": [(L, 0), (R, 0), (M, 1 / 6), (L, 1 / 3), (R, 1 / 3), (L, 2 / 3), (R, 2 / 3), (M, 5 / 6), (L, 1), (R, 1)],
}


def paper(w, h, seed):
    """Cream paper: a soft top-to-bottom gradient with faint fibre noise."""
    rng = np.random.default_rng(seed)
    y = np.linspace(0, 1, h)[:, None]
    base = np.array(CREAM, float)[None, None, :] * (1.02 - 0.05 * y[..., None])
    noise = rng.normal(0, 3.0, (h // 4 + 1, w // 4 + 1))
    noise = np.array(Image.fromarray(noise.astype(np.float32), mode="F").resize((w, h), Image.BICUBIC))
    img = np.clip(base + noise[..., None], 0, 255).astype(np.uint8)
    return Image.fromarray(img, "RGB").convert("RGBA")


def rounded_mask(w, h, r):
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, w - 1, h - 1], r, fill=255)
    return m


def card_base(seed):
    W, H = CARD_W * SS, CARD_H * SS
    img = paper(W, H, seed)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([SS, SS, W - 1 - SS, H - 1 - SS], 15 * SS, outline=PAPER_LINE, width=2 * SS)
    img.putalpha(rounded_mask(W, H, 16 * SS))
    return img


def corner_index(img, rank, suit):
    """Rank and small suit in the top-left corner, and the same turned round in the bottom-right."""
    W, H = img.size
    colour = suit_colour(suit)
    font = ImageFont.truetype(SERIF_BOLD, (34 if rank == "10" else 40) * SS)
    layer = Image.new("RGBA", (40 * SS, 90 * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.text((20 * SS, 4 * SS), rank, font=font, fill=colour, anchor="mt")
    suit_shape(d, suit, 20 * SS, 62 * SS, 26 * SS, colour)
    img.alpha_composite(layer, (8 * SS, 8 * SS))
    img.alpha_composite(layer.rotate(180), (W - 48 * SS, H - 98 * SS))


def number_card(rank, suit, seed):
    img = card_base(seed)
    d = ImageDraw.Draw(img)
    colour = suit_colour(suit)
    W, H = img.size
    if rank == "A":
        suit_shape(d, suit, W / 2, H / 2, 96 * SS, colour)
    else:
        x0, x1 = 66 * SS, (CARD_W - 66) * SS
        y0, y1 = 52 * SS, (CARD_H - 52) * SS
        size = 40 * SS
        for fx, fy in PIPS[rank]:
            cx, cy = x0 + fx * (x1 - x0), y0 + fy * (y1 - y0)
            if fy > 0.5:  # lower pips point the other way, as on real cards
                pip = Image.new("RGBA", (size * 2, size * 2), (0, 0, 0, 0))
                suit_shape(ImageDraw.Draw(pip), suit, size, size, size, colour)
                pip = pip.rotate(180)
                img.alpha_composite(pip, (int(cx - size), int(cy - size)))
            else:
                suit_shape(d, suit, cx, cy, size, colour)
    corner_index(img, rank, suit)
    return downsample(img, CARD_W, CARD_H)


def face_card(rank, suit, seed, source):
    """The original face illustration, enlarged and set into a gold-and-oxblood frame."""
    col = RANKS.index(rank)
    row = SUITS.index(suit)
    cell = source.crop((col * 73, row * 98, col * 73 + 73, row * 98 + 98)).convert("RGBA")
    art = cell.crop((14, 12, 59, 86))  # inside the original black frame
    aw, ah = 116, 191  # clear of the corner indices
    art = art.resize((aw * SS, ah * SS), Image.LANCZOS).filter(ImageFilter.UnsharpMask(radius=6, percent=60, threshold=2))
    # warm the colours slightly towards the cream paper so they sit in the palette
    a = np.asarray(art).astype(float)
    warm = np.array([255, 238, 205], float)
    a[..., :3] = a[..., :3] * 0.85 + (a[..., :3] * warm / 255.0) * 0.15
    art = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")

    img = card_base(seed)
    W, H = img.size
    x, y = (W - aw * SS) // 2, (H - ah * SS) // 2
    img.alpha_composite(art, (x, y))
    d = ImageDraw.Draw(img)
    d.rectangle([x - 3 * SS, y - 3 * SS, x + aw * SS + 3 * SS, y + ah * SS + 3 * SS], outline=GOLD_DARK, width=2 * SS)
    d.rectangle([x - SS, y - SS, x + aw * SS + SS, y + ah * SS + SS], outline=OXBLOOD, width=SS)
    corner_index(img, rank, suit)
    return downsample(img, CARD_W, CARD_H)


def card_sheet(out):
    source = Image.open(os.path.join(HERE, "source", "cards.png")).convert("RGBA")
    sheet = Image.new("RGBA", (CARD_W * 13, CARD_H * 4), (0, 0, 0, 0))
    for r, suit in enumerate(SUITS):
        for c, rank in enumerate(RANKS):
            seed = r * 13 + c
            card = face_card(rank, suit, seed, source) if rank in "JQK" else number_card(rank, suit, seed)
            sheet.paste(card, (c * CARD_W, r * CARD_H))
    # 256 colours with dithering: looks the same, a tenth of the file size
    sheet.convert("RGBA").quantize(256, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.FLOYDSTEINBERG).save(
        os.path.join(out, "cards_hd.png"), optimize=True)
    return sheet


def card_back(out):
    W, H = CARD_W * SS, CARD_H * SS
    y = np.linspace(0, 1, H)[:, None, None]
    x = np.linspace(0, 1, W)[None, :, None]
    r = np.sqrt((x - 0.5) ** 2 + (y - 0.5) ** 2)
    base = np.array(OXBLOOD, float) * (1.15 - 0.6 * r) + np.array(OXBLOOD_DARK, float) * 0.0
    img = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(img)
    inset = 14 * SS
    # diamond lattice inside the border
    step = 18 * SS
    lattice = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lattice)
    for k in range(-H, W + H, step):
        ld.line([(k, 0), (k + H, H)], fill=GOLD + (90,), width=SS)
        ld.line([(k, H), (k + H, 0)], fill=GOLD + (90,), width=SS)
    clip = Image.new("L", (W, H), 0)
    ImageDraw.Draw(clip).rounded_rectangle([inset + 6 * SS, inset + 6 * SS, W - inset - 6 * SS, H - inset - 6 * SS], 8 * SS, fill=255)
    img.alpha_composite(Image.composite(lattice, Image.new("RGBA", (W, H), (0, 0, 0, 0)), clip))
    d.rounded_rectangle([inset, inset, W - inset, H - inset], 10 * SS, outline=GOLD, width=3 * SS)
    d.rounded_rectangle([inset + 6 * SS, inset + 6 * SS, W - inset - 6 * SS, H - inset - 6 * SS], 8 * SS, outline=GOLD_DARK, width=SS)
    # medallion
    cx, cy, rad = W / 2, H / 2, 38 * SS
    d.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=OXBLOOD_DARK, outline=GOLD, width=3 * SS)
    d.ellipse([cx - rad + 7 * SS, cy - rad + 7 * SS, cx + rad - 7 * SS, cy + rad - 7 * SS], outline=GOLD_DARK, width=SS)
    suit_shape(d, "spades", cx, cy, 40 * SS, GOLD)
    d.rounded_rectangle([SS, SS, W - 1 - SS, H - 1 - SS], 15 * SS, outline=CREAM_EDGE, width=5 * SS)
    img.putalpha(rounded_mask(W, H, 16 * SS))
    downsample(img, CARD_W, CARD_H).save(os.path.join(out, "back_hd.png"), optimize=True)


# ---------------------------------------------------------------- table and UI

def table(out):
    """Dark warm felt under a soft overhead spotlight, falling away to near-black."""
    W, H = 1280, 640
    rng = np.random.default_rng(7)
    y = np.linspace(-1, 1, H)[:, None]
    x = np.linspace(-1, 1, W)[None, :]
    r = np.sqrt((x / 1.25) ** 2 + ((y + 0.05) / 0.95) ** 2)
    light = np.clip(1 - r, 0, 1) ** 1.6
    dark = np.array([20, 12, 8], float)
    lit = np.array([92, 58, 38], float)
    img = dark[None, None, :] + (lit - dark)[None, None, :] * light[..., None]
    fibre = rng.normal(0, 1, (H, W)).astype(np.float32)
    fibre = np.clip(fibre * 40 + 128, 0, 255).astype(np.uint8)
    fibre = (np.asarray(Image.fromarray(fibre, "L").filter(ImageFilter.GaussianBlur(0.8))).astype(float) - 128) / 40
    img += fibre[..., None] * 6
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").save(os.path.join(out, "table.jpg"), quality=90, optimize=True)


def nine_patch(name, out, fill, border, width=2, radius=12, size=48, glow=None):
    S = size * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    inset = (4 if glow else 1) * SS
    if glow:
        g = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        ImageDraw.Draw(g).rounded_rectangle([inset, inset, S - inset, S - inset], radius * SS, outline=glow, width=4 * SS)
        img.alpha_composite(g.filter(ImageFilter.GaussianBlur(3 * SS)))
    d.rounded_rectangle([inset, inset, S - inset, S - inset], radius * SS, fill=fill, outline=border, width=width * SS)
    downsample(img, size, size).save(os.path.join(out, name + ".png"))


def icon_card(fill, inner=None):
    """A small card-shaped icon: gold edge, optional inner border like the card back."""
    W, H = 84 * SS, 108 * SS
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([3 * SS, 3 * SS, W - 3 * SS, H - 3 * SS], 12 * SS, fill=fill, outline=GOLD, width=5 * SS)
    if inner:
        d.rounded_rectangle([11 * SS, 11 * SS, W - 11 * SS, H - 11 * SS], 7 * SS, outline=inner, width=2 * SS)
    return img, d


def struck_zero(d, W, H, colour, shadow):
    """A circle struck through, drawn twice: a dark offset copy, then the symbol."""
    r, t, k = 24 * SS, 7 * SS, 24 * SS * 0.72
    for dx, c in ((2 * SS, shadow), (0, colour)):
        d.ellipse([W / 2 - r + dx, H / 2 - r + dx, W / 2 + r + dx, H / 2 + r + dx], outline=c, width=t)
        d.line([(W / 2 - k + dx, H / 2 + k + dx), (W / 2 + k + dx, H / 2 - k + dx)], fill=c, width=t)


def brushed_x(d, W, H, colour, shadow):
    """An X of two slightly tapered strokes, thicker near their start, with a dark offset copy behind."""
    k = 21 * SS
    for dx, c in ((2 * SS, shadow), (0, colour)):
        for (x0, y0, x1, y1) in [(-k, -k * 1.15, k, k * 1.15), (k, -k * 1.15, -k, k * 1.15)]:
            for i in range(41):
                t = i / 40
                r = (6.5 - 2.5 * abs(t - 0.35)) * SS
                cx, cy = W / 2 + x0 + (x1 - x0) * t + dx, H / 2 + y0 + (y1 - y0) * t + dx
                d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=c)


def icons(out):
    # a won trick: a small cream card with a gold edge and a dark pip
    W, H = 84 * SS, 108 * SS
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([3 * SS, 3 * SS, W - 3 * SS, H - 3 * SS], 12 * SS, fill=CREAM, outline=GOLD, width=5 * SS)
    suit_shape(d, "spades", W / 2, H / 2, 46 * SS, INK)
    downsample(img, 84, 108).save(os.path.join(out, "icon_trick.png"))
    # a trick over the bid: an oxblood card, inner gold border, a gold brushed X with an engraved shadow
    img, d = icon_card(OXBLOOD, inner=GOLD)
    brushed_x(d, W, H, GOLD_LIGHT, shadow=OXBLOOD_DARK)
    downsample(img, 84, 108).save(os.path.join(out, "icon_over.png"))
    # a bid of zero: an espresso card, inner gold border, a gold struck-through 0 with an engraved shadow
    img, d = icon_card((44, 30, 20), inner=GOLD_DARK)
    struck_zero(d, W, H, GOLD_LIGHT, shadow=(10, 6, 4))
    downsample(img, 84, 108).save(os.path.join(out, "icon_zero.png"))
    # the dealer button: a gold chip with a D
    S = 108 * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([4 * SS, 4 * SS, S - 4 * SS, S - 4 * SS], fill=GOLD, outline=GOLD_DARK, width=4 * SS)
    d.ellipse([16 * SS, 16 * SS, S - 16 * SS, S - 16 * SS], fill=GOLD_LIGHT, outline=GOLD_DARK, width=2 * SS)
    font = ImageFont.truetype(SERIF_BOLD, 56 * SS)
    d.text((S / 2, S / 2 + 2 * SS), "D", font=font, fill=ESPRESSO, anchor="mm")
    downsample(img, 108, 108).save(os.path.join(out, "icon_dealer.png"))


def level_badges(out):
    """Difficulty badges: gold-rimmed chips like the dealer button, with one, two or three spades (Hard in oxblood)."""
    for level, name, fill in ((1, "easy", (44, 30, 20)), (2, "normal", (44, 30, 20)), (3, "hard", OXBLOOD)):
        S = 108 * SS
        img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.ellipse([4 * SS, 4 * SS, S - 4 * SS, S - 4 * SS], fill=fill, outline=GOLD, width=5 * SS)
        d.ellipse([14 * SS, 14 * SS, S - 14 * SS, S - 14 * SS], outline=GOLD_DARK, width=2 * SS)
        c = S / 2
        pips = {1: [(c, c)], 2: [(c - 17 * SS, c), (c + 17 * SS, c)],
                3: [(c, c - 15 * SS), (c - 18 * SS, c + 14 * SS), (c + 18 * SS, c + 14 * SS)]}[level]
        size = {1: 40, 2: 30, 3: 28}[level] * SS
        for (x, y) in pips:
            suit_shape(d, "spades", x + 1.5 * SS, y + 1.5 * SS, size, (10, 6, 4))
            suit_shape(d, "spades", x, y, size, GOLD_LIGHT)
        downsample(img, 108, 108).save(os.path.join(out, "level_" + name + ".png"))


def sprites(out):
    # soft shadow for cards: a blurred rounded rectangle with margin all round (card area 120x160 of 160x200)
    S = 4
    img = Image.new("L", (160 * S, 200 * S), 0)
    ImageDraw.Draw(img).rounded_rectangle([20 * S, 20 * S, 140 * S, 180 * S], 10 * S, fill=255)
    img = img.filter(ImageFilter.GaussianBlur(9 * S)).resize((160, 200), Image.LANCZOS)
    shadow = Image.new("RGBA", (160, 200), (0, 0, 0, 0))
    shadow.putalpha(img)
    shadow.save(os.path.join(out, "shadow.png"))
    # glow particle: white, fading out radially (tinted gold when drawn)
    n = 64
    y, x = np.mgrid[0:n, 0:n]
    r = np.sqrt((x - n / 2 + 0.5) ** 2 + (y - n / 2 + 0.5) ** 2) / (n / 2)
    a = np.clip(1 - r, 0, 1) ** 2.2
    glow = np.zeros((n, n, 4), np.uint8)
    glow[..., :3] = 255
    glow[..., 3] = (a * 255).astype(np.uint8)
    Image.fromarray(glow, "RGBA").save(os.path.join(out, "glow.png"))


def blurred_sheet(sheet, out, pad=24, radius=7):
    """The card sheet blurred, each card in a padded cell so the blur has room to spread (title depth of field)."""
    cw, ch = CARD_W + 2 * pad, CARD_H + 2 * pad
    bl = Image.new("RGBA", (cw * 13, ch * 4), (0, 0, 0, 0))
    for r in range(4):
        for c in range(13):
            card = sheet.crop((c * CARD_W, r * CARD_H, (c + 1) * CARD_W, (r + 1) * CARD_H))
            cell = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
            cell.paste(card, (pad, pad))
            bl.paste(premultiplied_blur(cell, radius), (c * cw, r * ch))
    # stored at half size (it's blurred anyway); the game reads cell sizes from the image
    hw, hh = round(cw / 2), round(ch / 2)
    half = Image.new("RGBA", (hw * 13, hh * 4), (0, 0, 0, 0))
    for r in range(4):
        for c in range(13):
            half.paste(bl.crop((c * cw, r * ch, (c + 1) * cw, (r + 1) * ch)).resize((hw, hh), Image.LANCZOS), (c * hw, r * hh))
    half.save(os.path.join(out, "cards_blur.png"), optimize=True)
    back = Image.open(os.path.join(out, "back_hd.png"))
    cell = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    cell.paste(back, (pad, pad))
    premultiplied_blur(cell, radius).save(os.path.join(out, "back_blur.png"))


def premultiplied_blur(img, radius):
    """Blurs without dark fringes at transparent edges."""
    a = np.asarray(img).astype(np.float32) / 255.0
    pre = np.concatenate([a[..., :3] * a[..., 3:4], a[..., 3:4]], -1)
    blurred = []
    for i in range(4):
        ch = Image.fromarray((pre[..., i] * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(radius))
        blurred.append(np.asarray(ch).astype(np.float32) / 255.0)
    rgb, al = np.stack(blurred[:3], -1), blurred[3][..., None]
    out = np.concatenate([np.where(al > 1e-3, rgb / np.maximum(al, 1e-3), 0), al], -1)
    return Image.fromarray(np.clip(out * 255, 0, 255).astype(np.uint8), "RGBA")


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "..", "assets", "images")
    os.makedirs(out, exist_ok=True)
    sheet = card_sheet(out)
    card_back(out)
    blurred_sheet(sheet, out)
    table(out)
    icons(out)
    level_badges(out)
    sprites(out)
    nine_patch("panel", out, fill=(18, 12, 8, 222), border=(176, 136, 66, 255))
    nine_patch("panel_turn", out, fill=(52, 36, 14, 236), border=GOLD_LIGHT + (255,), width=3, glow=GOLD_LIGHT + (200,))
    nine_patch("button", out, fill=(116, 22, 24, 245), border=GOLD + (255,))
    nine_patch("button_alt", out, fill=(30, 20, 14, 232), border=(150, 116, 60, 255))
    nine_patch("button_off", out, fill=(52, 46, 42, 220), border=(96, 88, 78, 255))
    print("done")
