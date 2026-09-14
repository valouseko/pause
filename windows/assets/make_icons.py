# Vygeneruje ikonu Mezery (modrý dýchací kruh) v potřebných velikostech.
import math
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))

def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))

def make_orb(size, pad_ratio=0.10, with_shadow=True):
    """Sphere s radiálním přechodem, měkký highlight, jemný stín."""
    ss = 4  # supersampling
    S = size * ss
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = img.load()

    pad = int(S * pad_ratio)
    r = (S - 2 * pad) / 2.0
    cx = S / 2.0
    cy = S / 2.0

    # highlight posunutý vlevo nahoru
    hx = cx - r * 0.30
    hy = cy - r * 0.34

    # barvy sféry (od světlého středu k modré)
    c_hi = (255, 255, 255)
    c_1 = (201, 213, 255)
    c_2 = (127, 155, 255)
    c_3 = (85, 112, 244)
    c_edge = (74, 96, 226)

    shadow_col = (60, 90, 200)

    for y in range(S):
        for x in range(S):
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx * dx + dy * dy)
            if dist <= r:
                # vzdálenost od highlightu určuje odstín
                hd = math.sqrt((x - hx) ** 2 + (y - hy) ** 2) / (r * 1.55)
                hd = max(0.0, min(1.0, hd))
                if hd < 0.33:
                    col = lerp(c_hi, c_1, hd / 0.33)
                elif hd < 0.62:
                    col = lerp(c_1, c_2, (hd - 0.33) / 0.29)
                elif hd < 0.85:
                    col = lerp(c_2, c_3, (hd - 0.62) / 0.23)
                else:
                    col = lerp(c_3, c_edge, (hd - 0.85) / 0.15)
                # antialiasing na okraji
                edge = r - dist
                a = 255 if edge >= ss else int(255 * (edge / ss))
                a = max(0, min(255, a))
                px[x, y] = (col[0], col[1], col[2], a)
            elif with_shadow and dist <= r * 1.22:
                # měkký stín pod koulí
                t = (dist - r) / (r * 0.22)
                below = 1.0 if dy > 0 else 0.35
                alpha = int(70 * (1 - t) * below)
                if alpha > 0:
                    px[x, y] = (shadow_col[0], shadow_col[1], shadow_col[2], alpha)

    return img.resize((size, size), Image.LANCZOS)

def main():
    # hlavní ikona (čistá koule, bez pečeného stínu)
    base = make_orb(512, pad_ratio=0.07, with_shadow=False)
    base.save(os.path.join(HERE, "icon.png"))

    # tray (menší, bez stínu, ostřejší)
    tray = make_orb(64, pad_ratio=0.06, with_shadow=False)
    tray.save(os.path.join(HERE, "tray.png"))

    # .ico z více velikostí
    sizes = [16, 24, 32, 48, 64, 128, 256]
    ico_imgs = [make_orb(s, pad_ratio=0.08, with_shadow=False) for s in sizes]
    ico_imgs[0].save(
        os.path.join(HERE, "icon.ico"),
        format="ICO",
        sizes=[(s, s) for s in sizes],
        append_images=ico_imgs[1:],
    )
    print("Hotovo: icon.png, tray.png, icon.ico")

if __name__ == "__main__":
    main()
