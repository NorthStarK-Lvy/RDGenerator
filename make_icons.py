"""生成启动图标：legacy 圆角方形 PNG + adaptive foreground PNG。

运行：python make_icons.py
"""
from __future__ import annotations

import os

from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "app", "src", "main", "res")

PRIMARY = (103, 80, 164, 255)        # #6750A4
PRIMARY_DARK = (86, 64, 143, 255)    # #56408F
WHITE = (255, 255, 255, 255)

SS = 4  # 超采样倍数


def rounded_mask(size: int, radius: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill=255)
    return mask


def background(size: int) -> Image.Image:
    """深紫底 + 左上角柔光。"""
    img = Image.new("RGBA", (size, size), PRIMARY_DARK)
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    cx, cy = size * 0.32, size * 0.26
    r = size * 0.72
    gd.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(150, 122, 220, 190))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=size * 0.14))
    img.alpha_composite(glow)
    return img


def dice_layer(size: int, scale: float, with_shadow: bool = True) -> Image.Image:
    """透明底上画一颗骰子；scale 相对画布边长。"""
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    side = size * scale
    cx, cy = size / 2, size / 2

    # 旋转后的骰子画在一块独立画布上，再旋转贴回
    pad = int(side * 0.18)
    canvas_side = int(side + pad * 2)
    tile = Image.new("RGBA", (canvas_side, canvas_side), (0, 0, 0, 0))
    td = ImageDraw.Draw(tile)
    box = (pad, pad, pad + side, pad + side)
    radius = side * 0.24

    if with_shadow:
        shadow = Image.new("RGBA", (canvas_side, canvas_side), (0, 0, 0, 0))
        sd = ImageDraw.Draw(shadow)
        offset = side * 0.035
        sd.rounded_rectangle(
            (box[0] + offset, box[1] + offset * 1.6, box[2] + offset, box[3] + offset * 1.6),
            radius=radius,
            fill=(30, 20, 50, 110),
        )
        shadow = shadow.filter(ImageFilter.GaussianBlur(radius=side * 0.045))
        tile.alpha_composite(shadow)

    td.rounded_rectangle(box, radius=radius, fill=WHITE)

    dot_r = side * 0.072
    positions = [(0.27, 0.27), (0.73, 0.27), (0.5, 0.5), (0.27, 0.73), (0.73, 0.73)]
    for fx, fy in positions:
        x = pad + side * fx
        y = pad + side * fy
        td.ellipse((x - dot_r, y - dot_r, x + dot_r, y + dot_r), fill=PRIMARY)

    tile = tile.rotate(-8, resample=Image.BICUBIC, expand=False, center=(canvas_side / 2, canvas_side / 2))
    layer.alpha_composite(tile, (int(cx - canvas_side / 2), int(cy - canvas_side / 2)))
    return layer


def legacy_icon(size: int) -> Image.Image:
    big = size * SS
    img = background(big)
    img.alpha_composite(dice_layer(big, 0.52))
    # 圆角：约 22%
    mask = rounded_mask(big, int(big * 0.22))
    img.putalpha(mask)
    return img.resize((size, size), Image.LANCZOS)


def round_icon(size: int) -> Image.Image:
    big = size * SS
    img = background(big)
    img.alpha_composite(dice_layer(big, 0.52))
    mask = Image.new("L", (big, big), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, big - 1, big - 1), fill=255)
    img.putalpha(mask)
    return img.resize((size, size), Image.LANCZOS)


def adaptive_foreground(size: int) -> Image.Image:
    """108dp 画布，内容压在中间 66dp 安全区内。"""
    big = size * SS
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    img.alpha_composite(dice_layer(big, 0.40))
    return img.resize((size, size), Image.LANCZOS)


DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}


def main() -> None:
    for density, factor in DENSITIES.items():
        folder = os.path.join(OUT, f"mipmap-{density}")
        os.makedirs(folder, exist_ok=True)
        legacy = legacy_icon(int(48 * factor))
        legacy.save(os.path.join(folder, "ic_launcher.png"))
        round_img = round_icon(int(48 * factor))
        round_img.save(os.path.join(folder, "ic_launcher_round.png"))

        drawable = os.path.join(OUT, f"drawable-{density}")
        os.makedirs(drawable, exist_ok=True)
        adaptive_foreground(int(108 * factor)).save(
            os.path.join(drawable, "ic_launcher_foreground.png")
        )
        print(f"{density}: legacy {legacy.size}, fg {int(108 * factor)}")


if __name__ == "__main__":
    main()
