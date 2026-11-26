#!/usr/bin/env python3
"""
Makes the game's generated sound effects (so far just the button press: a soft, bright two-note
chime, like a menu select) as 16-bit mono WAV files.

Usage: python3 tools/make_sounds.py [output dir, default assets/sfx]   (needs numpy)
"""
import os
import sys
import wave

import numpy as np

RATE = 22050


def tone(freq, length, attack=0.004):
    t = np.arange(int(RATE * length)) / RATE
    wave_ = np.sin(2 * np.pi * freq * t) + 0.25 * np.sin(2 * np.pi * freq * 2 * t) + 0.08 * np.sin(2 * np.pi * freq * 3 * t)
    envelope = np.minimum(1, t / attack) * np.exp(-t / (length * 0.45))
    return wave_ * envelope


def press():
    gap = np.zeros(int(RATE * 0.012))
    return np.concatenate([tone(1318.5, 0.055), gap, tone(1975.5, 0.09)]) * 0.35


def write(path, samples):
    data = (np.clip(samples, -1, 1) * 32767).astype("<i2").tobytes()
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "assets/sfx"
    os.makedirs(out, exist_ok=True)
    write(os.path.join(out, "press.wav"), press())
    print("wrote", os.path.join(out, "press.wav"))
