---
type: refactor
complexity: high
impact: high
related_issues: [QUYIT-961]
related_prs: [#39, #40, #41]
time_spent_hours: ~12
status: completed
---

# Kế hoạch: Thiết kế lại giao diện các màn của Webhook MockAPI (UX/UI)

> **Ngày:** 2026-10-27
> **Phạm vi:** toàn bộ template Thymeleaf (dashboard, api-config, api-config-edit, request-logs, login, register, change-password, admin/users, admin/user-create, admin/user-detail), `docs/prototype/`
> **Trạng thái:** ✅ Đã hoàn thành, đã merge vào `coding` (PR #39, #40, #41)

---

## 1. Bối cảnh

Giao diện cũ là các form và bảng dài một cột, thao tác quan trọng bị chìm (nút cuối trang, form tạo API chiếm đầu trang), thiếu tìm kiếm/lọc, ít phản hồi trực tiếp (độ mạnh mật khẩu, tóm tắt). Yêu cầu: sắp xếp lại thành phần cho thân thiện với người dùng, giữ nguyên nghiệp vụ và nhãn nút.

Quy trình: thiết kế trên prototype (`docs/prototype/`, theme bootstrap) → chụp ảnh và bấm thử bằng trình duyệt headless → áp dụng vào template thật → chạy app thật (JDK 17 tạm, DB cũ) để kiểm tra.

## 2. Công việc đã thực hiện

Chia thành ba nhánh, đều tách từ `origin/coding` (quy tắc của project):

| Nhánh | Nội dung |
|---|---|
| `feature/api-config-edit-redesign` | Trang sửa endpoint: header có method và path, card Endpoint/Response/Note, cột Summary dính, chip status, Format JSON, cảnh báo thay đổi chưa lưu. Sửa lỗi render lại thiếu id/domain. Có plan riêng `2026-10-27-api-config-edit-redesign.md` (trên nhánh `claude/minipc`). |
| `feature/dashboard-redesign` | Dashboard: nút New API, ô Webhook URL copy được, 3 ô số liệu, 5 request gần nhất, Quick Start Guide thu gọn. Thêm `countRequestLogsSince` và truyền số liệu từ controller. Nút copy đổi class (class `copy-btn` bị CSS toàn cục ẩn). |
| `feature/screens-redesign` (nhánh này) | Auth (login, register, change-password), api-config (danh sách + modal tạo), request-logs (lọc server-side + panel chi tiết), admin (users, create, detail). |

Quyết định và đánh đổi chính:
- **Bootstrap JS nạp hai lần** (`header.html` và `footer.html`) làm mọi `data-bs-toggle="collapse"`/dropdown bật rồi tắt trong một lần bấm. Bỏ lần nạp ở header (commit `c515ca0`, cherry-pick vào các nhánh cần).
- **Lọc request log ở server** (`RequestLogRepository.search`, tham số `method`, `status`): dùng chuỗi rỗng và khoảng số thay vì `null` để truy vấn không phụ thuộc DB. Phân trang giữ nguyên bộ lọc.
- **Lọc danh sách API ở client:** danh sách không phân trang nên lọc trên DOM là đủ, không thêm endpoint.
- **Panel chi tiết log dùng chung** (offcanvas điền từ `data-*` của hàng) thay vì một modal cho mỗi log, giảm lượng HTML.
- **Hộp thoại xác nhận dùng chung cho admin** (đổi vai trò, bật/tắt, xóa) gửi form POST kèm CSRF.
- **"Last request" hiện giờ tuyệt đối** thay vì "n phút trước", vì log lưu giờ máy chủ.
- Format JSON và kiểm tra JSON không chặn lưu khi chuỗi có `{{biến}}` ở vị trí không phải chuỗi.
- Không đụng `index` (trong app thật `/` chuyển sang dashboard) và `swagger-ui` (thư viện ngoài).
- Lỗi có sẵn đã sửa: `request-log.js` hiển thị giờ giả cố định; kiểm tra JSON trong `api-config.js` gắn nhầm vào form đăng xuất; form tạo API thiếu giá trị mặc định nên không gửi được; `AuthController` không cung cấp `domain`/`username` cho view.

## 3. Files đã thay đổi

| File | Thay đổi |
|---|---|
| [src/main/resources/templates/](../src/main/resources/templates/) | Viết lại `login`, `register`, `change-password`, `api-config`, `api-config-edit`, `dashboard`, `request-logs`, `admin/users`, `admin/user-create`, `admin/user-detail`; `fragments/header.html` bỏ nạp Bootstrap JS trùng |
| [src/main/resources/static/js/password-toggle.js](../src/main/resources/static/js/password-toggle.js) (**Mới**) | Nút hiện/ẩn mật khẩu dùng chung |
| [src/main/resources/static/js/api-config.js](../src/main/resources/static/js/api-config.js) | Kiểm tra JSON gắn đúng form, bỏ qua `{{biến}}` |
| [src/main/resources/static/js/request-log.js](../src/main/resources/static/js/request-log.js) | Giờ UTC thật, không tải lại khi panel mở |
| [src/main/java/vn/autobot/webhook/repository/RequestLogRepository.java](../src/main/java/vn/autobot/webhook/repository/RequestLogRepository.java) | `search(...)` lọc path/method/status |
| [src/main/java/vn/autobot/webhook/service/RequestLogService.java](../src/main/java/vn/autobot/webhook/service/RequestLogService.java) | `searchRequestLogs(...)` |
| [src/main/java/vn/autobot/webhook/controller/WebhookViewController.java](../src/main/java/vn/autobot/webhook/controller/WebhookViewController.java), [AdminController.java](../src/main/java/vn/autobot/webhook/controller/AdminController.java) | Tham số `method`/`status`, mặc định form tạo API |
| [src/main/java/vn/autobot/webhook/controller/AuthController.java](../src/main/java/vn/autobot/webhook/controller/AuthController.java) | `@ModelAttribute` `domain` và `username` |
| [src/test/java/vn/autobot/webhook/service/RequestLogSearchTest.java](../src/test/java/vn/autobot/webhook/service/RequestLogSearchTest.java) (**Mới**) | 3 test lọc log |
| [docs/prototype/](../docs/prototype/) | Prototype các màn đã thiết kế lại |

(Dashboard và api-config-edit nằm trên hai nhánh còn lại.)

## 4. Trạng thái hiện tại

- Jira: [QUYIT-961](https://nhquydev.atlassian.net/browse/QUYIT-961) (Task, Sprint 35, version Tháng 10/2026, ước lượng 12h, liên kết Relates QUYIT-960), trạng thái **Done** (2026-10-27).
- Đã merge vào `coding`: #41 (screens), #40 (api-config-edit), #39 (dashboard; xung đột `RequestLogService` đã xử lý bằng cách giữ cả `countRequestLogsSince` và `searchRequestLogs`). Sau khi gộp đã chạy lại app thật: dashboard, lọc log và các trang đã gộp hoạt động. Nhánh screens gồm: `2cb9edd` prototype, `c515ca0` fix Bootstrap JS, `3073890` template và backend.
- Đã chạy app thật và kiểm tra: đăng ký, đăng nhập, đổi mật khẩu, tạo/xóa API, lọc method/status, panel chi tiết, phân trang, vô hiệu hóa/tạo/xóa người dùng; 9 unit test pass; 7 trang không tràn ngang ở 390px; không lỗi JS.
- Chưa kiểm tra: Backup/Restore bằng tệp thật, chạy lại trên DB trống, trình duyệt thật (mới dùng Chromium headless).

## 5. Việc còn mở (chưa làm, để quyết định sau)

- [x] Push ba nhánh và mở PR vào `coding` (#39, #40, #41)
- [x] Xử lý xung đột khi gộp ba nhánh (chỉ có `RequestLogService`)
- [ ] Kiểm tra thủ công Backup/Restore, Copy URL, Format JSON, giao diện mobile bằng trình duyệt thật
- [ ] Áp dụng bố cục form tạo mới cho đồng nhất với trang sửa (đã gần giống, chưa có chip status/Format JSON trong modal)
- [ ] Test tự động cho `countRequestLogsSince` và controller view

## 6. Bài học rút ra (Tùy chọn)

- Kiểm tra thư viện JS bị nạp trùng trước khi dùng `data-bs-toggle`; lỗi này làm hỏng collapse âm thầm.
- CSS toàn cục theo tên class chung (`.copy-btn`) có thể ẩn phần tử mới; đặt tên class riêng.
- `th:inline="javascript"`: không nối chuỗi ngay sau `/*[[...]]*/`; gán biến riêng.
- `pgrep -f`/`pkill -f` trong cùng lệnh shell có thể khớp chính nó; kill theo PID.
- Thiết kế trên prototype trước giúp chốt bố cục nhanh, nhưng lỗi thật chỉ lộ khi chạy template Thymeleaf.

## 7. Tham khảo bên ngoài (Tùy chọn)

- Liên quan: `plans/2026-10-27-api-config-description-planning.md` (issue #35), `plans/2026-10-27-api-config-edit-redesign.md` (nhánh `claude/minipc`).

## Cập nhật 2026-10-27 (sửa sau khi gộp: menu 3 chấm và cột Description)

- Sửa lỗi menu 3 chấm ở `/admin/users` không mở (commit `9a92f8f`): `fragments/header.html` có CSS toàn cục `.dropdown-menu { opacity: 0; visibility: hidden }` chỉ hiện khi hover `.dropdown`. Menu trong `.btn-group` nhận class `.show` nhưng vẫn bị ẩn. Thêm `.dropdown-menu.show` vào rule hiển thị; menu hover trên navbar giữ nguyên. Rule dùng chung nên các menu mở bằng click khác (ví dụ API Configs) cũng được sửa.
- Thêm cột **Description** giữa Path và Status trong bảng `/api-configs` (commit `7ba6673`), tối đa 280px, cắt bằng "…" và có tooltip; ô search theo path/description giữ nguyên. Prototype (`_gen/frag/api-config.html`) được cập nhật tương ứng, `interact.py --all --strict` sạch.
- Cả hai commit nằm trên `claude/minipc`, đã push, chưa mở PR vào `coding`.
- **Chưa kiểm tra:** hai thay đổi này chưa chạy trên app thật hoặc trình duyệt; chưa có test tự động.
- Việc còn lại: mở PR vào `coding`; kiểm tra thủ công menu 3 chấm và cột Description trên trình duyệt.
