"""Ghép fragment giao diện vào khung chung.

Fragment:  _gen/frag/<màn>.html   (chỉ phần nội dung riêng của màn)
Khung:     _gen/frame.html        (khung chung: header, sidebar... có chỗ {{title}} và {{content}})

Tiêu đề màn khai báo ngay trong fragment bằng một comment ở đầu:  <!--title: Danh sách đơn hàng-->
Không có thì dùng tên màn.

Chạy riêng để xem kết quả ghép:  python _gen/build.py <màn>
Thường không cần gọi trực tiếp: interact.py tự dùng render() cho màn chỉ có fragment.
"""
import html
import re
import sys
from pathlib import Path

GEN = Path(__file__).resolve().parent


def render(name):
    frag = (GEN / "frag" / (name + ".html")).read_text(encoding="utf-8")
    frame = (GEN / "frame.html").read_text(encoding="utf-8")
    m = re.search(r"<!--\s*title:\s*(.*?)\s*-->", frag)
    title = m.group(1) if m else name
    if m:
        frag = frag.replace(m.group(0), "", 1).lstrip("\r\n")
    # <!--layout: app--> hiện menu người dùng ở header; <!--active: dashboard--> hiện thanh điều hướng và đánh dấu mục đang chọn
    layout = directive(frag, "layout")
    active = directive(frag, "active")
    frag = re.sub(r"<!--\s*(layout|active):.*?-->\s*", "", frag)
    header = (GEN / "partials" / ("header_app.html" if layout == "app" else "header_public.html")).read_text(encoding="utf-8")
    nav = render_nav(active) if active else ""
    frag = frag.replace("<!--include:request-vars-->", (GEN / "partials" / "request-vars.html").read_text(encoding="utf-8"))
    out = frame.replace("{{header}}", header).replace("{{nav}}", nav)
    return out.replace("{{title}}", html.escape(title)).replace("{{content}}", frag)


def directive(frag, name):
    m = re.search(r"<!--\s*%s:\s*(.*?)\s*-->" % name, frag)
    return m.group(1) if m else ""


NAV = [
    ("dashboard", "dashboard", "fa-home", "Dashboard"),
    ("configs", "api-config", "fa-cogs", "API Configs"),
    ("logs", "request-logs", "fa-history", "Request Logs"),
    ("admin", "admin-users", "fa-users-cog", "Admin"),
]


def render_nav(active):
    # Thanh điều hướng dạng pills, mục đang chọn lấy từ <!--active: ...--> của màn
    items = "\n".join(
        '        <a class="nav-link rounded-pill%s" href="%s.dc.html"><i class="fas %s me-2"></i> %s</a>'
        % (" active" if key == active else "", screen, icon, label)
        for key, screen, icon, label in NAV)
    return ('<div class="bg-white border rounded-3 p-3 mb-4 shadow-sm">\n      <nav class="nav nav-pills nav-justified">\n'
            + items + "\n      </nav>\n    </div>")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Cách dùng: python _gen/build.py <màn>")
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    sys.stdout.write(render(sys.argv[1]))
