# Vygeneruje launcher ikony Mezery (modra koule) do res/mipmap-*.
import math
import os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "app", "src", "main", "res")

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))

def make_orb(S):
    ss = 4
    W = S * ss
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    px = img.load()
    r = W / 2.0
    cx = cy = W / 2.0
    hx = cx - r * 0.30
    hy = cy - r * 0.34
    c_hi = (255, 255, 255)
    c_1 = (201, 213, 255)
    c_2 = (127, 155, 255)
    c_3 = (85, 112, 244)
    c_edge = (74, 96, 226)
    for y in range(W):
        for x in range(W):
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx * dx + dy * dy)
            if dist <= r:
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
                edge = r - dist
                a = 255 if edge >= ss else int(255 * (edge / ss))
                px[x, y] = (col[0], col[1], col[2], max(0, min(255, a)))
    return img.resize((S, S), Image.LANCZOS)

def icon_square(size):
    bg = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    orb_size = int(size * 0.72)
    orb = make_orb(orb_size)
    off = (size - orb_size) // 2
    bg.alpha_composite(orb, (off, off))
    return bg

def icon_round(size):
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse([0, 0, size - 1, size - 1], fill=255)
    white = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    bg = Image.composite(white, bg, mask)
    orb_size = int(size * 0.72)
    orb = make_orb(orb_size)
    off = (size - orb_size) // 2
    bg.alpha_composite(orb, (off, off))
    return bg

def main():
    for dens, size in DENSITIES.items():
        d = os.path.join(RES, f"mipmap-{dens}")
        os.makedirs(d, exist_ok=True)
        icon_square(size).save(os.path.join(d, "ic_launcher.png"))
        icon_round(size).save(os.path.join(d, "ic_launcher_round.png"))
        print(f"mipmap-{dens}: {size}px")
    print("Hotovo.")

if __name__ == "__main__":
    main()
