"""Cuts the Codex sheets in art/sheets into app drawables.

    python tools/cut.py

Creatures (3x2): pet_<id>_<0..5>.webp – wary or shy, curious, calm, its own move, cheering, asleep.
Items (4x4): item_<id>.webp. Props (4x4): prop_<id>.webp.
Lodge: lodge_bg.webp with the magenta sky made see-through (the sky is painted behind it).
The magenta background is keyed out; each frame is cropped to its creature.
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))
from gen_art import WILD, SHY, FIND, WILD_OWN, SHY_OWN  # noqa: E402

SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

PETS = list(WILD) + list(SHY) + list(FIND) + list(WILD_OWN) + list(SHY_OWN)
ITEMS = ["rocker", "lamp", "bench", "wood", "birdhouse", "well", "chimes", "mailbox",
         "swing", "scarecrow", "geranium", "shelf", "flags", "hammock", "sign", "kettle"]
PROPS = ["cone", "cones", "sprout", "seedling", "sunflower", "pumpkin", "lavender", "apple",
         "blueberry", "cabbage", "rose", "tomato", "letter", "hourglass", "notebook", "bowl"]
ICONS = ["lodge", "journal", "paw", "scroll", "lantern", "gear", "acorn", "map",
         "hourglass", "heart", "star", "candle", "seeds", "pencil", "bell", "sign"]
MAX = 320


def key(im):
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            m = min(r, b) - g  # how magenta
            if m > 90 and r > 170 and b > 170:
                px[x, y] = (0, 0, 0, 0)
            elif m > 40 and r > 150 and b > 150:
                # a fringe: pull the pink out and fade it
                k = min(1, (m - 40) / 50)
                px[x, y] = (int(r - (r - g) * k), g, int(b - (b - g) * k), int(255 * (1 - k)))
    return im


def border_clean(cell):
    """Drops bits of the neighbouring cells: anything opaque that touches the cell's edge."""
    import numpy as np
    from scipy import ndimage
    arr = np.array(cell)
    lab, n = ndimage.label(arr[:, :, 3] > 30)
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    sizes = ndimage.sum(np.ones_like(lab), lab, range(n + 1))
    big = max(range(1, n + 1), key=lambda k: sizes[k]) if n else 0
    for k in edge:
        if k != big:
            arr[lab == k, 3] = 0
    return Image.fromarray(arr)


def frames(sheet, cols, rows):
    im = key(Image.open(sheet))
    w, h = im.size
    cw, ch = w / cols, h / rows
    out = []
    for j in range(rows):
        for i in range(cols):
            cell = border_clean(im.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch))))
            box = cell.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox()
            cell = cell.crop(box) if box else cell
            s = MAX / max(cell.size)
            if s < 1:
                cell = cell.resize((max(1, round(cell.width * s)), max(1, round(cell.height * s))), Image.LANCZOS)
            out.append(cell)
    return out


def save(im, name):
    im.save(OUT / f"{name}.webp", "WEBP", quality=90, method=6)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = set(sys.argv[1:])
    for p in PETS:
        f = SHEETS / f"{p}.png"
        if want and p not in want:
            continue
        if not f.exists():
            print("missing", p)
            continue
        for n, fr in enumerate(frames(f, 3, 2)):
            save(fr, f"pet_{p}_{n}")
        print("pet", p)
    for sheet, names, prefix in (("items", ITEMS, "item"), ("props", PROPS, "prop")):
        if (SHEETS / f"{sheet}.png").exists() and (not want or sheet in want):
            for name, fr in zip(names, frames(SHEETS / f"{sheet}.png", 4, 4)):
                save(fr, f"{prefix}_{name}")
            print(sheet)
    if (SHEETS / "icons.png").exists() and (not want or "icons" in want):
        for name, fr in zip(ICONS, frames(SHEETS / "icons.png", 4, 4)):
            fr.thumbnail((160, 160), Image.LANCZOS)
            save(fr, f"ic_{name}")
        print("icons")
    if (SHEETS / "landmarks.png").exists() and (not want or "landmarks" in want):
        for n, fr in enumerate(frames(SHEETS / "landmarks.png", 4, 4)):
            fr.thumbnail((200, 200), Image.LANCZOS)
            save(fr, f"ic_mark_{n}")
        print("landmarks")
    if (SHEETS / "weather.png").exists() and (not want or "weather" in want):
        for n, fr in enumerate(frames(SHEETS / "weather.png", 4, 2)):
            fr.thumbnail((160, 160), Image.LANCZOS)
            save(fr, f"ic_mood_{n}")
        print("weather")
    if (SHEETS / "clouds.png").exists() and (not want or "clouds" in want):
        for n, fr in enumerate(frames(SHEETS / "clouds.png", 3, 2)):
            save(fr, f"cloud_{n}")
        print("clouds")
    for scene in ("yard", "house"):
        if (SHEETS / f"{scene}.png").exists() and (not want or scene in want):
            g = key(Image.open(SHEETS / f"{scene}.png"))
            g = g.resize((1152, round(1152 * g.height / g.width)), Image.LANCZOS)
            g.save(OUT / f"{scene}_bg.webp", "WEBP", quality=88, method=6)
            print(scene)
    if (SHEETS / "lodge.png").exists() and (not want or "lodge" in want):
        g = key(Image.open(SHEETS / "lodge.png"))
        g = g.resize((1152, round(1152 * g.height / g.width)), Image.LANCZOS)
        g.save(OUT / "lodge_bg.webp", "WEBP", quality=88, method=6)
        print("lodge")


if __name__ == "__main__":
    main()
