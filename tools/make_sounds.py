#!/usr/bin/env python3
"""
Makes the game's generated sound effects as 16-bit mono WAV files: the button presses, which sound
like a tavern, quietly. Most buttons clink like two glasses touching; the buttons that go somewhere
(Play, Continue, Teach me) sound like a tankard set down on a wooden table. The table's accents keep to
the same old-tavern palette: wood, felt, paper, coins, wax, brass, a drum and a creaking door.

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
    return mix / np.abs(mix).max() * 0.16


def tankard():
    """A pewter tankard set down on a wooden table: a short, warm knock of wood with a faint metal ring."""
    rng = np.random.default_rng(5)
    t = np.arange(int(RATE * 0.35)) / RATE
    wood = (np.sin(2 * np.pi * 150 * t) + 0.6 * np.sin(2 * np.pi * 310 * t) + 0.3 * np.sin(2 * np.pi * 520 * t)) * np.exp(-t / 0.035)
    thud = rng.normal(0, 1, len(t))
    thud = np.convolve(thud, np.ones(12) / 12, mode="same") * np.exp(-t / 0.012) * 1.2
    ring = (np.sin(2 * np.pi * 1180 * t) + 0.5 * np.sin(2 * np.pi * 2730 * t)) * np.exp(-t / 0.09) * 0.18
    sound = (wood + thud + ring) * np.minimum(1, t / 0.001)
    return sound / np.abs(sound).max() * 0.2


def seconds(length):
    return np.arange(int(RATE * length)) / RATE


def band_noise(length, low, high, seed):
    """White noise kept between two frequencies."""
    n = int(RATE * length)
    spectrum = np.fft.rfft(np.random.default_rng(seed).normal(0, 1, n))
    freqs = np.fft.rfftfreq(n, 1 / RATE)
    spectrum[(freqs < low) | (freqs > high)] = 0
    out = np.fft.irfft(spectrum, n)
    return out / max(1e-9, np.abs(out).max())


def ring(freqs, amps, decays, length):
    t = seconds(length)
    return sum(a * np.exp(-t / d) * np.sin(2 * np.pi * f * t) for f, a, d in zip(freqs, amps, decays))


def level(sound, peak):
    return sound / max(1e-9, np.abs(sound).max()) * peak


def thump():
    """Your own card landing: felt over wood, low and soft."""
    t = seconds(0.18)
    body = ring((95, 190, 300), (1, 0.4, 0.2), (0.045, 0.03, 0.02), 0.18)
    return level(body + band_noise(0.18, 200, 1500, 1) * np.exp(-t / 0.01) * 0.5, 0.22)


def knock():
    """A knuckle on the table: your turn."""
    t = seconds(0.12)
    wood = ring((240, 510, 830), (1, 0.5, 0.25), (0.025, 0.015, 0.01), 0.12)
    return level(wood + band_noise(0.12, 800, 4000, 2) * np.exp(-t / 0.003) * 0.4, 0.14)


def thup():
    """A dull, muffled no."""
    t = seconds(0.14)
    return level(ring((120, 180), (1, 0.4), (0.04, 0.025), 0.14) + band_noise(0.14, 80, 600, 3) * np.exp(-t / 0.02) * 0.6, 0.2)


def whump():
    """A trump coming down on another suit's lead: deep and heavy."""
    t = seconds(0.3)
    f = 85 - 30 * np.minimum(1, t / 0.15)
    body = np.sin(2 * np.pi * np.cumsum(f) / RATE) * np.exp(-t / 0.09)
    return level(body + band_noise(0.3, 60, 900, 4) * np.exp(-t / 0.02) * 0.5, 0.26)


def sweep():
    """Cards raked across the felt."""
    t = seconds(0.28)
    env = np.sin(np.pi * np.minimum(1, t / 0.28)) ** 1.5
    return level(band_noise(0.28, 900, 5000, 5) * env, 0.1)


def coin():
    """A coin dropped on the table: a bright ting and a small bounce."""
    first = ring((3150, 4870, 7900), (1, 0.6, 0.3), (0.25, 0.12, 0.06), 0.7)
    second = ring((3150, 4870), (0.35, 0.2), (0.12, 0.06), 0.55)
    mix = first.copy()
    start = int(RATE * 0.11)
    mix[start:start + len(second)] += second[:len(mix) - start]
    return level(mix, 0.13)


def crackle():
    """A pinch of hearth crackle, for the momentum chevrons."""
    rng = np.random.default_rng(6)
    out = np.zeros(int(RATE * 0.3))
    for _ in range(9):
        at = rng.integers(0, len(out) - 200)
        pop = band_noise(0.006, 1500, 7000, int(rng.integers(1000))) * np.exp(-seconds(0.006) / 0.0015) * rng.uniform(0.3, 1)
        out[at:at + len(pop)] += pop
    return level(out, 0.12)


def stamp():
    """A wax seal pressed home: a soft wooden thud with a squash."""
    t = seconds(0.2)
    return level(ring((90, 170), (1, 0.4), (0.05, 0.03), 0.2) + band_noise(0.2, 300, 2500, 7) * np.exp(-t / 0.03) * 0.5, 0.22)


def riffle():
    """Cards riffling: a run of small paper snaps, quickening."""
    out = np.zeros(int(RATE * 0.38))
    at = 0.0
    for k in range(16):
        snap = band_noise(0.008, 2000, 6000, 20 + k) * np.exp(-seconds(0.008) / 0.002) * (0.5 + 0.5 * np.sin(np.pi * k / 15))
        i = int(RATE * at)
        out[i:i + len(snap)] += snap[:len(out) - i]
        at += 0.03 - 0.0012 * k
    return level(out, 0.12)


def paper():
    """A card slid off the table: a soft paper whisper."""
    t = seconds(0.3)
    return level(band_noise(0.3, 2500, 7000, 8) * np.sin(np.pi * t / 0.3) ** 2, 0.06)


def drum():
    """One low drum hit, for sudden death."""
    t = seconds(0.6)
    f = 70 - 20 * np.minimum(1, t / 0.2)
    body = np.sin(2 * np.pi * np.cumsum(f) / RATE) * np.exp(-t / 0.18)
    return level(body + band_noise(0.6, 100, 1200, 9) * np.exp(-t / 0.03) * 0.4, 0.3)


def tick():
    """A coin counted onto the pile."""
    return level(ring((4200, 6100), (1, 0.4), (0.012, 0.008), 0.05), 0.07)


def creak():
    """An old tavern door swinging open."""
    t = seconds(0.8)
    rate = 38 + 30 * np.sin(np.pi * t / 0.8)
    phase = np.cumsum(rate) / RATE
    pulses = (np.diff(np.floor(phase), prepend=0) > 0).astype(float)
    body = np.convolve(pulses, ring((620, 1340, 2100), (1, 0.6, 0.3), (0.01, 0.006, 0.004), 0.03), mode="full")[:len(t)]
    return level(body * np.sin(np.pi * t / 0.8) ** 0.7, 0.14)


def medal():
    """A medal set down: a heavier metal clink."""
    return level(ring((1480, 3910, 6200), (1, 0.45, 0.2), (0.3, 0.12, 0.05), 0.6), 0.13)


def whoosh():
    """A card turned over between screens: a soft rush of air."""
    t = seconds(0.22)
    return level(band_noise(0.22, 400, 2500, 10) * np.sin(np.pi * t / 0.22) ** 2, 0.06)


def latch():
    """A brass latch: click and clack."""
    click = ring((2600, 5100), (1, 0.4), (0.008, 0.004), 0.04)
    out = np.zeros(int(RATE * 0.09))
    out[:len(click)] += click
    out[int(RATE * 0.035):int(RATE * 0.035) + len(click)] += click * 0.6
    return level(out, 0.1)


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
    for name, samples in (("clink.wav", clink()), ("tankard.wav", tankard()), ("thump.wav", thump()), ("knock.wav", knock()),
                          ("thup.wav", thup()), ("whump.wav", whump()), ("sweep.wav", sweep()), ("coin.wav", coin()),
                          ("crackle.wav", crackle()), ("stamp.wav", stamp()), ("riffle.wav", riffle()), ("paper.wav", paper()),
                          ("drum.wav", drum()), ("tick.wav", tick()), ("creak.wav", creak()), ("medal.wav", medal()),
                          ("whoosh.wav", whoosh()), ("latch.wav", latch())):
        write(os.path.join(out, name), samples)
        print("wrote", os.path.join(out, name))
