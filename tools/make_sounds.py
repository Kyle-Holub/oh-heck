#!/usr/bin/env python3
"""
Makes the game's generated sound effects as 16-bit mono WAV files: the button presses, which sound
like a tavern. Most buttons clink like two glasses touching; the buttons that go somewhere (Play,
Continue, Teach me) give a short, soft two-note horn call.

Usage: python3 tools/make_sounds.py [output dir, default assets/sfx]   (needs numpy)
"""
import os
import sys
import wave

import numpy as np

RATE = 22050


def glass(at, length, loud, rng):
    """One touch of glass: a few inharmonic partials ringing down at their own rates, over a tick of noise."""
    t = np.arange(int(RATE * length)) / RATE
    f0 = 2150.0
    out = np.zeros_like(t)
    for ratio, amp, decay in ((1.0, 1.0, 0.16), (2.32, 0.55, 0.09), (4.25, 0.3, 0.05), (6.63, 0.18, 0.03)):
        f = f0 * ratio
        out += amp * np.exp(-t / decay) * (np.sin(2 * np.pi * f * t) + 0.6 * np.sin(2 * np.pi * (f + 3.5) * t))
    tick = rng.normal(0, 1, len(t)) * np.exp(-t / 0.002) * 0.6
    sound = (out + tick) * np.minimum(1, t / 0.0008) * loud
    lead = np.zeros(int(RATE * at))
    return np.concatenate([lead, sound])


def clink():
    rng = np.random.default_rng(3)
    first, second = glass(0, 0.5, 1.0, rng), glass(0.05, 0.45, 0.35, rng)
    n = max(len(first), len(second))
    mix = np.pad(first, (0, n - len(first))) + np.pad(second, (0, n - len(second)))
    return mix / np.abs(mix).max() * 0.32


def horn_note(freq, length):
    """A brass note: harmonics falling off gently, a breathy start that brightens, a little vibrato."""
    t = np.arange(int(RATE * length)) / RATE
    vibrato = 1 + 0.004 * np.sin(2 * np.pi * 5.2 * t) * np.minimum(1, t / 0.08)
    phase = 2 * np.pi * freq * np.cumsum(vibrato) / RATE
    bright = np.minimum(1, t / 0.05)
    out = np.zeros_like(t)
    for n in range(1, 9):
        out += np.sin(n * phase) / n ** (1.6 - 0.7 * bright)
    envelope = np.minimum(1, t / 0.025) * np.minimum(1, (length - t) / 0.06)
    return out * envelope


def horn():
    call = np.concatenate([horn_note(392.0, 0.11), np.zeros(int(RATE * 0.015)), horn_note(523.25, 0.24)])
    return call / np.abs(call).max() * 0.15


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
    for name, samples in (("clink.wav", clink()), ("horn.wav", horn())):
        write(os.path.join(out, name), samples)
        print("wrote", os.path.join(out, name))
