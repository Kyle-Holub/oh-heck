#!/usr/bin/env python3
"""
Draws the Android launcher icon in the game's palette: the ace of spades and a
card back fanned on a dark spotlit background with a few gold sparkles.

Writes an adaptive icon (background + foreground layers, so launchers can mask
it to any shape), a one-colour version for Android 13 themed icons, and plain
icons for older Android versions.

Usage: python3 tools/make_icon.py [images dir, default assets/images] [res dir, default android/res]
(run make_art.py first; needs Pillow and NumPy)
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")
RES = os.path.join(ROOT, "android", "res")
IMAGES = os.path.join(ROOT, "assets", "images")

S = 432  # 108 dp at xxxhdpi; the layers are drawn at this size and scaled down
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
CARD_W, CARD_H = 219, 294


def background():
    y, x = np.mgrid[0:S, 0:S].astype(float)
    r = np.sqrt((x - S * 0.5) ** 2 + (y - S * 0.42) ** 2) / (S * 0.62)
    light = np.clip(1 - r, 0, 1) ** 1.4
    dark, lit = np.array([22, 13, 9], float), np.array([104, 64, 40], float)
    img = dark + (lit - dark) * light[..., None]
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    # a few soft gold sparkles
    rng = np.random.default_rng(3)
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(glow)
    for _ in range(26):
        cx, cy = rng.uniform(0, S), rng.uniform(0, S)
        rad = rng.uniform(2, 7)
        a = int(rng.uniform(70, 170))
        d.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=(255, 205, 110, a))
    out.alpha_composite(glow.filter(ImageFilter.GaussianBlur(2.2)))
    return out


def rotated_card(card, height, angle):
    w = int(card.width * height / card.height)
    c = card.resize((w, height), Image.LANCZOS)
    return c.rotate(angle, resample=Image.BICUBIC, expand=True)


def foreground():
    sheet = Image.open(os.path.join(IMAGES, "cards_hd.png")).convert("RGBA")
    ace = sheet.crop((0, CARD_H, CARD_W, 2 * CARD_H))  # row 1 = spades, column 0 = ace
    back = Image.open(os.path.join(IMAGES, "back_hd.png")).convert("RGBA")
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    h = 214  # cards stay inside the 66 dp safe zone that every mask shape keeps
    for card, angle, dx, dy in ((back, 14, -34, 6), (ace, -9, 30, -2)):
        c = rotated_card(card, h, angle)
        x, y = S // 2 - c.width // 2 + dx, S // 2 - c.height // 2 + dy
        shadow = Image.new("RGBA", c.size, (0, 0, 0, 0))
        shadow.putalpha(c.getchannel("A").point(lambda v: int(v * 0.6)))
        layer.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(9)), (x + 6, y + 10))
        layer.alpha_composite(c, (x, y))
    return layer


def monochrome():
    """A spade silhouette for themed icons (Android tints it)."""
    img = Image.new("L", (S * 4, S * 4), 0)
    d = ImageDraw.Draw(img)
    cx, cy, s = S * 2, S * 2, S * 4 * 0.42

    def P(x, y):
        return (cx + x * s, cy + y * s)

    def circle(x, y, r):
        d.ellipse([cx + (x - r) * s, cy + (y - r) * s, cx + (x + r) * s, cy + (y + r) * s], fill=255)

    circle(-0.215, 0.08, 0.235)
    circle(0.215, 0.08, 0.235)
    d.polygon([P(-0.445, 0.02), P(0.445, 0.02), P(0, -0.5)], fill=255)
    d.polygon([P(-0.2, 0.18), P(0.2, 0.18), P(0, -0.1)], fill=255)
    d.polygon([P(0, 0.12), P(-0.17, 0.5), P(0.17, 0.5)], fill=255)
    out = Image.new("RGBA", (S, S), (255, 255, 255, 0))
    out.putalpha(img.resize((S, S), Image.LANCZOS))
    return out


def legacy(bg, fg, size):
    """For Android 7 and older: the middle 72 dp of the layers in a rounded square."""
    full = bg.copy()
    full.alpha_composite(fg)
    m = int(S * 18 / 108)
    crop = full.crop((m, m, S - m, S - m)).resize((size * 4, size * 4), Image.LANCZOS)
    mask = Image.new("L", crop.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, crop.width - 1, crop.height - 1], int(crop.width * 0.18), fill=255)
    crop.putalpha(mask)
    return crop.resize((size, size), Image.LANCZOS)


def main():
    global IMAGES, RES
    if len(sys.argv) > 1:
        IMAGES = sys.argv[1]
    if len(sys.argv) > 2:
        RES = sys.argv[2]
    bg, fg, mono = background(), foreground(), monochrome()
    for name, scale in DENSITIES.items():
        d = os.path.join(RES, "mipmap-" + name)
        os.makedirs(d, exist_ok=True)
        layer = int(108 * scale)
        bg.resize((layer, layer), Image.LANCZOS).save(os.path.join(d, "ic_launcher_background.png"))
        fg.resize((layer, layer), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.png"))
        mono.resize((layer, layer), Image.LANCZOS).save(os.path.join(d, "ic_launcher_monochrome.png"))
        legacy(bg, fg, int(48 * scale)).save(os.path.join(d, "ic_launcher.png"))
    d = os.path.join(RES, "mipmap-anydpi-v26")
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, "ic_launcher.xml"), "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                '  <background android:drawable="@mipmap/ic_launcher_background" />\n'
                '  <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n'
                '  <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />\n'
                '</adaptive-icon>\n')
    # a preview of how launchers show it: circle and rounded square masks
    full = bg.copy()
    full.alpha_composite(fg)
    prev = Image.new("RGBA", (S * 3 + 40, S + 20), (240, 240, 240, 255))
    for i, shape in enumerate(["circle", "squircle"]):
        m = Image.new("L", (S, S), 0)
        inset = int(S * 9 / 108)
        if shape == "circle":
            ImageDraw.Draw(m).ellipse([inset, inset, S - inset, S - inset], fill=255)
        else:
            ImageDraw.Draw(m).rounded_rectangle([inset, inset, S - inset, S - inset], int(S * 0.24), fill=255)
        t = full.copy()
        t.putalpha(m)
        prev.alpha_composite(t, (10 + i * (S + 10), 10))
    prev.alpha_composite(legacy(bg, fg, S - 60), (10 + 2 * (S + 10) + 30, 40))
    prev.save(os.path.join(os.environ.get("TMPDIR", "/tmp"), "icon_preview.png"))
    print("icon written")


if __name__ == "__main__":
    main()
