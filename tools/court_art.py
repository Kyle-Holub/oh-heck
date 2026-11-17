"""Court card figures (Jack, Queen, King), drawn from shapes in the game's palette.

Each figure is half a card, head and shoulders, then mirrored top to bottom like a
real court card. Outlined in ink, like a woodcut print; the suit decides the robe's
colours and pattern, the rank the headwear, hair and what the figure holds.
"""

from PIL import Image, ImageDraw

INK = (30, 27, 24)
CREAM = (243, 232, 210)
GOLD = (210, 168, 82)
GOLD_LIGHT = (240, 206, 128)
GOLD_DARK = (140, 104, 44)
RED = (150, 28, 31)
OXBLOOD = (104, 18, 20)
NAVY = (38, 50, 84)
NAVY_LIGHT = (70, 88, 130)
SKIN = (238, 208, 172)
SKIN_SHADE = (214, 176, 140)
STEEL = (196, 196, 188)

HAIR = {"J": (176, 122, 52), "Q": (96, 58, 32), "K": (226, 220, 206)}  # fair, dark, grey


def suit_colours(suit):
    """(main robe colour, second robe colour) for a suit: reds lead with red, blacks with navy."""
    return (RED, NAVY) if suit in ("hearts", "diamonds") else (NAVY, RED)


class Pen:
    """Draws in a unit grid (half-card coordinates, 0..116 by 0..96) scaled up by k."""

    def __init__(self, img, k, line):
        self.d = ImageDraw.Draw(img)
        self.k = k
        self.line = line

    def p(self, pts):
        return [(x * self.k, y * self.k) for x, y in pts]

    def poly(self, pts, fill, outline=INK):
        self.d.polygon(self.p(pts), fill=fill)
        if outline:
            self.d.line(self.p(pts + [pts[0]]), fill=outline, width=self.line, joint="curve")

    def ellipse(self, cx, cy, rx, ry, fill, outline=INK):
        k = self.k
        self.d.ellipse([(cx - rx) * k, (cy - ry) * k, (cx + rx) * k, (cy + ry) * k], fill=fill,
                       outline=outline, width=self.line if outline else 0)

    def stroke(self, pts, fill=INK, width=None):
        self.d.line(self.p(pts), fill=fill, width=width or self.line, joint="curve")

    def clip_rect(self, x0, y0, x1, y1):
        return [x0 * self.k, y0 * self.k, x1 * self.k, y1 * self.k]


W, H = 116, 96  # half the art box, in card pixels


def court_half(rank, suit, k, suit_shape):
    """The top half of a court card's art, k times card resolution (transparent around the figure)."""
    img = Image.new("RGBA", (W * k, H * k), (0, 0, 0, 0))
    pen = Pen(img, k, max(1, int(1.1 * k)))
    main, second = suit_colours(suit)
    side = 1 if rank != "Q" else -1  # which hand holds the item: K and J right, Q left
    cx = 58

    body(pen, rank, suit, cx, main, second)
    held(pen, rank, cx, side)
    head(pen, rank, cx)
    headwear(pen, rank, cx, main)
    held_front(pen, rank, cx, side, main)
    # the suit sign by the head, as on a real court card
    suit_shape(pen.d, suit, (cx - 40 * side) * k, 16 * k, 13 * k, RED if suit in ("hearts", "diamonds") else INK)
    return img


# ---- the figure, back to front

def held(pen, rank, cx, side):
    """The long part of what the figure holds, up past the shoulder."""
    x = cx + 36 * side
    if rank == "K":  # an upright sword
        pen.poly([(x - 2.2, 8), (x, 3), (x + 2.2, 8), (x + 2.2, 70), (x - 2.2, 70)], STEEL)
        pen.stroke([(x, 9), (x, 68)], fill=(150, 150, 144), width=max(1, pen.line // 2))
    elif rank == "J":  # a halberd
        pen.poly([(x - 1.6, 10), (x + 1.6, 10), (x + 1.6, 96), (x - 1.6, 96)], GOLD_DARK)
        pen.poly([(x, 2), (x + 3, 10), (x, 14), (x - 3, 10)], STEEL)
        pen.poly([(x + 1.6, 12), (x + 11 * 1, 9), (x + 12, 17), (x + 9, 22), (x + 1.6, 19)], STEEL)


def body(pen, rank, suit, cx, main, second):
    # broad shoulders, then straight down to the middle of the card (so the mirrored halves make one robe)
    robe = [(cx - 22, 57), (cx + 22, 57), (cx + 44, 66), (cx + 48, 74), (cx + 48, 96), (cx - 48, 96), (cx - 48, 74),
            (cx - 44, 66)]
    pen.poly(robe, main)
    if rank == "J":  # a doublet in two colours
        for s in (-1, 1):
            pen.poly([(cx + 8 * s, 54), (cx + 22 * s, 57), (cx + 44 * s, 66), (cx + 48 * s, 74), (cx + 48 * s, 96),
                      (cx + 8 * s, 96)], second)
    # sleeves: a band of the other colour down each side
    for s in (-1, 1):
        pen.poly([(cx + 36 * s, 63), (cx + 44 * s, 66), (cx + 48 * s, 74), (cx + 48 * s, 96), (cx + 38 * s, 96)],
                 GOLD if rank == "J" else second)
    # the front panel, patterned by suit
    panel = [(cx - 8, 54), (cx + 8, 54), (cx + 11, 96), (cx - 11, 96)]
    pen.poly(panel, GOLD)
    pattern(pen, suit, cx, second)
    pen.poly(panel, None)  # re-ink the panel's edge over the pattern
    # trim down the robe's outer edges
    for s in (-1, 1):
        pen.stroke([(cx + 23 * s, 59), (cx + 35 * s, 64), (cx + 37 * s, 95)], fill=GOLD, width=pen.line * 2)
    # the collar: ermine for the King and Queen, a gold band for the Jack
    if rank == "J":
        pen.poly([(cx - 17, 56), (cx, 64), (cx + 17, 56), (cx + 20, 61), (cx, 70), (cx - 20, 61)], GOLD_LIGHT)
    else:
        pen.poly([(cx - 24, 58), (cx - 10, 52), (cx, 60), (cx + 10, 52), (cx + 24, 58), (cx + 14, 70), (cx, 66),
                  (cx - 14, 70)], CREAM)
        for (x, y) in [(-15, 60), (-8, 63), (8, 63), (15, 60), (0, 63)]:
            pen.stroke([(cx + x, y - 1.5), (cx + x, y + 1.5)], width=pen.line * 2)


def pattern(pen, suit, cx, colour):
    """The front panel's pattern: stripes, chevrons, a lattice or dots, by suit."""
    if suit == "clubs":
        for y in range(60, 96, 6):
            pen.stroke([(cx - 9, y), (cx + 9, y)], fill=colour, width=pen.line * 2)
    elif suit == "spades":
        for y in range(58, 96, 7):
            pen.stroke([(cx - 9, y + 3), (cx, y), (cx + 9, y + 3)], fill=colour, width=pen.line * 2)
    elif suit == "hearts":
        for y in range(60, 96, 9):
            pen.poly([(cx, y - 3.5), (cx + 4, y), (cx, y + 3.5), (cx - 4, y)], colour, outline=None)
    else:
        for y in range(60, 96, 7):
            for x in (-4.5, 4.5):
                pen.ellipse(cx + x, y + (3.5 if x > 0 else 0), 1.8, 1.8, colour, outline=None)


def head(pen, rank, cx):
    hair = HAIR[rank]
    if rank == "Q":  # long hair falling behind the shoulders
        pen.poly([(cx - 16, 26), (cx + 16, 26), (cx + 20, 60), (cx + 8, 62), (cx - 8, 62), (cx - 20, 60)], hair)
    pen.poly([(cx - 5, 48), (cx + 5, 48), (cx + 6, 58), (cx - 6, 58)], SKIN_SHADE)  # neck
    if rank == "K":  # a full beard
        pen.poly([(cx - 13, 36), (cx + 13, 36), (cx + 12, 50), (cx + 5, 58), (cx, 60), (cx - 5, 58), (cx - 12, 50)],
                 hair)
    pen.ellipse(cx, 35, 13, 16, SKIN)
    if rank == "K":  # moustache and beard over the jaw
        pen.poly([(cx - 12, 38), (cx - 6, 42), (cx, 41), (cx + 6, 42), (cx + 12, 38), (cx + 11, 50), (cx + 5, 57),
                  (cx, 59), (cx - 5, 57), (cx - 11, 50)], hair)
        pen.stroke([(cx - 3, 46), (cx, 47), (cx + 3, 46)], fill=OXBLOOD)
    elif rank == "J":  # short curls at the sides
        for s in (-1, 1):
            pen.ellipse(cx + 12 * s, 36, 4, 6, hair)
            pen.ellipse(cx + 11 * s, 44, 3.5, 4, hair)
    # the face
    for s in (-1, 1):
        pen.ellipse(cx + 4.5 * s, 32, 1.6, 1.2, INK, outline=None)
        pen.stroke([(cx + 2.5 * s, 28.8), (cx + 7 * s, 28.4)], width=max(1, pen.line))
    pen.stroke([(cx, 33), (cx - 1.2, 38.5), (cx + 0.8, 39)])
    if rank != "K":
        pen.stroke([(cx - 3.5, 43), (cx, 44.2), (cx + 3.5, 43)], fill=OXBLOOD, width=pen.line + 1)
        for s in (-1, 1):  # a little colour in the cheeks
            pen.ellipse(cx + 7 * s, 39, 2.4, 1.6, (230, 168, 140), outline=None)


def headwear(pen, rank, cx, main):
    if rank == "K":  # a tall crown with three points and gems
        pen.poly([(cx - 14, 22), (cx - 15, 8), (cx - 8, 14), (cx, 3), (cx + 8, 14), (cx + 15, 8), (cx + 14, 22)], GOLD)
        pen.poly([(cx - 15, 19), (cx + 15, 19), (cx + 15, 24), (cx - 15, 24)], GOLD_LIGHT)
        for x in (-8, 0, 8):
            pen.ellipse(cx + x, 21.5, 1.8, 1.8, RED if x == 0 else NAVY, outline=None)
        pen.ellipse(cx, 3, 2, 2, GOLD_LIGHT)
    elif rank == "Q":  # a low crown of fleurs over her hair
        pen.poly([(cx - 14, 23), (cx - 13, 14), (cx - 7, 18), (cx, 11), (cx + 7, 18), (cx + 13, 14), (cx + 14, 23)], GOLD)
        for x in (-13, 0, 13):
            pen.ellipse(cx + x, 13 if x else 10, 2.2, 2.2, GOLD_LIGHT)
        pen.poly([(cx - 14, 21), (cx + 14, 21), (cx + 14, 25), (cx - 14, 25)], GOLD_LIGHT)
        pen.ellipse(cx, 23, 1.6, 1.6, RED, outline=None)
    else:  # a soft cap with a feather
        pen.poly([(cx - 16, 24), (cx - 13, 14), (cx - 2, 9), (cx + 12, 11), (cx + 17, 20), (cx + 15, 25)], main)
        pen.poly([(cx - 16, 22), (cx + 16, 22), (cx + 16, 27), (cx - 16, 27)], GOLD)
        pen.poly([(cx + 10, 12), (cx + 24, 0), (cx + 28, 2), (cx + 15, 15)], CREAM)
        pen.stroke([(cx + 12, 13), (cx + 26, 1)], width=max(1, pen.line // 2))


def held_front(pen, rank, cx, side, main):
    """The hand, and the part of what it holds that shows in front."""
    x = cx + 36 * side
    if rank == "K":  # the sword's hilt and the fist round it
        pen.poly([(x - 8, 66), (x + 8, 66), (x + 8, 70), (x - 8, 70)], GOLD)
        pen.ellipse(x, 76, 5, 5, SKIN)
        pen.ellipse(x, 84, 2.5, 2.5, GOLD)
    elif rank == "Q":  # a flower held up to her shoulder
        pen.stroke([(x, 78), (x - 3 * side, 50)], fill=(70, 110, 60), width=pen.line * 2)
        for a, b in [(-3, 0), (3, 0), (0, -3), (0, 3)]:
            pen.ellipse(x - 3 * side + a, 47 + b, 3.2, 3.2, RED if main == NAVY else NAVY_LIGHT)
        pen.ellipse(x - 3 * side, 47, 2, 2, GOLD_LIGHT)
        pen.ellipse(x, 80, 4.5, 4.5, SKIN)
    else:  # his fist round the halberd's shaft
        pen.ellipse(x, 74, 4.5, 5, SKIN)
