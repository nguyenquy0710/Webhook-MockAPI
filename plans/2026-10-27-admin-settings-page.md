---
type: feature
complexity: medium
impact: medium
related_issues: [QUYIT-962]
related_prs: [#47]
time_spent_hours: ~6
status: completed
---

# Kế hoạch: Admin > Settings (cấu hình chung, bảo trì database) và bố cục admin hai cột

> **Ngày:** 2026-10-27
> **Phạm vi:** `templates/admin/*`, `fragments/admin-sidebar.html`, `SettingsService`, `DatabaseMaintenanceService`, `StartupDatabaseCompactor`, migration V5, `docs/prototype/`
> **Trạng thái:** ✅ Đã làm, kiểm tra trên máy, đã mở PR #47 vào `coding` (chờ merge)

---

## 1. Bối cảnh

Hai yêu cầu liên tiếp cho khu vực admin: (1) chia phần nội dung `/admin/*` thành hai cột, menu admin bên trái, nội dung bên phải; menu chỉ chứa các tab tính năng chính (không có "Create User"); (2) thêm trang Settings với hai card: cấu hình chung và Database maintenance (thu hồi không gian do dữ liệu đã xóa, cắt log giao dịch, làm mới thống kê truy vấn, chạy tự động mỗi ngày, lần đầu trên DB cũ nén cả tệp, cần dung lượng trống bằng cỡ DB).

## 2. Công việc đã thực hiện

- **Bố cục hai cột:** fragment dùng chung `fragments/admin-sidebar.html` (tab Users, Settings); `users`, `user-create`, `user-detail` bọc nội dung trong `aside` + `main`. Trang tạo/chi tiết user tô tab Users vì thuộc nhóm đó. Prototype cập nhật tương ứng.
- **Settings, card General:** ba mục do người dùng chưa nêu cụ thể nên tự chọn: Site name (header/footer), Public base URL (webhook URL, Swagger server; trống = `app.domain`), cho phép đăng ký người dùng mới (tắt thì `/register` chuyển về `/login?registrationClosed`, ẩn link Create Account). Lưu ở bảng `app_settings` (migration V5), có cache trong `SettingsService`; `GlobalModelAdvice` đưa `siteName`, `registrationEnabled` vào mọi view; các controller và `SwaggerService` đọc domain qua `SettingsService`.
- **Settings, card Database maintenance:** DB là H2, không có WAL/VACUUM như mô tả gốc (kiểu SQLite). Ánh xạ theo lựa chọn của người dùng: `CHECKPOINT` (cắt log) + `ANALYZE` (làm mới thống kê), tự chạy 03:30 mỗi ngày (`@Scheduled`) và chạy tay bằng nút có hộp xác nhận. Lần đầu trên DB chưa nén thì đặt cờ `maintenance.compactPending`; `StartupDatabaseCompactor` (đăng ký trong `META-INF/spring.factories`, chạy trước khi tạo DataSource) thực hiện `SHUTDOWN COMPACT` ở lần khởi động kế tiếp, bỏ qua và ghi cảnh báo nếu ổ đĩa trống ít hơn cỡ DB.
- **Quyết định/đánh đổi:** thử `SHUTDOWN COMPACT` khi app đang chạy làm H2 đóng DB, pool kết nối hỏng ("Database is already closed"), nên bỏ cách này và dời sang lúc khởi động. Số migration: dùng V5 cho settings, đổi migration của plan retry thành V6 (`2026-04-16-webhook-retry-planning.md`).
- **Prototype:** màn mới `admin-settings` (kiểm tra bắt buộc/URL, Save Settings, Run Maintenance có xác nhận và cập nhật kết quả), khai báo trong `canvas.json` và `actions.py`.

## 3. Files đã thay đổi

| File | Thay đổi |
|---|---|
| [src/main/resources/templates/admin/settings.html](../src/main/resources/templates/admin/settings.html) (**Mới**) | Trang Settings hai card |
| [src/main/resources/templates/fragments/admin-sidebar.html](../src/main/resources/templates/fragments/admin-sidebar.html) (**Mới**) | Menu admin bên trái |
| [src/main/resources/templates/admin/](../src/main/resources/templates/admin/) `users`, `user-create`, `user-detail` | Bố cục hai cột |
| [src/main/resources/templates/fragments/header.html](../src/main/resources/templates/fragments/header.html), `footer.html`, `login.html` | Tên site động, ẩn link đăng ký khi đóng |
| [src/main/java/vn/autobot/webhook/service/SettingsService.java](../src/main/java/vn/autobot/webhook/service/SettingsService.java) (**Mới**), `DatabaseMaintenanceService.java` (**Mới**) | Cài đặt và bảo trì DB |
| [src/main/java/vn/autobot/webhook/config/StartupDatabaseCompactor.java](../src/main/java/vn/autobot/webhook/config/StartupDatabaseCompactor.java) (**Mới**), `GlobalModelAdvice.java` (**Mới**), `src/main/resources/META-INF/spring.factories` (**Mới**) | Nén lúc khởi động, model dùng chung |
| `model/AppSetting.java`, `repository/AppSettingRepository.java`, `dto/GeneralSettingsDto.java` (**Mới**), `db/migration/V5__Create_app_settings.sql` (**Mới**) | Lưu trữ và validate |
| `AdminController`, `AuthController`, `WebhookViewController`, `SwaggerService`, `WebHookMockApplication` | Endpoint Settings, đọc domain qua `SettingsService`, `@EnableScheduling` |
| [src/test/java/vn/autobot/webhook/service/SettingsServiceTest.java](../src/test/java/vn/autobot/webhook/service/SettingsServiceTest.java) (**Mới**) | 3 test; `ApiConfigDescriptionNoteTest` sửa theo constructor mới |
| [docs/prototype/](../docs/prototype/) | Màn `admin-settings`, sidebar trong 3 màn admin |

## 4. Trạng thái hiện tại

- Đã commit và push nhánh `claude/minipc`, PR #47 vào `coding` (chờ merge).
- 29 test pass (sau khi thêm test bảo trì/nén/controller; toàn bộ dự án hiện 43 test, tính cả mô phỏng lỗi). Đã chạy app thật (JDK 17 tạm, bản sao DB cục bộ) và kiểm tra bằng Chromium headless: lưu cấu hình, lỗi validate, chạy bảo trì, nén lúc khởi động (192 KB → 24 KB), đóng/mở đăng ký, không tràn ngang ở 390px, không lỗi JS. Đã xem preview qua localtunnel rồi tắt.
- Jira: [QUYIT-962](https://nhquydev.atlassian.net/browse/QUYIT-962) (Task, Sprint 35, version Tháng 10/2026, ước lượng 6h, Relates QUYIT-961), trạng thái **Done** (chuyển ngày 2026-10-27 theo yêu cầu của người dùng khi PR [#47](https://github.com/nguyenquy0710/Webhook-MockAPI/pull/47) đã mở, chưa merge). Đã comment tiến độ ngày 2026-10-27 (retention log, test, thử DB lớn). Tính năng mô phỏng lỗi nằm ở [QUYIT-963](https://nhquydev.atlassian.net/browse/QUYIT-963), cùng nhánh và cùng chưa commit.

## 5. Việc còn mở (chưa làm, để quyết định sau)

- [x] Commit (`850cd6d`, `725e2ca`), push và mở PR [#47](https://github.com/nguyenquy0710/Webhook-MockAPI/pull/47) vào `coding`; chờ merge
- [x] Danh sách mục trong card General (4 mục: Site name, Public base URL, cho phép đăng ký, retention log) đã được xác nhận giữ nguyên, theo xác nhận của người dùng ngày 2026-10-27; retention giữ mặc định 0 (không tự xóa), không thêm mục mới
- [x] Giữ log theo số ngày: thêm mục "Request log retention (days)" vào General (0 = giữ hết, mặc định 0; tối đa 3650). Lần chạy bảo trì (tay và hằng ngày) xóa log cũ hơn N ngày trước khi `CHECKPOINT`. Không dùng `app.request-log-retention-days=30` trong cấu hình làm mặc định để không tự xóa log của hệ thống đang chạy
- [x] Thử bảo trì trên DB lớn: DB 354 MB với 80 nghìn log; đặt retention 7 ngày, chạy bảo trì xóa 69.329 log mất ~17 giây (tệp tạm phình lên 518 MB do bản ghi hoàn tác), app vẫn dùng được trong lúc chạy. Sau khi tắt app tệp còn 42 MB, lần khởi động kế tiếp nén còn ~2 MB (41.364 KB → 1.972 KB), tổng thời gian khởi động ~27 giây. Thiếu dung lượng đĩa thật chưa thử được trên máy; nhánh bỏ qua được kiểm bằng test đơn vị với hàm đo dung lượng giả (`StartupDatabaseCompactorTest`)
- [x] Test tự động: `DatabaseMaintenanceServiceTest` (6), `StartupDatabaseCompactorTest` (4, dùng H2 tệp thật: nén, bỏ qua khi chưa đặt lịch, bỏ qua khi thiếu dung lượng, DB/bảng chưa có), `AdminSettingsControllerTest` (5), `SettingsServiceTest` mở rộng (4), `RequestLogSearchTest` thêm test `purgeOlderThan`; tổng 29 test pass

## Cập nhật 2026-10-27 (hoàn tất các việc còn mở có thể tự làm)

- Đã làm: giữ log theo số ngày (mục retention trong General, mặc định 0 = giữ hết), test tự động cho bảo trì/nén/controller, thử trên DB 354 MB với 80 nghìn log (xóa 69.329 log ~17 giây; nén lúc khởi động 41.364 KB → 1.972 KB; khởi động ~27 giây).
- Còn lại: commit/PR; chủ sản phẩm xác nhận danh sách mục card General (4 mục hiện có); chưa thử thiếu dung lượng đĩa thật (chỉ test đơn vị với hàm đo dung lượng giả).
- Hạn chế đã biết: lần bảo trì xóa log lớn làm tệp tạm phình (354 → 518 MB trong lần thử) trước khi nén ở lần khởi động sau, cần dư đĩa.

## 6. Bài học rút ra (Tùy chọn)

- H2 không nén tệp an toàn khi pool đang mở: `SHUTDOWN COMPACT` phải chạy trước khi tạo DataSource.
- Mô tả bảo trì viết cho SQLite (WAL, VACUUM) cần ánh xạ sang H2 trước khi làm; hỏi người dùng chọn ánh xạ.

## 7. Tham khảo bên ngoài (Tùy chọn)

- Liên quan: `plans/2026-10-27-ui-redesign-screens.md` (bố cục admin, QUYIT-961).

## Cập nhật 2026-10-27 (xác nhận danh sách card General)

- Người dùng đồng ý danh sách 4 mục hiện có của card General, không thêm hay bớt mục nào; retention log giữ mặc định 0 (giữ hết).
- Các ứng viên chưa làm (múi giờ hiển thị, số dòng mỗi trang log, giới hạn số API mỗi người dùng, thời gian hết hạn bộ đếm mô phỏng lỗi, bật/tắt Swagger công khai) để ngỏ cho lần sau, chưa có yêu cầu.
- Việc còn lại duy nhất: commit/PR.
