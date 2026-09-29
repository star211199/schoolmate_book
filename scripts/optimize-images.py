#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 public/images 下的 PNG 批量转为 WebP 并降分辨率。

背景：原始素材是 1024~1536px 的未压缩 PNG，单张 2~3MB；而它们只用于
背景图（cover）、插画和头像。在 1Mbps 的穿透隧道下，登录页需要等 50 秒，
axios 15 秒超时必然报「网络异常」。

用法：
    python scripts/optimize-images.py            # 转换并在同目录生成 .webp
    python scripts/optimize-images.py --delete   # 转换后删除原 .png

注意：图片文件名是固定的（没有 hash），改完记得同步更新代码与数据库里的引用。
"""
import argparse
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
IMG_DIR = os.path.join(ROOT, "schoolmate-web", "public", "images")

# 按用途定目标：长边上限 + WebP 质量
#   头像实际显示只有几十像素，1024px 纯属浪费
#   插画显示宽度约 400~600px
#   背景/封面要铺满屏幕，留大一些
RULES = {
    "avatar-girl.png":      (256, 256, 85),
    "avatar-boy.png":       (256, 256, 85),
    "login-illustration.png": (900, 1350, 80),
    "h5-hero.png":          (900, 1350, 80),
    "album-sample.png":     (1200, 800, 78),
    "banner-graduation.png": (1200, 800, 78),
    "bg-campus.png":        (1440, 960, 78),
}


def convert(name: str) -> tuple[int, int]:
    src = os.path.join(IMG_DIR, name)
    if not os.path.exists(src):
        print(f"  跳过（不存在）: {name}")
        return 0, 0

    max_w, max_h, quality = RULES.get(name, (1440, 1440, 80))
    before = os.path.getsize(src)

    im = Image.open(src)
    if im.mode not in ("RGB", "RGBA"):
        im = im.convert("RGB")

    # 等比缩放到长边下限内，不放大
    ratio = min(max_w / im.width, max_h / im.height, 1.0)
    if ratio < 1.0:
        im = im.resize((round(im.width * ratio), round(im.height * ratio)), Image.LANCZOS)

    dst = os.path.join(IMG_DIR, os.path.splitext(name)[0] + ".webp")
    im.save(dst, "WEBP", quality=quality, method=6)
    after = os.path.getsize(dst)

    print(f"  {name:26s} {before/1024:8.1f} KB "
          f"{im.width}x{im.height:<5} → {after/1024:7.1f} KB  "
          f"(压缩 {before/after:.0f}x)")
    return before, after


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--delete", action="store_true", help="转换后删除原 PNG")
    args = ap.parse_args()

    if not os.path.isdir(IMG_DIR):
        print(f"!! 找不到目录 {IMG_DIR}", file=sys.stderr)
        return 1

    print(f"源目录: {IMG_DIR}\n")
    tb = ta = 0
    for name in sorted(RULES):
        b, a = convert(name)
        tb += b
        ta += a
        if args.delete and b:
            os.remove(os.path.join(IMG_DIR, name))

    print(f"\n合计: {tb/1024/1024:.1f} MB → {ta/1024/1024:.2f} MB"
          f"  （减少 {100 - ta/tb*100:.1f}%）")
    if args.delete:
        print("原 PNG 已删除。")
    else:
        print("原 PNG 保留；确认无误后可用 --delete 删除。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
