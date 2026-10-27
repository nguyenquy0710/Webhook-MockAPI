"""Rà lệch theme của nguồn prototype.

Chạy:  python _gen/lint_theme.py [--strict] [đường dẫn thêm ...]
Quét: _gen/frame*.html, _gen/pane-*, _gen/orig/*, _gen/frag/* và các đường dẫn thêm (file hoặc thư mục,
vd design/input.css, design/tailwind.config.js).

Quy chuẩn lấy từ ../.theme.json (mọi khóa đều tùy chọn):
    "tokens":  {"primary": "#006FEE", ...}   màu cho phép; có khóa này mới kiểm "màu ngoài token"
    "radii":   [8, 12, 14, 9999]              bo góc (px) cho phép
    "weights": [400, 500, 600, 700]           trọng số chữ cho phép (mặc định như ví dụ)
    "approximate": true                       token xấp xỉ, kết quả chỉ để tham khảo

Phát hiện: gradient; bóng đổ có màu (khác đen/xám trong suốt); bo góc ngoài quy chuẩn (kể cả rounded-[..]);
trọng số chữ ngoài quy chuẩn; màu hex ngoài token. Màu trong khai báo token (--tên: #hex), hex hiển thị làm chữ trong bảng
và dòng khai báo "colors"/"tokens" của tailwind được bỏ qua. Cảnh báo chỉ gợi ý: mỗi chỗ cần xét ngữ cảnh nền trước khi sửa
(vd chữ sáng trên nền xanh không được đổi thành màu primary mờ).
"""
import json
import re
import sys
from pathlib import Path

GEN = Path(__file__).resolve().parent
ROOT = GEN.parent
HEX = re.compile(r"#(?:[0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{3})\b")
SHADOW = re.compile(r"(?:box-shadow|drop-shadow|shadow)[^;\n}]*", re.I)
NEUTRAL_SHADOW = re.compile(r"rgba?\(\s*0\s*[, ]\s*0\s*[, ]\s*0|#000(?:000)?\b|var\(", re.I)
RADIUS = re.compile(r"border-radius\s*:\s*([^;}\n]+)|rounded-\[(\d+)px\]", re.I)
WEIGHT = re.compile(r"font-weight\s*:\s*(\d{3})|font-\[(\d{3})\]", re.I)


def load_theme():
    p = ROOT / ".theme.json"
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def files(extra):
    out = []
    for pat in ("frame*.html", "pane-*", "orig/*", "frag/*"):
        out += [p for p in GEN.glob(pat) if p.is_file()]
    for e in extra:
        p = Path(e)
        out += [q for q in (p.rglob("*") if p.is_dir() else [p]) if q.is_file()]
    return sorted(set(out))


def check(path, theme):
    tokens = {v.lower() for v in (theme.get("tokens") or {}).values()}
    radii = set(theme.get("radii") or [])
    weights = set(theme.get("weights") or [400, 500, 600, 700])
    res = []
    for n, line in enumerate(path.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
        if re.search(r"(linear|radial|conic)-gradient|bg-gradient", line, re.I):
            res.append((n, "gradient", line.strip()))
        for m in SHADOW.finditer(line):
            if (HEX.search(m.group(0)) or re.search(r"rgba?\(", m.group(0))) and not NEUTRAL_SHADOW.search(m.group(0)):
                res.append((n, "bóng đổ có màu", m.group(0).strip()))
        if radii:
            for m in RADIUS.finditer(line):
                vals = [int(x) for x in re.findall(r"(\d+)px", m.group(1) or m.group(2) or "")] if m.group(1) else [int(m.group(2))]
                bad = [v for v in vals if v not in radii and v != 0]
                if bad:
                    res.append((n, "bo góc ngoài quy chuẩn %s" % bad, m.group(0).strip()))
        for m in WEIGHT.finditer(line):
            w = int(m.group(1) or m.group(2))
            if w not in weights:
                res.append((n, "trọng số chữ %d ngoài quy chuẩn" % w, m.group(0)))
        if tokens and not re.search(r"colors|tokens", line, re.I):
            # bỏ khai báo token (--tên: #hex) và hex hiển thị làm chữ (<td>#hex</td>)
            plain = re.sub(r"--[\w-]+\s*:\s*#[0-9a-fA-F]{3,8}", "", line)
            plain = re.sub(r">\s*#[0-9a-fA-F]{3,8}\s*<", "><", plain)
            for m in HEX.finditer(plain):
                if m.group(0).lower() not in tokens:
                    res.append((n, "màu ngoài token", m.group(0)))
    return res


def main(argv):
    strict = "--strict" in argv
    theme = load_theme()
    total = 0
    for f in files([a for a in argv if not a.startswith("--")]):
        for n, kind, text in check(f, theme):
            total += 1
            print("%s:%d  %s  | %s" % (f.relative_to(ROOT) if ROOT in f.parents else f, n, kind, text[:100]))
    if theme.get("approximate"):
        print("Lưu ý: token đang ở mức xấp xỉ (.theme.json), cần chủ sản phẩm xác nhận.")
    print("%d chỗ lệch theme." % total if total else "Không phát hiện lệch theme.")
    return 1 if (total and strict) else 0


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    sys.exit(main(sys.argv[1:]))
