#!/usr/bin/env python3
"""
Bakes TrueType fonts into large BMFont (.fnt + .png) bitmap fonts that the
game draws mipmapped and scaled down, so text stays sharp at any size
without needing libGDX's FreeType natives.

The game's face is Almendra (SIL Open Font License, tools/fonts/OFL.txt), a calligraphic storybook serif;
characters it lacks (the suit signs) come from DejaVu Sans.

Usage: python3 tools/make_fonts.py [output dir, default assets/fonts]   (needs Pillow and the DejaVu fonts)
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
FALLBACK = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONTS = [
    # name, ttf, pixel size, slant (lean added to the upright glyphs; 0 for a true italic)
    ("ui", os.path.join(HERE, "fonts", "Almendra-Regular.ttf"), 44, 0),
    ("serif", os.path.join(HERE, "fonts", "Almendra-Bold.ttf"), 52, 0),
]
CHARS = [chr(c) for c in range(32, 127)] + ["©", "–", "—", "…", "’", "♥", "♠", "♦", "♣"]
PAD = 3
ATLAS_W = 1024


def missing(font, ch):
    """True if the font has no glyph for ch: it draws the same box as a private-use character."""
    def ink(c):
        m = font.getmask(c)
        return m.size, bytes(m)

    return ch != " " and ink(ch) == ink("\ue000")


def bake(name, ttf, size, slant, out_dir):
    font = ImageFont.truetype(ttf, size)
    fallback = ImageFont.truetype(FALLBACK, size)
    ascent, descent = font.getmetrics()
    glyphs = []
    for ch in CHARS:
        face = fallback if missing(font, ch) else font
        adv = round(face.getlength(ch))
        # render on a roomy canvas with the baseline origin at (size, 2*size), then crop to the ink
        canvas = Image.new("L", (size * 4, size * 3), 0)
        ImageDraw.Draw(canvas).text((size, 2 * size), ch, font=face, fill=255, anchor="ls")
        if slant:
            canvas = canvas.transform(canvas.size, Image.AFFINE, (1, slant, -slant * 2 * size, 0, 1, 0), Image.BICUBIC)
        ink = canvas.getbbox()
        if ink is None:
            glyphs.append((ch, None, 0, 0, 0, 0, adv))
            continue
        img = canvas.crop(ink)
        x0, y0 = ink[0] - size, ink[1] - 2 * size
        glyphs.append((ch, img, x0, y0, img.width, img.height, adv))

    # shelf-pack the glyphs
    x = y = PAD
    row_h = 0
    places = []
    for ch, img, x0, y0, w, h, adv in glyphs:
        if x + w + PAD > ATLAS_W:
            x = PAD
            y += row_h + PAD
            row_h = 0
        places.append((x, y))
        x += w + PAD
        row_h = max(row_h, h)
    atlas_h = 1
    while atlas_h < y + row_h + PAD:
        atlas_h *= 2

    atlas = Image.new("RGBA", (ATLAS_W, atlas_h), (255, 255, 255, 0))
    lines = []
    for (ch, img, x0, y0, w, h, adv), (px, py) in zip(glyphs, places):
        if img is not None:
            white = Image.new("RGBA", (w, h), (255, 255, 255, 255))
            white.putalpha(img)
            atlas.paste(white, (px, py))
        lines.append(
            "char id=%d x=%d y=%d width=%d height=%d xoffset=%d yoffset=%d xadvance=%d page=0 chnl=15"
            % (ord(ch), px if img else 0, py if img else 0, w, h, x0, ascent + y0, adv)
        )

    png = name + ".png"
    atlas.save(os.path.join(out_dir, png))
    with open(os.path.join(out_dir, name + ".fnt"), "w") as f:
        f.write('info face="%s" size=%d bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 '
                'padding=0,0,0,0 spacing=%d,%d\n' % (name, size, PAD, PAD))
        f.write("common lineHeight=%d base=%d scaleW=%d scaleH=%d pages=1 packed=0\n"
                % (ascent + descent, ascent, ATLAS_W, atlas_h))
        f.write('page id=0 file="%s"\n' % png)
        f.write("chars count=%d\n" % len(lines))
        f.write("\n".join(lines) + "\n")
    print("baked", name, ATLAS_W, "x", atlas_h)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "assets/fonts"
    os.makedirs(out, exist_ok=True)
    for n, t, s, lean in FONTS:
        bake(n, t, s, lean, out)
