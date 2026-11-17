"""Court card art (Jack, Queen, King) in the traditional style, drawn from shapes in the game's palette.

Each figure is drawn once, head in profile and shoulders down past the middle of the card,
then cut along a diagonal through the centre and turned half a turn for the other end, as
on a real court card. Every area is a flat colour or a printed pattern (scrollwork,
checks, sawtooth, ermine...) inside an ink outline, like the woodcut-printed English cards.
The suit decides which colours and patterns go where; the rank decides the headwear, hair,
beard and what the figure holds.
"""

import math

from PIL import Image, ImageDraw

INK = (30, 27, 24)
CREAM = (243, 232, 210)
PAPER = (236, 222, 194)
GOLD = (210, 168, 82)
GOLD_LIGHT = (240, 206, 128)
GOLD_DARK = (140, 104, 44)
RED = (150, 28, 31)
OXBLOOD = (104, 18, 20)
NAVY = (38, 50, 84)
SKIN = (240, 214, 182)

HAIR = {"J": (196, 146, 66), "Q": (92, 56, 30), "K": (226, 220, 206)}  # fair, dark, grey
STRAND = {"J": GOLD_DARK, "Q": INK, "K": NAVY}

W, H = 116, 192  # the art box, in card pixels
HEAD_ZOOM, HEAD_DROP = 1.15, 5  # the head and its props, larger than the robe they're drawn with
SEAM = 12  # the cut runs from (0, H/2 + SEAM) to (W, H/2 - SEAM)


def reds(suit):
    return suit in ("hearts", "diamonds")


class Pen:
    """Draws in card-pixel coordinates on an image k times larger."""

    def __init__(self, img, k):
        self.img = img
        self.d = ImageDraw.Draw(img)
        self.k = k
        self.line = max(1, int(1.15 * k))
        self.zoom(1)

    def zoom(self, s, ox=0.0, oy=0.0, dy=0.0):
        """Draws what follows s times larger about (ox, oy), moved down dy (1 = as given)."""
        self.s, self.ox, self.oy, self.dy = s, ox, oy, dy

    def x(self, x):
        return (self.ox + (x - self.ox) * self.s) * self.k

    def y(self, y):
        return (self.oy + (y - self.oy) * self.s + self.dy) * self.k

    def p(self, pts):
        return [(self.x(x), self.y(y)) for x, y in pts]

    def poly(self, pts, fill, outline=INK):
        if fill is not None:
            self.d.polygon(self.p(pts), fill=fill)
        if outline:
            self.d.line(self.p(pts + [pts[0], pts[1]]), fill=outline, width=self.line, joint="curve")

    def patterned(self, pts, pattern, outline=INK):
        """Fills the polygon with a pattern (a function drawing on a Pen over the whole box)."""
        layer = Image.new("RGBA", self.img.size, (0, 0, 0, 0))
        pattern(Pen(layer, self.k))
        mask = Image.new("L", self.img.size, 0)
        ImageDraw.Draw(mask).polygon(self.p(pts), fill=255)
        self.img.paste(layer, (0, 0), mask)
        self.poly(pts, None, outline)

    def ellipse(self, cx, cy, rx, ry, fill, outline=INK):
        k, X, Y = self.k * self.s, self.x(cx), self.y(cy)
        self.d.ellipse([X - rx * k, Y - ry * k, X + rx * k, Y + ry * k], fill=fill,
                       outline=outline, width=self.line if outline else 0)

    def stroke(self, pts, fill=INK, width=1.0):
        self.d.line(self.p(pts), fill=fill, width=max(1, int(self.line * width)), joint="curve")

    def arc(self, cx, cy, r, a0, a1, fill=INK, width=1.0):
        k, X, Y = self.k * self.s, self.x(cx), self.y(cy)
        self.d.arc([X - r * k, Y - r * k, X + r * k, Y + r * k], a0, a1, fill=fill,
                   width=max(1, int(self.line * width)))


# ---- printed patterns, each covering the whole box (clipped to a shape by Pen.patterned)

def solid(colour):
    def draw(p):
        p.d.rectangle([0, 0, p.img.size[0], p.img.size[1]], fill=colour)
    return draw


def scrollwork(base, ink):
    """Rows of curling C-scrolls, like the gold work on a robe."""
    def draw(p):
        solid(base)(p)
        for row, y in enumerate(range(-4, H + 8, 9)):
            for x in range(-6 + (row % 2) * 6, W + 8, 12):
                p.arc(x, y, 3.2, 200, 470, fill=ink, width=1.3)
                p.ellipse(x + 2.6, y - 1.4, 0.9, 0.9, ink, outline=None)
    return draw


def checks(a, b, size=3.2):
    """A diagonal chequer."""
    def draw(p):
        solid(a)(p)
        for i in range(-60, 60):
            for j in range(-60, 60):
                if (i + j) % 2 == 0:
                    cx, cy = (i - j) * size, (i + j) * size
                    if -size < cx < W + size and -size < cy < H + size:
                        p.poly([(cx, cy - size), (cx + size, cy), (cx, cy + size), (cx - size, cy)], b, outline=None)
    return draw


def sawtooth(base, teeth, size=4.0):
    """Rows of triangles."""
    def draw(p):
        solid(base)(p)
        for y in range(0, H + 8, int(size * 2)):
            for x in range(-8, W + 8, int(size * 2)):
                p.poly([(x, y + size), (x + size, y), (x + 2 * size, y + size)], teeth, outline=None)
    return draw


def ermine(p):
    """White fur with black tails."""
    solid(CREAM)(p)
    for row, y in enumerate(range(0, H + 6, 6)):
        for x in range(-6 + (row % 2) * 4, W + 6, 8):
            p.poly([(x, y - 1.6), (x + 1.1, y + 1.4), (x - 1.1, y + 1.4)], INK, outline=None)
            p.ellipse(x, y - 2.2, 0.7, 0.7, INK, outline=None)


def dots(base, dot, step=5.0):
    def draw(p):
        solid(base)(p)
        for row, y in enumerate(range(0, H + 6, int(step))):
            for x in range(-6 + (row % 2) * int(step / 2), W + 6, int(step)):
                p.ellipse(x, y, 0.9, 0.9, dot, outline=None)
    return draw


def stripes(base, stripe, step=4.0):
    def draw(p):
        solid(base)(p)
        for x in range(-H, W + H, int(step)):
            p.stroke([(x, 0), (x + H, H)], fill=stripe, width=1.6)
    return draw


# ---- the figure

def court_art(rank, suit, k, suit_shape):
    """The whole art box for a court card at k times card resolution, background included."""
    top = Image.new("RGBA", (W * k, H * k), (0, 0, 0, 0))
    figure(Pen(top, k), rank, suit, suit_shape)
    # keep the part above the diagonal cut; the half turn fills the rest
    mask = Image.new("L", top.size, 0)
    ImageDraw.Draw(mask).polygon([(0, 0), (W * k, 0), (W * k, (H / 2 - SEAM) * k), (0, (H / 2 + SEAM) * k)],
                                 fill=255)
    half = Image.new("RGBA", top.size, (0, 0, 0, 0))
    half.paste(top, (0, 0), mask)
    art = Image.new("RGBA", top.size, PAPER + (255,))
    lattice = ImageDraw.Draw(art)  # a faint lattice behind the figure
    for i in range(-H, W + H, 12):
        lattice.line([(i * k, 0), ((i + H) * k, H * k)], fill=(226, 208, 176, 255), width=k)
        lattice.line([(i * k, 0), ((i - H) * k, H * k)], fill=(226, 208, 176, 255), width=k)
    art.alpha_composite(half)
    art.alpha_composite(half.rotate(180))
    return art


def figure(p, rank, suit, suit_shape):
    red = reds(suit)
    main = RED if red else NAVY  # the robe's leading colour
    # patterns by suit, so no two suits dress alike
    robe_left = {"hearts": scrollwork(RED, GOLD_LIGHT), "diamonds": stripes(RED, OXBLOOD),
                 "spades": scrollwork(NAVY, GOLD), "clubs": dots(NAVY, GOLD_LIGHT)}[suit]
    band = {"hearts": checks(GOLD_LIGHT, INK), "diamonds": sawtooth(GOLD_LIGHT, INK),
            "spades": checks(GOLD_LIGHT, RED), "clubs": sawtooth(GOLD_LIGHT, RED)}[suit]
    robe_right = {"hearts": sawtooth(NAVY, CREAM), "diamonds": scrollwork(NAVY, GOLD),
                  "spades": sawtooth(RED, CREAM), "clubs": scrollwork(RED, GOLD_LIGHT)}[suit]

    robe(p, rank, main, robe_left, band, robe_right)
    p.zoom(HEAD_ZOOM, 64, 60, HEAD_DROP)  # the head, crown and what's held, a size up
    held_behind(p, rank)
    hair_and_head(p, rank)
    crown(p, rank, main)
    held_front(p, rank, red)
    p.zoom(1)
    # the big suit sign, top left, as on a real court card
    suit_shape(p.d, suit, 21 * p.k, 26 * p.k, 26 * p.k, RED if red else INK)


def held_behind(p, rank):
    """What the figure holds, raised behind the head on the right."""
    if rank == "K":  # a sword held up behind the crown
        p.poly([(98, 0), (101, 6), (101, 52), (95, 52), (95, 6)], (214, 212, 200))
        p.stroke([(98, 7), (98, 50)], fill=(160, 158, 150), width=0.8)
        p.poly([(88, 52), (108, 52), (108, 57), (88, 57)], GOLD)
        p.poly([(96, 57), (100, 57), (100, 70), (96, 70)], OXBLOOD)
        p.ellipse(98, 72, 3, 3, GOLD)
    elif rank == "J":  # a halberd: shaft, spike and axe head
        p.poly([(98, 8), (101, 8), (101, 120), (98, 120)], GOLD_DARK)
        p.poly([(99.5, 0), (103, 9), (99.5, 13), (96, 9)], (214, 212, 200))
        p.poly([(101, 12), (113, 6), (115, 18), (112, 26), (101, 22)], (214, 212, 200))
        p.arc(113, 16, 6, 300, 60, width=0.8)


def robe(p, rank, main, robe_left, band, robe_right):
    # the shoulders and robe run down past the middle (the diagonal cut trims them)
    left = [(6, 70), (20, 60), (44, 56), (58, 66), (52, 130), (0, 130), (0, 78)]
    right = [(74, 62), (96, 58), (112, 66), (116, 74), (116, 130), (70, 130)]
    p.patterned(left, robe_left)
    p.patterned(right, robe_right)
    # the gold band down the front, between them
    p.patterned([(58, 66), (74, 62), (70, 130), (52, 130)], band)
    # ermine trims: along the left shoulder and down the right edge of the front
    p.patterned([(6, 70), (20, 60), (44, 56), (46, 62), (22, 67), (8, 76)], ermine)
    p.patterned([(70, 64), (77, 62), (73, 130), (66, 130)], ermine)
    # a sawtooth border down the left robe (nothing runs across the middle, where the halves meet)
    p.patterned([(4, 84), (12, 80), (8, 130), (0, 130), (0, 86)], sawtooth(NAVY, CREAM, 3.0))
    # a medallion on the chest
    p.ellipse(64, 80, 5.5, 5.5, GOLD)
    p.ellipse(64, 80, 2.6, 2.6, main)
    if rank == "Q":  # a necklace of pearls
        for i in range(9):
            a = math.pi * (0.15 + 0.7 * i / 8)
            p.ellipse(64 - 14 * math.cos(a), 60 + 9 * math.sin(a), 1.3, 1.3, CREAM, outline=INK)


def hair_and_head(p, rank):
    hair, strand = HAIR[rank], STRAND[rank]
    # hair falling behind the head to the shoulders, in strands
    back = [(64, 20), (84, 22), (90, 40), (92, 60), (84, 66), (72, 64)] if rank != "Q" else \
        [(62, 20), (86, 22), (94, 44), (98, 72), (86, 78), (72, 70)]
    p.poly(back, hair)
    for i in range(6):
        x = 70 + i * 3.6
        p.stroke([(x, 26), (x + 4, 44), (x + 3, 62 if rank != "Q" else 72)], fill=strand, width=0.7)
    # the face in profile, looking left
    face = [(66, 22), (56, 24), (52, 31), (51, 36), (45.5, 42.5), (50.5, 44.5), (49.5, 47.5), (52.5, 49),
            (51, 52), (53, 56), (60, 59), (68, 58), (74, 50), (76, 34), (72, 24)]
    p.poly([(60, 56), (70, 56), (72, 66), (58, 66)], SKIN)  # neck
    p.poly(face, SKIN)
    # eye, brow, nostril and mouth
    p.poly([(55, 35.5), (58.5, 34), (61, 35.6), (58, 37)], CREAM, outline=INK)
    p.ellipse(57.4, 35.5, 0.9, 0.9, NAVY, outline=None)
    p.stroke([(54, 32), (58, 30.4), (62, 31.6)], width=1.1)
    p.stroke([(49.5, 42.5), (52, 43.6)], width=0.8)
    p.stroke([(50.5, 47.4), (53.5, 47.6)], fill=OXBLOOD, width=1.2)
    if rank == "K":  # a curled moustache and a full beard in curls
        p.poly([(48, 46), (53, 44.5), (58, 47), (56, 49.5), (51, 48.5)], HAIR[rank])
        p.arc(47, 48, 2, 90, 360, width=0.9)
        beard = [(52, 49), (60, 48), (70, 54), (72, 62), (66, 72), (58, 74), (52, 68), (50, 58)]
        p.poly(beard, hair)
        for (x, y) in [(55, 56), (61, 58), (66, 62), (57, 64), (63, 68)]:
            p.arc(x, y, 2.4, 180, 450, fill=strand, width=0.8)
    else:
        p.ellipse(56, 45, 2.4, 1.6, (232, 170, 142), outline=None)  # a touch of colour in the cheek
    # an ear and a lock of hair in front of it
    p.ellipse(68, 40, 2.4, 3.6, SKIN)
    p.poly([(64, 22), (74, 24), (76, 40), (72, 46), (70, 32), (64, 28)], hair)
    for i in range(3):
        p.stroke([(66 + i * 2.5, 25), (71 + i * 1.5, 34), (72, 44)], fill=strand, width=0.7)


def crown(p, rank, main):
    if rank == "J":  # a cap with a rolled brim and a feather
        p.poly([(52, 24), (54, 12), (66, 6), (80, 10), (84, 22)], main)
        p.patterned([(50, 26), (52, 20), (86, 20), (86, 27)], checks(GOLD_LIGHT, INK, 2.2))
        p.poly([(78, 10), (96, 0), (100, 2), (86, 14)], CREAM)
        for i in range(4):
            p.stroke([(82 + i * 4, 9 - i * 2.2), (84 + i * 4, 12 - i * 2.2)], width=0.6)
        return
    tall = rank == "K"
    top = 2 if tall else 10
    # the cap inside the crown, then the points, then the gold band with jewels
    p.poly([(52, 22), (54, top + 6), (68, top + 2), (84, top + 6), (86, 22)], main)
    for x in (54, 69, 84):  # fleur points: a trefoil on a stalk
        p.poly([(x - 3, 18), (x, top + 6), (x + 3, 18)], GOLD)
        p.ellipse(x, top + 4, 2.4, 2.4, GOLD_LIGHT)
        p.ellipse(x - 2.6, top + 6.5, 1.6, 1.6, GOLD_LIGHT)
        p.ellipse(x + 2.6, top + 6.5, 1.6, 1.6, GOLD_LIGHT)
    p.poly([(50, 18), (88, 18), (88, 25), (50, 25)], GOLD)
    for x in range(53, 87, 4):
        p.ellipse(x, 21.5, 1.1, 1.1, INK, outline=None)
    if not tall:  # a veil behind the Queen's crown
        p.poly([(84, 20), (94, 26), (96, 46), (88, 40)], CREAM)


def held_front(p, rank, red):
    """The hand, and anything held in front."""
    if rank == "K":  # his hand on the sword's grip
        p.ellipse(98, 63, 5, 4.5, SKIN)
        for i in range(3):
            p.stroke([(95 + i * 2.4, 60), (95 + i * 2.4, 64)], width=0.6)
    elif rank == "Q":  # a flower held up before her
        p.stroke([(30, 92), (34, 74), (38, 58)], fill=(60, 96, 52), width=2)
        p.poly([(34, 74), (26, 70), (33, 68)], (84, 124, 66))
        for a in range(5):
            ang = a * 2 * math.pi / 5
            p.ellipse(38 + 4.2 * math.cos(ang), 53 + 4.2 * math.sin(ang), 3.2, 3.2, NAVY if red else RED)
        p.ellipse(38, 53, 2.2, 2.2, GOLD_LIGHT)
        p.ellipse(30, 92, 5, 4.2, SKIN)
    else:  # his fist round the halberd's shaft
        p.ellipse(99.5, 66, 4.6, 5, SKIN)
