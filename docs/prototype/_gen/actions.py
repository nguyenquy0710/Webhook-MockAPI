"""Khai báo hành vi cho các nút của prototype.

Hành vi được gắn vào nút theo CHỮ HIỂN THỊ của nút (đã chuẩn hóa: chữ thường, gộp khoảng trắng).
Đổi nhãn nút trên giao diện thì phải đổi khóa tương ứng ở đây. interact.py sẽ cảnh báo:
  - nút trong nguồn chưa có hành vi,
  - hành vi khai báo cho màn nhưng không khớp nút nào.

Cấu trúc:
    ACTIONS = {
        "*":       { "<nhãn nút>": {spec}, ... },   # dùng cho mọi màn
        "<tên màn>": { "<nhãn nút>": {spec}, ... }, # chỉ màn đó, ghi đè khóa chung
    }

Các loại hành vi (spec["type"]):
    modal    Hộp thoại có ô nhập.   title, body?, fields?, confirm?, cancel?, toast?, delay?, outcome?, error?
    confirm  Hộp thoại xác nhận (nút nguy hiểm).  title, body?, confirm?, cancel?, toast?, delay?, outcome?, error?
    toggle   Bật/tắt một vùng.      region   (vùng khai báo trong nguồn bằng <!--region:tên--> ... <!--/region-->)
    toast    Hiện thông báo.        message, delay? (có delay thì nút chuyển sang "đang xử lý" rồi mới báo), outcome?
    goto     Chuyển sang màn khác.  screen   (tên màn; phát sự kiện "proto:goto" để nơi nhúng có thể xử lý thay)
    close    Đóng hộp thoại đang mở.

Trường nhập (fields): {"label": "...", "type": "text|number|date|email|textarea|select",
                       "required": True, "placeholder": "...", "options": [...] (select), "value": "..."}
    outcome: "success" (mặc định) hoặc "error" để minh họa trạng thái lỗi.

Hành vi phức tạp hơn (sắp xếp, lọc, phân trang...): viết thẳng <script> trong nguồn orig/frag.
"""

# Chuỗi giao diện của phần hộp thoại/thông báo do bộ dựng chèn vào. UI sản phẩm bằng tiếng Anh.
STRINGS = {
    "required": "Please enter {label}",
    "processing": "Processing…",
    "saved": "Saved",
    "cancel": "Cancel",
    "confirm": "Save",
    "confirm_danger": "Delete",
    "error": "Something went wrong. Please try again.",
}

ACTIONS = {
    "*": {
        "logout": {"type": "goto", "screen": "login"},
    },
    "dashboard": {
        "copy url": {"type": "toast", "message": "URL copied to clipboard"},
    },
    "api-config": {
        "backup": {"type": "toast", "message": "API configurations backed up successfully!", "delay": 800},
        "restore confirm": {"type": "toast", "message": "API configurations restored successfully!", "delay": 600},
        "copy full endpoint url": {"type": "toast", "message": "Endpoint URL copied to clipboard!"},
        "create api": {"type": "toast", "message": "API configuration saved successfully!", "delay": 600},
        "delete": {"type": "confirm", "title": "Confirm Delete",
                   "body": "Are you sure you want to delete this API configuration?",
                   "confirm": "Delete", "toast": "API configuration deleted successfully!"},
    },
    "api-config-edit": {
        "copy url": {"type": "toast", "message": "URL copied to clipboard"},
        "delete api": {"type": "confirm", "title": "Delete API?", "body": "This action cannot be undone.",
                       "confirm": "Delete", "toast": "API configuration deleted successfully!"},
    },
    "request-logs": {
        "export excel": {"type": "toast", "message": "Excel file exported", "delay": 800},
    },
    "admin-users": {
        "demote to user": {"type": "confirm", "title": "Demote to User?", "confirm": "Demote", "toast": "User role updated"},
        "promote to admin": {"type": "confirm", "title": "Promote to Admin?", "confirm": "Promote", "toast": "User role updated"},
        "disable user": {"type": "confirm", "title": "Disable User?", "confirm": "Disable", "toast": "User disabled"},
        "enable user": {"type": "confirm", "title": "Enable User?", "confirm": "Enable", "toast": "User enabled"},
        "delete user": {"type": "confirm", "title": "Delete user?", "body": "This action cannot be undone.",
                        "confirm": "Delete", "toast": "User deleted successfully!"},
    },
    "admin-user-detail": {
        "promote": {"type": "confirm", "title": "Promote to Admin?", "confirm": "Promote", "toast": "User role updated"},
        "disable": {"type": "confirm", "title": "Disable User?", "confirm": "Disable", "toast": "User disabled"},
    },
}
