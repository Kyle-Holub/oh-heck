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


def card_back(out, name="back_hd.png", colour=OXBLOOD, deep=OXBLOOD_DARK, suit="spades"):
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
    d.rounded_rectangle([SS, SS, W - 1 - SS, H - 1 - SS], 15 * SS, outline=CREAM_EDGE, width=5 * SS)
    img.putalpha(rounded_mask(W, H, 16 * SS))
    downsample(img, CARD_W, CARD_H).save(os.path.join(out, name), optimize=True)


# ---------------------------------------------------------------- table and UI

def card_blank(out):
    """A card face with nothing printed on it, for the campaign's chapter and step cards."""
    downsample(card_base(77), CARD_W, CARD_H).save(os.path.join(out, "card_blank.png"), optimize=True)


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


def table(out, name="table.jpg", dark=(20, 12, 8), lit=(92, 58, 38)):
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
    """Each chapter's prize: a card back and a felt. Chapter 1: emerald and club, a card-room green."""
    card_back(out, "back_ch1.png", colour=(22, 92, 58), deep=(10, 48, 30), suit="clubs")
    blurred_back(out, "back_ch1.png", "back_ch1_blur.png")
    table(out, "table_ch1.jpg", dark=(6, 18, 12), lit=(30, 86, 56))


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
    card_blank(out)
    chapter_cosmetics(out)
    badge(out)
    blurred_sheet(sheet, out)
    table(out)
    icons(out)
    level_badges(out)
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
