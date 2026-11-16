#!/usr/bin/env python3
"""
Bakes TrueType fonts into large BMFont (.fnt + .png) bitmap fonts that the
game draws mipmapped and scaled down, so text stays sharp at any size
without needing libGDX's FreeType natives.

Usage: python3 make_fonts.py [output dir]   (needs Pillow and the DejaVu fonts)
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

FONTS = [
    # name, ttf, pixel size
    ("ui", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 42),
    ("serif", "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf", 52),
]
CHARS = [chr(c) for c in range(32, 127)] + ["©", "–", "—", "…", "’", "♥", "♠", "♦", "♣"]
PAD = 3
ATLAS_W = 1024


def bake(name, ttf, size, out_dir):
    font = ImageFont.truetype(ttf, size)
    ascent, descent = font.getmetrics()
    glyphs = []
    for ch in CHARS:
        adv = round(font.getlength(ch))
        # render on a roomy canvas with the baseline origin at (size, 2*size), then crop to the ink
        canvas = Image.new("L", (size * 4, size * 3), 0)
        ImageDraw.Draw(canvas).text((size, 2 * size), ch, font=font, fill=255, anchor="ls")
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
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    for n, t, s in FONTS:
        bake(n, t, s, out)
