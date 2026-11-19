#!/usr/bin/env python3
"""
Screenshot tools: fetch a run's screenshots, compare two sets, and make the phone view.

    shots.py fetch <tag | branch> <dir>     a release's screenshots; for a branch, its newest preview
    shots.py compare <base dir> <new dir> [--out <dir>] [--report <file>]
    shots.py phone <dir> <out.png>          table shots at a phone's physical size
    shots.py review [<branch>]              fetch the branch's newest preview and the newest dev
                                            build, compare, and make the phone view (in build/dev/shots)

compare sorts each shot into one of:
  same      no visible difference
  random    differs only where the run is random (sparkles, fireworks, the title shower,
            the motes round the trump card): nothing to check
  CHANGED   differs elsewhere: look at it (--out writes before/after pictures)
and exits 1 if anything CHANGED. A change you meant is fine; one you didn't is a bug.

Needs Pillow and NumPy, and gh for fetch and review.
"""
import json
import os
import subprocess
import sys

import numpy as np
from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))

# A pixel counts as changed when a channel moves more than this (0-255): below it is
# rounding and blending noise between runs.
THRESHOLD = 12
# A difference this small (pixels) is noise, not a change.
NOISE_PX = 16

# Shots that are random all over: every difference is "random".
RANDOM_ALL = ("intro", "title", "tutorial-13", "gameover")
# Shots with sparkles drifting over them: up to this share of the picture may differ.
SPARKLY = ("tutorial-",)
SPARKLE_SHARE = 0.015
# Areas where effects are random in every table shot, as fractions of the picture
# (x0, y0, x1, y1 from the top left): the motes round the trump card.
RANDOM_AREAS = [(0.89, 0.0, 1.0, 0.24)]
TABLE = ("shuffle", "deal", "bid", "trick", "collect", "handend", "handover", "menu", "play", "tossing")

# The phone view: a 6.3-inch phone held sideways shows the game about 5.8 inches wide;
# at a desktop monitor's 110 pixels per inch that's 640 pixels.
PHONE_W = 640


def state(name):
    """'18-deal-3.png' -> 'deal-3'."""
    base = os.path.splitext(name)[0]
    return base.split("-", 1)[1] if base[:2].isdigit() and "-" in base else base


def shots_in(d):
    """The screenshots in a folder, by state."""
    return {state(f): os.path.join(d, f) for f in sorted(os.listdir(d))
            if f.endswith(".png") and not f.startswith(("phone", "compare"))}


def diff_mask(a, b):
    return np.abs(a.astype(int) - b.astype(int)).max(axis=2) > THRESHOLD


def classify(st, a, b):
    """('same' | 'random' | 'CHANGED', changed pixels, box)."""
    if a.shape != b.shape:
        return "CHANGED", -1, "size differs"
    m = diff_mask(a, b)
    total = int(m.sum())
    if total <= NOISE_PX:
        return "same", total, ""
    ys, xs = np.nonzero(m)
    box = "x %d-%d y %d-%d" % (xs.min(), xs.max(), ys.min(), ys.max())
    if st.startswith(RANDOM_ALL):
        return "random", total, box
    if st.startswith(SPARKLY) and total <= SPARKLE_SHARE * m.size:
        return "random", total, box
    if st.startswith(TABLE):
        h, w = m.shape
        for x0, y0, x1, y1 in RANDOM_AREAS:
            m[int(y0 * h):int(y1 * h), int(x0 * w):int(x1 * w)] = False
        if not m.any():
            return "random", total, box
        ys, xs = np.nonzero(m)
        total = int(m.sum())
        box = "x %d-%d y %d-%d" % (xs.min(), xs.max(), ys.min(), ys.max())
    return "CHANGED", total, box


def before_after(a, b, out):
    """Before above after, with the changed pixels outlined in magenta on the after."""
    h, w, _ = a.shape
    m = diff_mask(a, b)
    after = b.copy()
    edge = m & ~np.roll(m, 1, 0) | m & ~np.roll(m, 1, 1)
    after[edge] = (255, 0, 255)
    pic = np.concatenate([a, np.full((8, w, 3), 40, np.uint8), after], axis=0)
    Image.fromarray(pic).resize((w // 2, (2 * h + 8) // 2), Image.LANCZOS).save(out)


def compare(base_dir, new_dir, out_dir=None, report=None):
    base, new = shots_in(base_dir), shots_in(new_dir)
    lines, changed = [], 0
    for st, p in new.items():
        if st not in base:
            lines.append("%-8s %s (no shot to compare with)" % ("new", st))
            continue
        a = np.asarray(Image.open(base[st]).convert("RGB"))
        b = np.asarray(Image.open(p).convert("RGB"))
        kind, px, box = classify(st, a, b)
        lines.append("%-8s %-14s %s" % (kind, st, ("%d px  %s" % (px, box)) if box else ""))
        if kind == "CHANGED":
            changed += 1
            if out_dir and a.shape == b.shape:
                os.makedirs(out_dir, exist_ok=True)
                before_after(a, b, os.path.join(out_dir, "compare-%s.png" % st))
    lines.append("%d changed, %d compared" % (changed, len(new)))
    text = "\n".join(lines)
    print(text)
    if report:
        with open(report, "w") as f:
            f.write(text + "\n")
    return changed


def phone(d, out):
    """The table shots at a phone's size, two to a row, labelled."""
    from PIL import ImageDraw
    shots = [(st, p) for st, p in shots_in(d).items() if st.startswith(TABLE)]
    if not shots:
        print("no table shots in", d)
        return
    first = Image.open(shots[0][1])
    w, h = PHONE_W, round(first.height * PHONE_W / first.width)
    cols, gap, label = 2, 12, 18
    rows = (len(shots) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * w + (cols + 1) * gap, rows * (h + label + gap) + gap), (24, 24, 24))
    d = ImageDraw.Draw(sheet)
    for i, (st, p) in enumerate(shots):
        x = gap + (i % cols) * (w + gap)
        y = gap + (i // cols) * (h + label + gap)
        d.text((x, y), st, fill=(200, 200, 200))
        sheet.paste(Image.open(p).convert("RGB").resize((w, h), Image.LANCZOS), (x, y + label))
    sheet.save(out)
    print("wrote", out, "(view it at 100% on a desktop screen: about a phone's size)")


def gh_json(path):
    return json.loads(subprocess.check_output(["gh", "api", path], cwd=ROOT))


def fetch(what, d):
    """Downloads a release's PNGs: what is a tag, or a branch (its newest preview)."""
    releases = gh_json("repos/{owner}/{repo}/releases?per_page=100")
    rel = next((r for r in releases if r["tag_name"] == what), None)
    if rel is None:  # a branch: its newest preview, found by the commit it was made from
        sha = subprocess.check_output(["git", "rev-parse", "origin/" + what], cwd=ROOT, text=True).strip()
        rel = next((r for r in releases if r["draft"] and r["target_commitish"] == sha), None)
        if rel is None:
            sys.exit("no preview for %s at %s yet (still running, or not pushed?)" % (what, sha[:8]))
    os.makedirs(d, exist_ok=True)
    for f in os.listdir(d):
        if f.endswith((".png", ".txt")):
            os.remove(os.path.join(d, f))
    for a in rel["assets"]:
        if a["name"].endswith((".png", ".txt")):
            data = subprocess.check_output(["gh", "api", "-H", "Accept: application/octet-stream",
                                            "repos/{owner}/{repo}/releases/assets/%d" % a["id"]], cwd=ROOT)
            with open(os.path.join(d, a["name"]), "wb") as f:
                f.write(data)
    print("fetched", rel["tag_name"], "into", d)
    return rel["tag_name"]


def newest_dev():
    releases = gh_json("repos/{owner}/{repo}/releases?per_page=30")
    return next(r["tag_name"] for r in releases if not r["draft"] and r["tag_name"].startswith("dev-"))


def main(args):
    if not args or args[0] in ("-h", "--help"):
        print(__doc__)
        return 0
    cmd = args[0]
    if cmd == "fetch" and len(args) == 3:
        fetch(args[1], args[2])
        return 0
    if cmd == "compare" and len(args) >= 3:
        opts = dict(zip(args[3::2], args[4::2]))
        return 1 if compare(args[1], args[2], opts.get("--out"), opts.get("--report")) else 0
    if cmd == "phone" and len(args) == 3:
        phone(args[1], args[2])
        return 0
    if cmd == "review":
        branch = args[1] if len(args) > 1 else subprocess.check_output(
            ["git", "branch", "--show-current"], cwd=ROOT, text=True).strip()
        top = os.path.join(ROOT, "build", "dev", "shots")
        fetch(newest_dev(), os.path.join(top, "base"))
        fetch(branch, os.path.join(top, "new"))
        phone(os.path.join(top, "new"), os.path.join(top, "phone-view.png"))
        return 1 if compare(os.path.join(top, "base"), os.path.join(top, "new"), os.path.join(top, "changed")) else 0
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
