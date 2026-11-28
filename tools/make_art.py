#!/usr/bin/env python3
"""
Draws the game's card deck, card back, table, icons and UI frames in one warm
palette (espresso, cream, oxblood, gold), at high resolution.

Everything is drawn from vector shapes: the number cards here, the jacks, queens
and kings in court_art.py.

Usage: python3 tools/make_art.py [output dir, default assets/images]
Needs Pillow, NumPy, the DejaVu fonts and TeX Gyre Pagella (the bid numerals).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

import court_art

HERE = os.path.dirname(os.path.abspath(__file__))
SS = 4  # supersampling for smooth edges

# palette
CREAM = (243, 232, 210)
CREAM_EDGE = (226, 210, 180)
# card faces: a shade under CREAM, so the trump glints show against them
CARD_PAPER = (233, 221, 197)
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
    base = np.array(CARD_PAPER, float)[None, None, :] * (1.02 - 0.05 * y[..., None])
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


def face_card(rank, suit, seed):
    """A court card: our own traditional-style figure (see court_art.py) in a gold-and-oxblood frame."""
    aw, ah = court_art.W, court_art.H  # clear of the corner indices
    art = court_art.court_art(rank, suit, SS, suit_shape)

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
    sheet = Image.new("RGBA", (CARD_W * 13, CARD_H * 4), (0, 0, 0, 0))
    for r, suit in enumerate(SUITS):
        for c, rank in enumerate(RANKS):
            seed = r * 13 + c
            card = face_card(rank, suit, seed) if rank in "JQK" else number_card(rank, suit, seed)
            sheet.paste(card, (c * CARD_W, r * CARD_H))
    # 256 colours with dithering: looks the same, a tenth of the file size
    sheet.convert("RGBA").quantize(256, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.FLOYDSTEINBERG).save(
        os.path.join(out, "cards_hd.png"), optimize=True)
    return sheet


def card_back(out, name="back_hd.png", colour=OXBLOOD, deep=OXBLOOD_DARK, suit="spades", gilt=False):
    W, H = CARD_W * SS, CARD_H * SS
    y = np.linspace(0, 1, H)[:, None, None]
    x = np.linspace(0, 1, W)[None, :, None]
    r = np.sqrt((x - 0.5) ** 2 + (y - 0.5) ** 2)
    base = np.array(colour, float) * (1.15 - 0.6 * r)
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
    d.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=deep, outline=GOLD, width=3 * SS)
    d.ellipse([cx - rad + 7 * SS, cy - rad + 7 * SS, cx + rad - 7 * SS, cy + rad - 7 * SS], outline=GOLD_DARK, width=SS)
    suit_shape(d, suit, cx, cy, 40 * SS, GOLD)
    if gilt:
        gild(img, d, W, H, inset, cx, cy, rad, suit)
    d.rounded_rectangle([SS, SS, W - 1 - SS, H - 1 - SS], 15 * SS, outline=CREAM_EDGE, width=5 * SS)
    img.putalpha(rounded_mask(W, H, 16 * SS))
    downsample(img, CARD_W, CARD_H).save(os.path.join(out, name), optimize=True)


def gild(img, d, W, H, inset, cx, cy, rad, suit):
    """Gold leaf for the Gilded back: a burnished band inside the border, rays from the medallion,
    a ring of pips and a gold suit in each corner."""
    band = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    bd = ImageDraw.Draw(band)
    for k, a in ((0, 255), (5, 200), (10, 140)):
        bd.rounded_rectangle([inset - (9 - k) * SS, inset - (9 - k) * SS, W - inset + (9 - k) * SS, H - inset + (9 - k) * SS],
                             12 * SS, outline=GOLD_LIGHT + (a,), width=2 * SS)
    img.alpha_composite(band)
    rays = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rays)
    for k in range(24):
        a = math.radians(k * 15)
        r0, r1 = rad + 6 * SS, rad + (34 if k % 2 == 0 else 20) * SS
        rd.line([(cx + r0 * math.cos(a), cy + r0 * math.sin(a)), (cx + r1 * math.cos(a), cy + r1 * math.sin(a))],
                fill=GOLD_LIGHT + (230,), width=2 * SS)
    img.alpha_composite(rays)
    for k in range(12):
        a = math.radians(k * 30 + 15)
        px, py = cx + (rad - 4 * SS) * math.cos(a), cy + (rad - 4 * SS) * math.sin(a)
        d.ellipse([px - 2.5 * SS, py - 2.5 * SS, px + 2.5 * SS, py + 2.5 * SS], fill=GOLD_LIGHT)
    for qx, qy in ((inset + 20 * SS, inset + 20 * SS), (W - inset - 20 * SS, inset + 20 * SS),
                   (inset + 20 * SS, H - inset - 20 * SS), (W - inset - 20 * SS, H - inset - 20 * SS)):
        d.ellipse([qx - 13 * SS, qy - 13 * SS, qx + 13 * SS, qy + 13 * SS], fill=GOLD_DARK, outline=GOLD_LIGHT, width=2 * SS)
        suit_shape(d, suit, qx, qy, 14 * SS, GOLD_LIGHT)


# ---------------------------------------------------------------- table and UI

def card_slot(out):
    """An empty card's place: a dashed outline of a card, for a hand dealt with no trump card."""
    W, H, r, line = CARD_W * SS, CARD_H * SS, 16 * SS, 5 * SS
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    inset = line
    d.rounded_rectangle([inset, inset, W - 1 - inset, H - 1 - inset], r, fill=(12, 8, 5, 70))
    x0, y0, x1, y1 = inset, inset, W - 1 - inset, H - 1 - inset
    path = []
    corners = [(x1 - r, y0 + r, -90), (x1 - r, y1 - r, 0), (x0 + r, y1 - r, 90), (x0 + r, y0 + r, 180)]
    for cx, cy, start in corners:
        for k in range(0, 91, 3):
            a = math.radians(start + k)
            path.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    path.append(path[0])
    lengths = [0.0]
    for (ax, ay), (bx, by) in zip(path, path[1:]):
        lengths.append(lengths[-1] + math.hypot(bx - ax, by - ay))
    total = lengths[-1]
    dashes = 34
    period = total / dashes

    def point_at(s):
        i = max(0, min(len(path) - 2, np.searchsorted(lengths, s) - 1))
        t = (s - lengths[i]) / max(1e-6, lengths[i + 1] - lengths[i])
        return (path[i][0] + (path[i + 1][0] - path[i][0]) * t, path[i][1] + (path[i + 1][1] - path[i][1]) * t)

    for k in range(dashes):
        start = k * period
        pts = [point_at(start + period * 0.55 * j / 8) for j in range(9)]
        d.line(pts, fill=CREAM_EDGE + (200,), width=line, joint="curve")
        for p in (pts[0], pts[-1]):
            d.ellipse([p[0] - line / 2, p[1] - line / 2, p[0] + line / 2, p[1] + line / 2], fill=CREAM_EDGE + (200,))
    downsample(img, CARD_W, CARD_H).save(os.path.join(out, "card_slot.png"), optimize=True)


def table(out, name="table.jpg", dark=(20, 12, 8), lit=(92, 58, 38), gilt=False):
    """Dark warm felt under a soft overhead spotlight, falling away to near-black."""
    W, H = 1280, 640
    rng = np.random.default_rng(7)
    y = np.linspace(-1, 1, H)[:, None]
    x = np.linspace(-1, 1, W)[None, :]
    r = np.sqrt((x / 1.25) ** 2 + ((y + 0.05) / 0.95) ** 2)
    light = np.clip(1 - r, 0, 1) ** 1.6
    dark = np.array(dark, float)
    lit = np.array(lit, float)
    img = dark[None, None, :] + (lit - dark)[None, None, :] * light[..., None]
    fibre = rng.normal(0, 1, (H, W)).astype(np.float32)
    fibre = np.clip(fibre * 40 + 128, 0, 255).astype(np.uint8)
    fibre = (np.asarray(Image.fromarray(fibre, "L").filter(ImageFilter.GaussianBlur(0.8))).astype(float) - 128) / 40
    img += fibre[..., None] * 6
    if gilt:
        flecks = np.clip(rng.normal(0, 1, (H, W)) - 2.6, 0, None) * light
        flecks = np.asarray(Image.fromarray(np.clip(flecks * 255, 0, 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(0.6))) / 255.0
        img += flecks[..., None] * np.array((255, 205, 110), float) * 1.4
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").save(os.path.join(out, name), quality=90, optimize=True)


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


def sunken_panel(name, out, fill, border, radius=12, size=48):
    """The reverse of the turn highlight: a darker panel pressed into the table, its dim edge
    shaded on the inside (heaviest along the top), and no glow."""
    S = size * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    inset = 2 * SS
    box = [inset, inset, S - inset, S - inset]
    d.rounded_rectangle(box, radius * SS, fill=fill)
    # an inner shadow: a dark ring blurred inward, kept inside the panel
    ring = Image.new("L", (S, S), 0)
    rd = ImageDraw.Draw(ring)
    rd.rounded_rectangle(box, radius * SS, outline=255, width=7 * SS)
    rd.rectangle([inset, inset, S - inset, inset + 5 * SS], fill=255)
    ring = ring.filter(ImageFilter.GaussianBlur(3 * SS))
    inside = Image.new("L", (S, S), 0)
    ImageDraw.Draw(inside).rounded_rectangle(box, radius * SS, fill=255)
    shade = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    shade.putalpha(Image.fromarray((np.asarray(ring, np.float32) * np.asarray(inside, np.float32) / 255 * 0.7).astype(np.uint8)))
    img.alpha_composite(shade)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle(box, radius * SS, outline=border, width=2 * SS)
    downsample(img, size, size).save(os.path.join(out, name + ".png"))


def icon_card(fill, inner=None, broad=True):
    """A small card-shaped icon: a gold edge, optional inner border like the card back.

    Placard icons have a broad edge (on a dark card it's the outline you see, so dark and
    light icons read the same size); the bid tracker's smaller cards keep a thinner one.
    """
    W, H = 84 * SS, 108 * SS
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if broad:
        d.rounded_rectangle([2 * SS, 2 * SS, W - 2 * SS, H - 2 * SS], 12 * SS, fill=fill, outline=GOLD, width=8 * SS)
        if inner:
            d.rounded_rectangle([14 * SS, 14 * SS, W - 14 * SS, H - 14 * SS], 6 * SS, outline=inner, width=2 * SS)
    else:
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


# Icon symbols share one size (about the spade's): ICON_SYMBOL is the hourglass's and the X's scale
# against the drawings' original, card-filling size.
ICON_SYMBOL = 0.7


def hourglass(d, W, H, colour, shadow):
    """An hourglass: a bar top and bottom, two glass bulbs meeting at the waist, sand settled below."""
    u = SS * ICON_SYMBOL
    cx, cy, hw, hh, bar = W / 2, H / 2, 20 * u, 30 * u, 5 * u
    for dx, c in ((2 * SS, shadow), (0, colour)):
        x, y = cx + dx, cy + dx
        d.rounded_rectangle([x - hw - 5 * u, y - hh - bar, x + hw + 5 * u, y - hh], 2 * u, fill=c)
        d.rounded_rectangle([x - hw - 5 * u, y + hh, x + hw + 5 * u, y + hh + bar], 2 * u, fill=c)
        glass = [(x - hw, y - hh), (x + hw, y - hh), (x + 3 * u, y), (x + hw, y + hh), (x - hw, y + hh), (x - 3 * u, y)]
        d.line(glass + [glass[0]], fill=c, width=int(5 * u), joint="curve")
        if c == colour:
            # a little sand left above, a heap below
            d.polygon([(x - 9 * u, y - 11 * u), (x + 9 * u, y - 11 * u), (x, y - 1 * u)], fill=c)
            d.polygon([(x - hw + 4 * u, y + hh - 2 * u), (x + hw - 4 * u, y + hh - 2 * u), (x, y + 13 * u)], fill=c)


def brushed_x(d, W, H, colour, shadow):
    """An X of two slightly tapered strokes, thicker near their start, with a dark offset copy behind."""
    u = SS * ICON_SYMBOL
    k = 21 * u
    for dx, c in ((2 * SS, shadow), (0, colour)):
        for (x0, y0, x1, y1) in [(-k, -k * 1.15, k, k * 1.15), (k, -k * 1.15, -k, k * 1.15)]:
            for i in range(41):
                t = i / 40
                r = (7 - 2.5 * abs(t - 0.35)) * u
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
    # no bid yet: an espresso card, inner gold border, a gold hourglass with an engraved shadow
    img, d = icon_card((44, 30, 20), inner=GOLD_DARK)
    hourglass(d, W, H, GOLD_LIGHT, shadow=(10, 6, 4))
    downsample(img, 84, 108).save(os.path.join(out, "icon_waiting.png"))
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


def gilt_ring(out):
    """The gilding around a champion's level badge: a double gold ring set with eight diamonds, open in the middle
    so the badge shows through (drawn 1.3 times the badge's size)."""
    S = 140 * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = S / 2
    for r, colour, width in ((64, GOLD_DARK, 3), (60, GOLD_LIGHT, 4), (55, GOLD, 2)):
        d.ellipse([c - r * SS, c - r * SS, c + r * SS, c + r * SS], outline=colour + (255,), width=width * SS)
    for k in range(8):
        a = math.pi / 4 * k
        x, y = c + math.cos(a) * 60 * SS, c + math.sin(a) * 60 * SS
        suit_shape(d, "diamonds", x, y, 14 * SS, GOLD_LIGHT + (255,))
    downsample(img, 140, 140).save(os.path.join(out, "gilt_ring.png"))


def bid_icons(out):
    """The table's bid tracker, one card per trick in the hand: open (no bid on it yet), bid, and bid over the hand."""
    W, H = 84 * SS, 108 * SS
    # open: a faint silhouette of a card
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([4 * SS, 4 * SS, W - 4 * SS, H - 4 * SS], 12 * SS, fill=(70, 52, 36, 120),
                        outline=(150, 118, 70, 210), width=5 * SS)
    downsample(img, 84, 108).save(os.path.join(out, "bid_open.png"))
    # bid: a cream card with an inner gold border and a small gold diamond
    img, d = icon_card(CREAM, inner=GOLD_DARK, broad=False)
    c, r = (W / 2, H / 2), 13 * SS
    d.polygon([(c[0], c[1] - r * 1.3), (c[0] + r, c[1]), (c[0], c[1] + r * 1.3), (c[0] - r, c[1])], fill=GOLD_DARK)
    downsample(img, 84, 108).save(os.path.join(out, "bid_taken.png"))
    # over: the same card in a warm red, for bids beyond the cards in the hand
    img, d = icon_card((178, 46, 36), inner=GOLD_LIGHT, broad=False)
    d.polygon([(c[0], c[1] - r * 1.3), (c[0] + r, c[1]), (c[0], c[1] + r * 1.3), (c[0] - r, c[1])], fill=GOLD_LIGHT)
    downsample(img, 84, 108).save(os.path.join(out, "bid_over.png"))


NUMERAL_FONT = "/usr/share/texmf/fonts/opentype/public/tex-gyre/texgyrepagella-bold.otf"


def bid_digits(out):
    """Bid numerals 0-9 in one strip, then a plain 0 for the tens: gold, shaded darker toward
    the foot, with a dark edge and an engraved shadow; the first 0 (a bid of zero) is struck
    through like the zero-bid icon.
    The numeral font must be TeX Gyre Pagella: its empty-set sign is the slashed zero."""
    CW, CH = 64, 88
    W, H = CW * SS, CH * SS
    font = ImageFont.truetype(NUMERAL_FONT, 80 * SS)
    strip = Image.new("RGBA", (CW * 11, CH), (0, 0, 0, 0))
    for n in range(11):
        mask = Image.new("L", (W, H), 0)
        md = ImageDraw.Draw(mask)
        cx, cy = W / 2, H / 2 + 2 * SS
        # the 0 is the font's own slashed zero (the empty-set sign, drawn as its 0 struck through)
        if n == 0:
            # its slash reaches past the other digits, so a little smaller, centred on its own outline
            zero = ImageFont.truetype(NUMERAL_FONT, 72 * SS)
            l, t, r, b = md.textbbox((0, 0), "\u2205", font=zero)
            md.text((cx - (l + r) / 2, H / 2 - (t + b) / 2), "\u2205", font=zero, fill=255)
        else:
            md.text((cx, cy), str(n % 10), font=font, fill=255, anchor="mm")
        edge = mask.filter(ImageFilter.MaxFilter(2 * (2 * SS) + 1))
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        # engraved shadow, then a dark edge, then the gold
        shadow = Image.new("RGBA", (W, H), (10, 6, 4, 200))
        img.paste(shadow, (3 * SS, 3 * SS), edge)
        img.paste(Image.new("RGBA", (W, H), ESPRESSO + (255,)), (0, 0), edge)
        grad = Image.new("RGBA", (W, H))
        gd = ImageDraw.Draw(grad)
        for yy in range(H):
            t = min(max((yy - H * 0.22) / (H * 0.56), 0), 1)
            c = tuple(int(a + (b - a) * t) for a, b in zip((255, 232, 170), GOLD_DARK))
            gd.line([(0, yy), (W, yy)], fill=c + (255,))
        img.paste(grad, (0, 0), mask)
        strip.paste(downsample(img, CW, CH), (n * CW, 0))
    strip.save(os.path.join(out, "bid_digits.png"))


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
    blurred_back(out, "back_hd.png", "back_blur.png", pad, radius)


def blurred_back(out, src, dst, pad=24, radius=7):
    cell = Image.new("RGBA", (CARD_W + 2 * pad, CARD_H + 2 * pad), (0, 0, 0, 0))
    cell.paste(Image.open(os.path.join(out, src)), (pad, pad))
    premultiplied_blur(cell, radius).save(os.path.join(out, dst))


def chapter_cosmetics(out):
    """Each chapter's prize: a card back and a felt. Chapter 1: emerald and club, a card-room green;
    2: amethyst and diamond, a deep purple; 3: midnight and heart, a late-night navy; 4: gilded and spade, gold leaf
    on old gold."""
    card_back(out, "back_ch1.png", colour=(22, 92, 58), deep=(10, 48, 30), suit="clubs")
    blurred_back(out, "back_ch1.png", "back_ch1_blur.png")
    table(out, "table_ch1.jpg", dark=(6, 18, 12), lit=(30, 86, 56))
    card_back(out, "back_ch2.png", colour=(98, 40, 150), deep=(44, 14, 74), suit="diamonds")
    blurred_back(out, "back_ch2.png", "back_ch2_blur.png")
    table(out, "table_ch2.jpg", dark=(14, 6, 22), lit=(72, 34, 108))
    card_back(out, "back_ch3.png", colour=(24, 38, 92), deep=(10, 16, 46), suit="hearts")
    blurred_back(out, "back_ch3.png", "back_ch3_blur.png")
    table(out, "table_ch3.jpg", dark=(6, 9, 22), lit=(30, 44, 92))
    card_back(out, "back_ch4.png", colour=(150, 108, 34), deep=(70, 44, 10), suit="spades", gilt=True)
    blurred_back(out, "back_ch4.png", "back_ch4_blur.png")
    table(out, "table_ch4.jpg", dark=(20, 14, 6), lit=(96, 70, 30), gilt=True)


def chapter_plaques(out):
    """The campaign hall's chapters: a tall arched plaque framed in gold, like a doorway, in a lit
    (wine) and a locked (ash) version."""
    W, H = 200 * SS, 300 * SS
    for name, top, bottom, edge, inner in (("plaque.png", (92, 30, 26), (40, 12, 12), GOLD, GOLD_DARK),
                                          ("plaque_locked.png", (46, 40, 36), (20, 17, 15), GOLD_DARK, (70, 58, 40))):
        y = np.linspace(0, 1, H)[:, None, None]
        x = np.linspace(0, 1, W)[None, :, None]
        glow = np.clip(1 - np.sqrt((x - 0.5) ** 2 * 3 + (y - 0.35) ** 2), 0, 1)
        fill = np.array(bottom, float) + (np.array(top, float) - np.array(bottom, float)) * glow
        body = Image.fromarray(np.clip(fill, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
        mask = Image.new("L", (W, H), 0)
        md = ImageDraw.Draw(mask)
        r = W / 2
        md.ellipse([0, 0, W - 1, 2 * r], fill=255)
        md.rectangle([0, r, W - 1, H - 1], fill=255)
        body.putalpha(mask)
        d = ImageDraw.Draw(body)
        for inset, colour, width in ((3 * SS, edge, 4 * SS), (12 * SS, inner, 2 * SS), (17 * SS, edge, 1 * SS)):
            rr = r - inset
            d.arc([inset, inset, W - 1 - inset, inset + 2 * rr], 180, 360, fill=colour, width=width)
            d.line([(inset + width / 2 - SS, r), (inset + width / 2 - SS, H - 1 - inset)], fill=colour, width=width)
            d.line([(W - 1 - inset - width / 2 + SS, r), (W - 1 - inset - width / 2 + SS, H - 1 - inset)], fill=colour, width=width)
            d.line([(inset, H - 1 - inset - width / 2 + SS), (W - 1 - inset, H - 1 - inset - width / 2 + SS)], fill=colour, width=width)
        for cx, cy, size in ((W / 2, 12 * SS, 14 * SS), (14 * SS, H - 14 * SS, 10 * SS), (W - 14 * SS, H - 14 * SS, 10 * SS)):
            suit_shape(d, "diamonds", cx, cy, size, edge)
        downsample(body, 200, 300).save(os.path.join(out, name), optimize=True)


def campaign_art(out):
    """The campaign's cards: a dark face framed in gold for chapters and steps, a gold emblem per
    suit (each chapter has one), an icon per step, and the diamond pips that mark steps done."""
    W, H = CARD_W * SS, CARD_H * SS
    y = np.linspace(0, 1, H)[:, None, None]
    x = np.linspace(0, 1, W)[None, :, None]
    r = np.sqrt((x - 0.5) ** 2 + (y - 0.45) ** 2)
    face = np.array((58, 26, 20), float) * (1.2 - 0.7 * r)
    img = Image.fromarray(np.clip(face, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(img)
    inset = 10 * SS
    d.rounded_rectangle([inset, inset, W - inset, H - inset], 10 * SS, outline=GOLD, width=3 * SS)
    d.rounded_rectangle([inset + 7 * SS, inset + 7 * SS, W - inset - 7 * SS, H - inset - 7 * SS], 7 * SS, outline=GOLD_DARK, width=SS)
    for cx, cy in [(inset + 7 * SS, inset + 7 * SS), (W - inset - 7 * SS, inset + 7 * SS), (inset + 7 * SS, H - inset - 7 * SS), (W - inset - 7 * SS, H - inset - 7 * SS)]:
        suit_shape(d, "diamonds", cx, cy, 16 * SS, GOLD)
    d.rounded_rectangle([SS, SS, W - 1 - SS, H - 1 - SS], 15 * SS, outline=GOLD_DARK, width=4 * SS)
    img.putalpha(rounded_mask(W, H, 16 * SS))
    downsample(img, CARD_W, CARD_H).save(os.path.join(out, "campaign_face.png"), optimize=True)

    def icon(name, draw_it):
        S = 108 * SS
        im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        draw_it(ImageDraw.Draw(im), S)
        downsample(im, 108, 108).save(os.path.join(out, name + ".png"))

    for suit in ("spades", "diamonds", "hearts", "clubs"):
        icon("emblem_" + suit, lambda dr, S, suit=suit: suit_shape(dr, suit, S / 2, S / 2, S * 0.86, GOLD + (255,)))

    def small_card(dr, cx, cy, w, h, angle, fill):
        pts = [(-w / 2, -h / 2), (w / 2, -h / 2), (w / 2, h / 2), (-w / 2, h / 2)]
        a = math.radians(angle)
        rot = [(cx + px * math.cos(a) - py * math.sin(a), cy + px * math.sin(a) + py * math.cos(a)) for px, py in pts]
        dr.polygon(rot, fill=fill, outline=GOLD_DARK + (255,), width=3 * SS)

    def table_icon(dr, S):
        for k, ang in enumerate((-22, 0, 22)):
            small_card(dr, S / 2 + (k - 1) * S * 0.14, S * 0.56, S * 0.32, S * 0.46, ang, GOLD + (255,))
        suit_shape(dr, "spades", S / 2, S * 0.52, S * 0.18, GOLD_DARK + (255,))

    def shots_icon(dr, S):
        c = S / 2
        for rr, col in ((0.44, GOLD), (0.32, GOLD_DARK), (0.2, GOLD), (0.08, GOLD_DARK)):
            dr.ellipse([c - rr * S, c - rr * S, c + rr * S, c + rr * S], fill=col + (255,))
        dr.line([(S * 0.86, S * 0.14), (c + 2 * SS, c - 2 * SS)], fill=GOLD_LIGHT + (255,), width=5 * SS)
        dr.polygon([(c, c), (c + 0.1 * S, c - 0.03 * S), (c + 0.03 * S, c - 0.1 * S)], fill=GOLD_LIGHT + (255,))

    def duel_icon(dr, S):
        small_card(dr, S * 0.4, S * 0.52, S * 0.36, S * 0.52, -18, GOLD + (255,))
        small_card(dr, S * 0.6, S * 0.48, S * 0.36, S * 0.52, 18, GOLD_LIGHT + (255,))
        suit_shape(dr, "hearts", S * 0.6, S * 0.48, S * 0.16, GOLD_DARK + (255,))

    icon("step_table", table_icon)
    icon("step_shots", shots_icon)
    icon("step_duel", duel_icon)
    icon("pip_on", lambda dr, S: suit_shape(dr, "diamonds", S / 2, S / 2, S * 0.9, GOLD_LIGHT + (255,)))

    def pip_off(dr, S):
        c = S / 2
        dr.polygon([(c, c - 0.45 * S), (c + 0.32 * S, c), (c, c + 0.45 * S), (c - 0.32 * S, c)], outline=GOLD_DARK + (255,), width=8 * SS)

    icon("pip_off", pip_off)


def play_menu_icons(out):
    """Gold icons for the Play screen's cards: continue, campaign, custom game, high scores."""
    def icon(name, draw_it):
        S = 108 * SS
        im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        draw_it(ImageDraw.Draw(im), S)
        downsample(im, 108, 108).save(os.path.join(out, name + ".png"))

    gold, dark, light = GOLD + (255,), GOLD_DARK + (255,), GOLD_LIGHT + (255,)

    def resume(d, S):
        c, r = S / 2, S * 0.42
        d.ellipse([c - r, c - r, c + r, c + r], outline=gold, width=6 * SS)
        d.polygon([(c - r * 0.35, c - r * 0.5), (c + r * 0.55, c), (c - r * 0.35, c + r * 0.5)], fill=light)

    def campaign(d, S):
        d.line([(S * 0.3, S * 0.12), (S * 0.3, S * 0.9)], fill=dark, width=6 * SS)
        d.polygon([(S * 0.32, S * 0.14), (S * 0.82, S * 0.28), (S * 0.32, S * 0.44)], fill=gold)
        suit_shape(d, "spades", S * 0.46, S * 0.29, S * 0.16, dark)
        d.ellipse([S * 0.22, S * 0.84, S * 0.38, S * 0.94], fill=gold)

    def custom(d, S):
        for k, at in enumerate((0.3, 0.6, 0.45)):
            y = S * (0.25 + 0.25 * k)
            d.line([(S * 0.14, y), (S * 0.86, y)], fill=dark, width=6 * SS)
            x = S * (0.14 + 0.72 * at)
            d.ellipse([x - S * 0.08, y - S * 0.08, x + S * 0.08, y + S * 0.08], fill=gold, outline=light, width=2 * SS)

    def scores(d, S):
        d.pieslice([S * 0.22, S * 0.02, S * 0.78, S * 0.6], 0, 180, fill=gold)
        d.rectangle([S * 0.22, S * 0.08, S * 0.78, S * 0.31], fill=gold)
        d.arc([S * 0.08, S * 0.12, S * 0.32, S * 0.42], 90, 270, fill=gold, width=5 * SS)
        d.arc([S * 0.68, S * 0.12, S * 0.92, S * 0.42], 270, 90, fill=gold, width=5 * SS)
        d.rectangle([S * 0.45, S * 0.58, S * 0.55, S * 0.76], fill=dark)
        d.rounded_rectangle([S * 0.3, S * 0.76, S * 0.7, S * 0.9], 4 * SS, fill=gold)
        points = []
        for k in range(10):
            ang = math.pi / 2 + k * math.pi / 5
            rr = S * (0.1 if k % 2 == 0 else 0.045)
            points.append((S / 2 + rr * math.cos(ang), S * 0.27 - rr * math.sin(ang)))
        d.polygon(points, fill=light)

    icon("menu_continue", resume)
    icon("menu_campaign", campaign)
    icon("menu_custom", custom)
    icon("menu_scores", scores)


def badge(out):
    """A gold medal with a star, for the badges on Records; unearned ones are drawn dim."""
    S = 108 * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c, r = S / 2, S / 2 - 4 * SS
    d.ellipse([c - r, c - r, c + r, c + r], fill=GOLD_DARK + (255,))
    d.ellipse([c - r + 6 * SS, c - r + 6 * SS, c + r - 6 * SS, c + r - 6 * SS], fill=GOLD + (255,), outline=GOLD_LIGHT + (255,), width=2 * SS)
    points = []
    for k in range(10):
        a = math.pi / 2 + k * math.pi / 5
        rr = (r - 18 * SS) if k % 2 == 0 else (r - 18 * SS) * 0.45
        points.append((c + rr * math.cos(a), c - rr * math.sin(a)))
    d.polygon(points, fill=GOLD_LIGHT + (255,), outline=GOLD_DARK + (255,))
    downsample(img, 108, 108).save(os.path.join(out, "badge.png"))


def padlock(out):
    """A small gold padlock, shown on a bid button the dealer may not choose when it is tapped."""
    S = 108 * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = S / 2
    d.arc([c - 26 * SS, 12 * SS, c + 26 * SS, 64 * SS], 180, 360, fill=GOLD_LIGHT + (255,), width=10 * SS)
    d.line([c - 26 * SS + 5 * SS, 38 * SS, c - 26 * SS + 5 * SS, 52 * SS], fill=GOLD_LIGHT + (255,), width=10 * SS)
    d.line([c + 26 * SS - 5 * SS, 38 * SS, c + 26 * SS - 5 * SS, 52 * SS], fill=GOLD_LIGHT + (255,), width=10 * SS)
    d.rounded_rectangle([c - 36 * SS, 48 * SS, c + 36 * SS, 100 * SS], 10 * SS, fill=GOLD + (255,), outline=GOLD_DARK + (255,), width=3 * SS)
    d.ellipse([c - 7 * SS, 64 * SS, c + 7 * SS, 78 * SS], fill=GOLD_DARK + (255,))
    d.rectangle([c - 3 * SS, 72 * SS, c + 3 * SS, 88 * SS], fill=GOLD_DARK + (255,))
    downsample(img, 108, 108).save(os.path.join(out, "padlock.png"))


def stone(out):
    """The campaign corridor's stone, as tiles that repeat seamlessly: a wall of rough blocks in staggered courses
    (stone_wall.png) and a floor of large worn flagstones (stone_floor.png). Grey-brown, with dark mortar and a
    little grain, so torchlight can warm them."""
    rng = np.random.default_rng(31)
    S = 256

    def tile(name, rows, cols, base, spread, mortar, stagger):
        grain = rng.normal(0, 1, (S, S))
        grain = np.real(np.fft.ifft2(np.fft.fft2(grain) * np.exp(-((np.fft.fftfreq(S)[:, None] ** 2 + np.fft.fftfreq(S)[None, :] ** 2) * 900))))
        grain = grain / np.abs(grain).max()
        rgb = np.zeros((S, S, 3))
        rgb[:] = mortar
        bh = S / rows
        for r in range(rows):
            bw = S / cols
            shift = (bw / 2 if stagger and r % 2 else 0) + rng.uniform(-6, 6) * (not stagger)
            for c in range(cols + 1):
                tint = np.array(base) + rng.uniform(-spread, spread) + np.array((rng.uniform(-4, 4), 0, rng.uniform(-6, 2)))
                x0, y0 = c * bw + shift + 3, r * bh + 3
                x1, y1 = x0 + bw - 6, y0 + bh - 6
                yy, xx = np.mgrid[0:S, 0:S]
                for off in (-S, 0, S):
                    inside = (xx >= x0 + off) & (xx < x1 + off) & (yy >= y0) & (yy < y1)
                    edge = np.minimum(np.minimum(xx - (x0 + off), (x1 + off) - xx), np.minimum(yy - y0, y1 - yy))
                    bevel = np.clip(edge / 5, 0, 1)
                    lit = 0.75 + 0.25 * bevel + 0.18 * (yy < y0 + 4)
                    rgb[inside] = (tint[None, :] * lit[inside][:, None])
        rgb = rgb * (1 + 0.22 * grain[..., None])
        Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").save(os.path.join(out, name), optimize=True)

    tile("stone_wall.png", 6, 3, (88, 78, 68), 12, (26, 21, 18), True)
    tile("stone_floor.png", 3, 2, (70, 62, 54), 10, (20, 16, 13), False)


def campaign_map(out):
    """The campaign's map, an old parchment chart of the country between the taverns: mountains, forests, a river,
    a lake and a compass rose drawn in brown ink inside a ruled border (map.jpg, the screen's shape; the trail and
    the taverns are drawn by the game over it). And the wax seal each tavern is marked with, lit (seal.png) and
    locked (seal_locked.png)."""
    rng = np.random.default_rng(41)
    W, H = 2048, 848
    def smooth_noise(scale, seed):
        n = np.random.default_rng(seed).normal(0, 1, (H, W))
        fy, fx = np.fft.fftfreq(H)[:, None], np.fft.fftfreq(W)[None, :]
        out = np.real(np.fft.ifft2(np.fft.fft2(n) * np.exp(-(fx ** 2 + fy ** 2) * scale)))
        return out / np.abs(out).max()
    yy, xx = np.mgrid[0:H, 0:W]
    edge = np.minimum(np.minimum(xx, W - 1 - xx) / W, np.minimum(yy, H - 1 - yy) / H * (H / W))
    vignette = np.clip(edge / 0.09, 0, 1) ** 0.6
    blotch = smooth_noise(4e5, 42) * 0.5 + smooth_noise(4e4, 43) * 0.3 + smooth_noise(4e3, 44) * 0.2
    base = np.array((224, 203, 160), float)
    shade = (0.82 + 0.1 * blotch) * (0.55 + 0.45 * vignette)
    rgb = base[None, None, :] * shade[..., None]
    rgb = rgb * (1 + 0.04 * rng.normal(0, 1, (H, W, 1)))
    img = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    S = 2
    ink = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(ink)
    INK = (74, 48, 28, 230)
    FAINT = (74, 48, 28, 120)
    WATER = (58, 66, 80, 200)

    def at(fx, fy):
        return fx * W * S, fy * H * S

    for inset, width in ((22, 5), (34, 2)):
        d.rectangle([inset * S, inset * S, (W - inset) * S, (H - inset) * S], outline=INK, width=width * S)

    def mountain(fx, fy, size):
        x, y = at(fx, fy)
        h, half = size * S, size * 0.75 * S
        d.line([(x - half, y), (x, y - h), (x + half, y)], fill=INK, width=3 * S, joint="curve")
        for k in range(1, 5):
            t = k / 5
            d.line([(x + half * t * 0.9, y - h * (1 - t) * 0.95), (x + half * t * 0.9 - 6 * S, y)], fill=FAINT, width=2 * S)

    for fx, fy, size in ((0.24, 0.24, 46), (0.28, 0.21, 62), (0.33, 0.25, 40), (0.43, 0.2, 52), (0.48, 0.24, 38),
                         (0.68, 0.88, 44), (0.73, 0.85, 58), (0.78, 0.9, 40), (0.97, 0.88, 36)):
        mountain(fx, fy, size)

    def tree(fx, fy, size):
        x, y = at(fx, fy)
        r = size * S
        d.line([(x, y), (x, y + r * 1.3)], fill=INK, width=2 * S)
        d.ellipse([x - r, y - r * 1.4, x + r, y + r * 0.4], outline=INK, width=2 * S)

    for cx, cy, n in ((0.06, 0.5, 7), (0.5, 0.86, 8), (0.74, 0.14, 7), (0.95, 0.4, 6), (0.06, 0.9, 5)):
        for k in range(n):
            tree(cx + rng.uniform(-0.035, 0.035), cy + rng.uniform(-0.05, 0.05), rng.uniform(9, 14))

    river = []
    for k in range(41):
        t = k / 40
        river.append(at(0.555 - 0.07 * t + 0.025 * math.sin(t * 9), 0.035 + t * 0.93))
    d.line(river, fill=WATER, width=7 * S, joint="curve")
    d.line([(x + 10 * S, y) for x, y in river[::2]], fill=(58, 66, 80, 70), width=2 * S)

    lx, ly = at(0.27, 0.66)
    d.ellipse([lx - 70 * S, ly - 30 * S, lx + 70 * S, ly + 30 * S], outline=WATER, width=4 * S)
    for k in range(3):
        d.arc([lx - 40 * S + k * 25 * S, ly - 6 * S, lx - 22 * S + k * 25 * S, ly + 6 * S], 200, 340, fill=(58, 66, 80, 150), width=2 * S)

    cx, cy = at(0.3, 0.84)
    R = 56 * S
    d.ellipse([cx - R, cy - R, cx + R, cy + R], outline=INK, width=2 * S)
    d.ellipse([cx - R * 0.72, cy - R * 0.72, cx + R * 0.72, cy + R * 0.72], outline=FAINT, width=2 * S)
    for k in range(8):
        a = math.pi / 4 * k
        long = R * (1.15 if k % 2 == 0 else 0.7)
        tip = (cx + math.sin(a) * long, cy - math.cos(a) * long)
        side = 9 * S
        left = (cx + math.sin(a - math.pi / 2) * side, cy - math.cos(a - math.pi / 2) * side)
        right = (cx + math.sin(a + math.pi / 2) * side, cy - math.cos(a + math.pi / 2) * side)
        d.polygon([tip, left, (cx, cy)], fill=INK)
        d.polygon([tip, right, (cx, cy)], outline=INK)
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf", 26 * S)
    d.text((cx, cy - R * 1.15 - 6 * S), "N", fill=INK, font=font, anchor="md")

    ink = ink.resize((W, H), Image.LANCZOS)
    img.alpha_composite(ink)
    img.convert("RGB").save(os.path.join(out, "map.jpg"), quality=88, optimize=True)

    for name, fill, deep, rim in (("seal.png", (150, 32, 30), (84, 14, 14), GOLD), ("seal_locked.png", (92, 84, 76), (52, 46, 42), (130, 116, 96))):
        Z = 128 * SS
        im = Image.new("RGBA", (Z, Z), (0, 0, 0, 0))
        sd = ImageDraw.Draw(im)
        c = Z / 2
        pts = []
        for k in range(48):
            a = math.pi * 2 * k / 48
            r = Z * 0.46 * (1 + 0.035 * math.sin(k * 2.7) + 0.02 * math.sin(k * 5.3))
            pts.append((c + math.cos(a) * r, c + math.sin(a) * r))
        sd.polygon(pts, fill=deep + (255,))
        sd.ellipse([c - Z * 0.4, c - Z * 0.4, c + Z * 0.4, c + Z * 0.4], fill=fill + (255,))
        sd.ellipse([c - Z * 0.4, c - Z * 0.4, c + Z * 0.4, c + Z * 0.4], outline=rim + (255,), width=3 * SS)
        sd.ellipse([c - Z * 0.33, c - Z * 0.33, c + Z * 0.33, c + Z * 0.33], outline=rim + (160,), width=1 * SS)
        shine = Image.new("RGBA", (Z, Z), (0, 0, 0, 0))
        ImageDraw.Draw(shine).ellipse([c - Z * 0.3, c - Z * 0.36, c - Z * 0.05, c - Z * 0.22], fill=(255, 255, 255, 38))
        im.alpha_composite(shine)
        downsample(im, 128, 128).save(os.path.join(out, name))


def momentum_icons(out):
    """The momentum bar beside a placard, one image per state: empty, one notch, and lit (x2 on).
    A dark capsule rimmed in gold holds two chevrons that light gold as bids are made running; when
    momentum is on the capsule itself fills with gold and the chevrons shine cream."""
    W, H = 44, 144
    S = SS * 2
    w, h = W * S, H * S
    for state in range(3):
        img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        lit = state == 2
        y = np.linspace(0, 1, h)[:, None]
        if lit:
            top, bottom = np.array(GOLD_LIGHT, float), np.array(GOLD_DARK, float)
        else:
            top, bottom = np.array((66, 38, 26), float), np.array((30, 16, 12), float)
        fill = top * (1 - y) + bottom * y
        body = Image.fromarray(np.repeat(fill[:, None, :], w, axis=1).astype(np.uint8), "RGB").convert("RGBA")
        body.putalpha(rounded_mask(w, h, w // 2))
        img.alpha_composite(body)
        d.rounded_rectangle([S, S, w - 1 - S, h - 1 - S], w // 2 - S, outline=(GOLD_LIGHT if lit else GOLD) + (255,), width=3 * S)
        d.rounded_rectangle([5 * S, 5 * S, w - 1 - 5 * S, h - 1 - 5 * S], w // 2 - 5 * S, outline=GOLD_DARK + (255,), width=S)
        for k, cy in enumerate((0.68, 0.36)):
            on = state > k
            colour = (255, 244, 214) if lit else GOLD_LIGHT if on else (92, 60, 40)
            edge = GOLD_DARK if on else (40, 22, 16)
            cx, yy, arm, rise = w / 2, cy * h, w * 0.3, w * 0.26
            pts = [(cx - arm, yy + rise / 2), (cx, yy - rise / 2), (cx + arm, yy + rise / 2)]
            d.line(pts, fill=edge + (255,), width=int(9 * S), joint="curve")
            d.line(pts, fill=colour + (255,), width=int(6 * S), joint="curve")
        downsample(img, W, H).save(os.path.join(out, "momentum_%d.png" % state))


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
    card_slot(out)
    chapter_cosmetics(out)
    campaign_art(out)
    chapter_plaques(out)
    stone(out)
    campaign_map(out)
    play_menu_icons(out)
    badge(out)
    padlock(out)
    momentum_icons(out)
    blurred_sheet(sheet, out)
    table(out)
    icons(out)
    level_badges(out)
    gilt_ring(out)
    bid_icons(out)
    bid_digits(out)
    sprites(out)
    nine_patch("panel", out, fill=(18, 12, 8, 222), border=(176, 136, 66, 255))
    nine_patch("panel_turn", out, fill=(52, 36, 14, 236), border=GOLD_LIGHT + (255,), width=3, glow=GOLD_LIGHT + (200,))
    sunken_panel("panel_wait", out, fill=(10, 7, 5, 214), border=(98, 76, 46, 255))
    nine_patch("button", out, fill=(116, 22, 24, 245), border=GOLD + (255,))
    nine_patch("button_alt", out, fill=(30, 20, 14, 232), border=(150, 116, 60, 255))
    nine_patch("button_off", out, fill=(52, 46, 42, 220), border=(96, 88, 78, 255))
    print("done")
