"""Nút thao tác trong bảng -> icon.

Nút nằm trong ô bảng (td) có nhãn trùng một khóa trong ICONS sẽ được đổi thành nút chỉ có icon,
giữ nhãn ở thuộc tính title và aria-label (tooltip + trình đọc màn hình).
Nhãn so khớp sau khi chuẩn hóa (chữ thường, gộp khoảng trắng), cùng quy tắc với actions.py.

Thêm icon mới: thêm một dòng vào ICONS. Nếu project đã có quy ước icon riêng thì sửa file này cho khớp.
"""


def _svg(inner):
    return ('<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">' + inner + "</svg>")


ICONS = {
    "xem": _svg('<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>'),
    "sửa": _svg('<path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>'),
    "xóa": _svg('<polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>'
                '<path d="M10 11v6M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>'),
    "tải về": _svg('<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/>'
                   '<line x1="12" y1="15" x2="12" y2="3"/>'),
    "sao chép": _svg('<rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>'
                     '<path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>'),
    "duyệt": _svg('<polyline points="20 6 9 17 4 12"/>'),
}
