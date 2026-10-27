---
type: refactor
complexity: medium
impact: medium
related_issues: []
related_prs: []
time_spent_hours: ~3
status: completed
---

# Kế hoạch: Thiết kế lại trang sửa API Configuration (/api-configs/{id})

> **Ngày:** 2026-10-27
> **Phạm vi:** `api-config-edit.html`, `WebhookViewController.updateApiConfig`, prototype `api-config-edit`
> **Trạng thái:** ✅ Đã hoàn thành (commit cục bộ trên `feature/api-config-edit-redesign`, chưa push/PR)

---

## 1. Bối cảnh

Trang sửa endpoint là một form dài một cột (hai trường mỗi hàng, không nhóm), nút Cancel/Update nằm cuối trang, danh sách biến ngữ cảnh chiếm chỗ giữa form, không có chỉ báo đang sửa endpoint nào ngoài tiêu đề chung. Sau issue #35 form có thêm `description` và `note` nên càng dài. Yêu cầu: sắp xếp lại thành phần cho thân thiện với người dùng (UX/UI), không đổi trường dữ liệu hay luồng lưu.

## 2. Công việc đã thực hiện

1. **Thiết kế trên prototype trước** (`docs/prototype/_gen/frag/api-config-edit.html`, theme bootstrap), chụp ảnh desktop và mobile, rồi áp dụng sang template thật. Lý do: chỉnh bố cục nhanh, không cần chạy app Spring.
2. **Bố cục mới:**
   - Header: breadcrumb `API Configs › Edit`, badge method và path đầy đủ, nút Copy URL, huy hiệu "Unsaved changes".
   - Cột trái, ba card: Endpoint (method, path, description), Response (status code, content type, delay, body, headers, accordion biến ngữ cảnh), Note (kèm bộ đếm `n/2000`).
   - Cột phải dính (`sticky-lg-top`): Summary (URL đầy đủ, status, content type, delay), nút Update API và Cancel; khung Delete riêng.
   - Bổ sung: chip chọn nhanh status code, nút Format JSON cho body và headers, summary cập nhật trực tiếp, cảnh báo khi rời trang có thay đổi chưa lưu, hộp thoại xác nhận xóa.
3. **Giữ nguyên** mọi `th:field`, tên trường và nhãn nút `Update API`/`Cancel`, để luồng POST `/api-configs/{id}` và test hiện có không đổi.
4. **Format JSON không chặn việc lưu:** body có thể dùng `{{biến}}` ở vị trí không phải chuỗi (ví dụ số), khi đó không phải JSON hợp lệ. Nút chỉ báo lỗi và định dạng khi hợp lệ; chuỗi có `{{` thì không tự định dạng. Đánh đổi: có thể lưu JSON sai, nhưng không chặn nhầm ca hợp lệ.
5. **Sửa lỗi có sẵn khi trang render lại sau lỗi** (`updateApiConfig`): trước đây id chỉ được gán sau bước validate nên form action thành `/api-configs/` và thiếu `domain`. Nay gán id trước và thêm `domain` ở cả hai nhánh lỗi.
6. **Lỗi phát hiện khi chạy thật:** trong `th:inline="javascript"`, viết `/*[[${username}]]*/ '' + path` làm Thymeleaf nuốt cả `'' + path`, header mất phần path sau khi đổi. Sửa bằng cách gán `user`, `base` ra biến riêng ngay đầu script.
7. **Nhánh làm việc:** theo quy tắc của project, tách `feature/api-config-edit-redesign` từ `origin/coding` (PR #37 đã merge vào `coding`, không phải `main`), cherry-pick các commit prototype/plan cần thiết.

## 3. Files đã thay đổi

| File | Thay đổi |
|---|---|
| [src/main/resources/templates/api-config-edit.html](../src/main/resources/templates/api-config-edit.html) | Viết lại bố cục theo thiết kế mới, thêm JS (chip, Format JSON, summary, dirty badge, copy URL) |
| [src/main/java/vn/autobot/webhook/controller/WebhookViewController.java](../src/main/java/vn/autobot/webhook/controller/WebhookViewController.java) | `updateApiConfig`: gán id trước validate, thêm `domain` ở nhánh lỗi |
| [docs/prototype/_gen/frag/api-config-edit.html](../docs/prototype/_gen/frag/api-config-edit.html) | Màn prototype thiết kế lại |
| [docs/prototype/_gen/actions.py](../docs/prototype/_gen/actions.py) | Hành vi `copy url` và `delete api` cho màn edit |
| [docs/prototype/project/api-config-edit.dc.html](../docs/prototype/project/api-config-edit.dc.html) | Đầu ra dựng từ nguồn |
| [plans/2026-10-27-api-config-edit-redesign.md](2026-10-27-api-config-edit-redesign.md) (**Mới**) | Nhật ký này |

## 4. Trạng thái hiện tại

- Đã commit cục bộ trên `feature/api-config-edit-redesign` (`80cf0d3` prototype, `e6b9dd4` template thật). Chưa push, chưa mở PR.
- Đã chạy app thật (JDK 17 tạm, bản sao repo, DB cũ): trang hiển thị đúng; chip, đổi path, summary, đếm ký tự, hộp thoại xóa hoạt động; lưu rồi nạp lại giữ giá trị; description 501 ký tự báo lỗi, form action vẫn đúng id; không có lỗi JS.
- Chưa có Jira cho đợt này.

## 5. Việc còn mở (chưa làm, để quyết định sau)

- [ ] Push nhánh và mở PR vào `coding`
- [ ] Tạo Jira (QUYIT) cho đợt thiết kế lại và liên kết với plan này
- [ ] Kiểm tra thủ công nút Copy URL, Format JSON trên body không có placeholder, và giao diện mobile của trang thật
- [ ] Áp dụng cùng bố cục cho form tạo mới trong `api-config.html` (đang là form cũ trong card thu gọn), nếu muốn đồng nhất
- [ ] Không có test tự động cho view; cân nhắc test controller nếu cần

## 6. Bài học rút ra (Tùy chọn)

- `th:inline="javascript"`: dấu `/*[[...]]*/ <giá trị mẫu>` nuốt phần giá trị mẫu theo sau, không viết biểu thức nối chuỗi ngay sau; gán vào biến riêng.
- Dựng prototype trước giúp chốt bố cục nhanh, nhưng lỗi như trên chỉ lộ khi chạy template thật.
- `pgrep -f`/`pkill -f` trong cùng lệnh shell có thể khớp chính tiến trình shell đó; dùng dạng `[w]ord` hoặc kill theo PID.

## 7. Tham khảo bên ngoài (Tùy chọn)

- Liên quan: `plans/2026-10-27-api-config-description-planning.md` (issue #35, thêm `description` và `note`).
