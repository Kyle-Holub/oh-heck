#!/usr/bin/env python3
"""
Rebuilds every generated asset in one go, or checks that the committed ones match.

    python3 tools/build_art.py           # rewrite assets/images, assets/fonts and the launcher icons
    python3 tools/build_art.py --check   # build into a temp folder and list anything that differs

Generated: everything make_art.py draws (assets/images), the bitmap fonts
(assets/fonts) and the launcher icons (android/res/mipmap-*). Not generated: the
hand-lettered word images kept from the original game (HAND_MADE below); they are
source files, edited directly.

Needs: pip install -r tools/requirements.txt (pinned, so output matches byte for byte),
plus the system fonts make_art.py and make_fonts.py draw with (the game faces, Cinzel and EB Garamond, are in tools/fonts):
    Debian/Ubuntu: sudo apt install fonts-dejavu-core fonts-texgyre
"""
import filecmp
import os
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
IMAGES = os.path.join(ROOT, "assets", "images")
FONTS = os.path.join(ROOT, "assets", "fonts")
RES = os.path.join(ROOT, "android", "res")

FONT_SOURCES = {"DejaVu-LICENSE.txt", "Cinzel-OFL.txt", "EBGaramond-OFL.txt"}
HAND_MADE = {"titleText.png", "bidTricks.png", "setText.png", "trickWonText.png", "makeBidText.png", "gameOver.png"}

NEEDED_FONTS = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf",
    "/usr/share/texmf/fonts/opentype/public/tex-gyre/texgyrepagella-bold.otf",
]


def run(script, *args):
    subprocess.run([sys.executable, os.path.join(HERE, script)] + list(args), check=True, cwd=HERE,
                   stdout=subprocess.DEVNULL)


def build(images, fonts, res):
    run("make_art.py", images)
    run("make_fonts.py", fonts)
    run("make_icon.py", images, res)


def differences(built, committed, skip=()):
    """Files that differ, or exist on one side only, between two folder trees."""
    out = []
    for dirpath, _, files in os.walk(built):
        for f in files:
            rel = os.path.relpath(os.path.join(dirpath, f), built)
            other = os.path.join(committed, rel)
            if not os.path.exists(other):
                out.append(rel + " (not committed)")
            elif not filecmp.cmp(os.path.join(dirpath, f), other, shallow=False):
                out.append(rel)
    for dirpath, _, files in os.walk(committed):
        for f in files:
            rel = os.path.relpath(os.path.join(dirpath, f), committed)
            if f not in skip and not os.path.exists(os.path.join(built, rel)):
                out.append(rel + " (no longer generated)")
    return sorted(out)


def main():
    missing = [f for f in NEEDED_FONTS if not os.path.exists(f)]
    if missing:
        sys.exit("Missing fonts:\n  " + "\n  ".join(missing) + "\nInstall fonts-dejavu-core and fonts-texgyre.")
    if "--check" not in sys.argv:
        build(IMAGES, FONTS, RES)
        print("Rebuilt assets/images, assets/fonts and android/res icons.")
        return
    tmp = tempfile.mkdtemp(prefix="ohheck-art-")
    try:
        images, fonts, res = (os.path.join(tmp, n) for n in ("images", "fonts", "res"))
        build(images, fonts, res)
        diff = differences(images, IMAGES, HAND_MADE) + ["fonts/" + d for d in differences(fonts, FONTS, FONT_SOURCES)]
        for d in sorted(os.listdir(res)):
            if d.startswith("mipmap"):
                diff += [d + "/" + x for x in differences(os.path.join(res, d), os.path.join(RES, d))]
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    if diff:
        print("Differs from the committed assets:\n  " + "\n  ".join(diff))
        sys.exit(1)
    print("All generated assets match.")


if __name__ == "__main__":
    main()
