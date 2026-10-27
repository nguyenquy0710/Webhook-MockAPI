---
type: feature
complexity: low
status: completed
related_issues: [#35, QUYIT-960]
related_prs: [#37]
estimated_hours: ~5
---

# Kế hoạch: Thêm trường Description và Note cho endpoint trong API Configurations

> **Ngày lập kế hoạch:** 2026-10-27
> **Scope dự kiến:** `ApiConfig`, `ApiConfigDto`, `ApiMockService`, `ApiConfigService`, `SwaggerService`, `WebhookViewController`, migration mới, `api-config.html`, `api-config-edit.html`
> **Priority:** medium

---

## 1. Phân tích / Bối cảnh

Issue [#35](https://github.com/nguyenquy0710/Webhook-MockAPI/issues/35): người dùng/tester cần ghi chú mục đích của từng mock endpoint nhưng hệ thống không có chỗ lưu.

Acceptance criteria của issue:
- Form Create/Edit có trường ghi chú, không bắt buộc, placeholder gợi ý.
- Danh sách endpoint hiển thị ghi chú (sub-text dưới path hoặc tooltip/icon).
- Lưu vào database/payload.

Quyết định đã chốt (2026-10-27):
- Có **hai trường**: `description` (tối đa 500 ký tự) và `note` (tối đa 2000 ký tự).
- Placeholder bằng **tiếng Anh**, nhất quán với UI hiện tại.
- Swagger `summary` lấy từ ghi chú.

Hiện trạng: `delayMs` đã đi qua đủ các lớp (model, DTO, `ApiMockService.saveApiConfig`/`convertToDto`, `ApiConfigService.convertToDto`/`restoreApiConfigs`, hai form), nên dùng làm mẫu.

Constraint:
- `application.properties` bật cả Flyway lẫn `ddl-auto=update`; `out-of-order=false`.
- `plans/2026-04-16-webhook-retry-planning.md` cũng định dùng migration số cao hơn (đã đổi sang V5, xem mục 2).
- Chưa có thư mục test, `pom.xml` chưa có `spring-boot-starter-test`.

## 2. Approach / Strategy

Đi theo đường của `delayMs`, không đổi kiến trúc.

**Phân vai hai trường** (giả định từ quyết định "description 500, note 2000"; nếu thực ra chỉ cần một trường thì bỏ `note`, mọi bước còn lại giữ nguyên):

| Trường | Giới hạn | Vai trò | Hiển thị |
|---|---|---|---|
| `description` | 500 ký tự | Mô tả ngắn mục đích endpoint | Sub-text `text-muted small` dưới path trong bảng; làm Swagger `summary` |
| `note` | 2000 ký tự | Ghi chú dài: luồng test, lưu ý | Icon info cạnh path, bấm/hover hiện popover chứa toàn văn |

- **Kiểu dữ liệu:** `description VARCHAR(500)`, `note VARCHAR(2000)`, cả hai nullable; `@Size` tương ứng ở DTO; `@Column(length = …)` khớp migration. Không dùng CLOB vì 2000 ký tự vẫn nhỏ.
- **Placeholder (tiếng Anh):**
  - `description`: "Short purpose of this endpoint, e.g. returns a mocked order".
  - `note`: "Enter a note or describe the purpose of this endpoint..." (dựa trên placeholder của issue).
- **Danh sách:** popover dùng `data-bs-trigger="focus"` để bấm được trên mobile; nội dung gán qua `th:attr` (tự escape). Cắt dòng `description` bằng `text-truncate`, `title` chứa toàn văn.
- **Swagger:** `summary` = `description` nếu có; nếu trống thì giữ "Mock API for …". `note` là ghi chú nội bộ của tester nên không đưa vào Swagger.
- **Số phiên bản migration:** plan này lấy **V4** (`V4__Add_description_and_note_columns.sql`) vì hiện mới có V1–V3 và kế hoạch retry chưa triển khai; `2026-04-16-webhook-retry-planning.md` đã được đổi sang **V5**. Không lấy V5 trước rồi để retry giữ V4: `out-of-order=false` và `ignore-ignored-migrations=false` nên V4 xuất hiện sau khi V5 đã áp dụng sẽ làm Flyway báo lỗi validate.
- **Thứ tự Flyway và Hibernate:** Spring Boot chạy Flyway trước khi khởi tạo JPA, nên cột do migration tạo trước, `ddl-auto=update` chỉ thấy cột đã có. Vẫn dùng `ADD COLUMN IF NOT EXISTS` và `@Column` khớp đúng kiểu/độ dài của migration.
- **Backup/restore:** thêm cả hai trường vào `restoreApiConfigs`; file backup cũ không có trường vẫn restore được (Jackson bỏ qua trường thiếu).
- **Chuỗi rỗng:** textarea rỗng được Spring bind thành `""`, không phải `null`. Chuẩn hóa trong `ApiMockService.saveApiConfig`: `trim()`, rỗng thì lưu `null`; template ẩn dòng/icon khi `null` hoặc rỗng.
- **Mất dữ liệu khi sửa:** `saveApiConfig` ghi đè mọi trường từ DTO, nên form Edit **bắt buộc** có cả hai ô, nếu không mỗi lần sửa endpoint sẽ xóa ghi chú.
- **Bảo mật:** render bằng `th:text`/`th:attr` (tự escape), không dùng `th:utext`, tránh XSS.

Phương án không chọn: lưu ghi chú trong `responseHeaders` hoặc bảng riêng, vì thừa phức tạp cho hai cột text.

## 3. Công việc cần thực hiện (Todo)

- [x] Trước khi tạo file migration: `ls src/main/resources/db/migration` và kiểm tra các nhánh/PR đang mở có `V4` chưa; có rồi thì lấy số kế tiếp và cập nhật lại plan này
- [x] Migration `V4__Add_description_and_note_columns.sql`: `ALTER TABLE api_configs ADD COLUMN IF NOT EXISTS description VARCHAR(500);` và `ADD COLUMN IF NOT EXISTS note VARCHAR(2000);`
- [x] `ApiConfig.java`: thêm `description` (`length = 500`) và `note` (`length = 2000`)
- [x] `ApiConfigDto.java`: thêm `description` với `@Size(max = 500)` và `note` với `@Size(max = 2000)`, message lỗi tiếng Anh
- [x] `ApiMockService.java`: set/get hai trường trong `saveApiConfig` (~dòng 58) và `convertToDto` (~dòng 256), kèm trim và rỗng thành `null`
- [x] `ApiConfigService.java`: hai trường trong `convertToDto` và `restoreApiConfigs`
- [x] `api-config.html` (form tạo): ô `description` (input, `maxlength="500"`) và ô `note` (textarea, `maxlength="2000"`, đếm ký tự)
- [x] `api-config.html` (bảng): `description` dưới path (cắt dòng, `title` toàn văn); icon info + popover cho `note`
- [x] `api-config-edit.html`: cả hai ô như form tạo
- [x] `WebhookViewController.saveApiConfig`: nhánh lỗi validate render lại `api-config` nhưng không `addAttribute("domain", …)` (lỗi có sẵn, URL hiện thành `null/api/…`); thêm `domain` vào nhánh lỗi vì lỗi quá dài sẽ đi qua nhánh này
- [x] `SwaggerService.java`: `op.put("summary", …)` lấy `description` khi có, ngược lại giữ "Mock API for …"
- [x] Kiểm tra migration trên hai DB: DB trống (Flyway tạo cột) và DB cũ đã có dữ liệu với `ddl-auto=update` (không lỗi cột trùng, log `Database migration status: SUCCESS`)
- [x] `pom.xml` chưa có `spring-boot-starter-test` và chưa có `src/test`: thêm dependency (scope test) trước khi viết test, hoặc chốt chỉ kiểm tra thủ công
- [x] Test tự động (`ApiConfigDescriptionNoteTest` 4 test và `ApiConfigBackupRestoreTest` 2 test, đều pass): trim, backup/restore giữ nguyên hai trường, restore file backup cũ không có hai trường; `description` > 500 và `note` > 2000 bị từ chối; chuỗi rỗng lưu thành `null`; Swagger `summary` đúng
- [x] Chạy thử trên app thật (đăng nhập, tạo, sửa, backup/restore, xem danh sách, xem `/swagger/@user`)
- [x] Cập nhật prototype `api-config` và `api-config-edit` trong `docs/prototype/_gen/frag/` với hai ô và popover
- [x] `CHANGELOG.md` do `auto-changelog` sinh từ commit, không sửa tay: đã dùng commit message dạng conventional (`feat: …`) để mục này tự sinh khi release

## 4. Risks & Unknowns

- **Risk 1:** Hibernate (`ddl-auto=update`) và Flyway cùng quản lý schema, cột có thể bị tạo hai lần hoặc lệch kiểu → **Mitigation:** Flyway chạy trước JPA theo mặc định của Spring Boot; migration dùng `ADD COLUMN IF NOT EXISTS`; `@Column` khớp kiểu; kiểm tra trên cả DB trống và DB cũ (mục Todo).
- **Risk 2:** Trùng số phiên bản migration `V4` với `2026-04-16-webhook-retry-planning.md` → **Mitigation:** plan này giữ V4, retry plan đã đổi sang V5; trước khi tạo file kiểm tra thư mục migration và các nhánh đang mở. Nếu retry được merge trước thì lấy V5 cho migration này và sửa lại plan, không bao giờ để số thấp hơn xuất hiện sau số cao đã áp dụng.
- **Risk 3:** XSS qua nội dung ghi chú → **Mitigation:** chỉ dùng `th:text`/`th:attr`.
- **Risk 4:** Backup cũ/mới không tương thích → **Mitigation:** trường mới nullable, Jackson bỏ qua trường thiếu; kiểm tra bằng test restore.
- **Risk 5:** Ghi chú rỗng bị lưu là `""` làm hiện dòng/icon trống → **Mitigation:** chuẩn hóa về `null` ở service, ẩn khi rỗng.
- **Risk 6:** Form Edit thiếu ô làm mất ghi chú khi sửa → **Mitigation:** mục Todo bắt buộc; kiểm tra thủ công sửa endpoint có ghi chú.
- **Risk 7:** Nhánh lỗi validate của `POST /api-configs` thiếu `domain` → **Mitigation:** mục Todo ở trên.
- **Risk 8:** Không có hạ tầng test → **Mitigation:** thêm `spring-boot-starter-test` hoặc ghi rõ kiểm tra thủ công.
- **Risk 9:** `description` dài hoặc có ký tự đặc biệt làm Swagger `summary` xấu → **Mitigation:** `summary` lấy đúng `description` (đã giới hạn 500); `ObjectMapper` tự escape JSON.
- **Unknown 1:** Hai trường `description` + `note` có đúng ý người đề xuất không (issue chỉ nói một trường) → **Plan:** ghi chú vào issue #35 và xác nhận trước khi code; nếu chỉ cần một thì bỏ `note`.

## 4b. Kết quả triển khai (2026-10-27, chưa commit)

Đã code xong và kiểm tra trên app thật (JDK 17 tạm, bản sao repo):
- Migration V4 chạy tốt trên **DB cũ** (`Successfully applied 1 migration … version v4`) và **DB trống** (`applied 4 migrations`); log `Database migration status: SUCCESS`.
- Tạo endpoint với `description`/`note`: lưu và hiển thị đúng; `description` dưới path, icon info + popover cho `note`.
- Chuỗi toàn khoảng trắng lưu thành `null`; `description` 501 ký tự báo "Description must be at most 500 characters", trang vẫn render, URL không còn `null/api`.
- `note` chứa `<script>` được escape (`&lt;script&gt;`), không thực thi.
- Form Edit nạp đúng giá trị; sửa endpoint giữ nguyên ghi chú.
- Backup có hai trường; restore file backup cũ (không có trường) và mới đều thành công.
- Swagger `summary` lấy từ `description`, fallback "Mock API for …".
- Đã thêm `spring-boot-starter-test` (scope test) và 6 unit test (4 + 2 cho backup/restore): trim, chuỗi rỗng thành `null`, ranh giới 500/2000 ký tự (đúng giới hạn qua, vượt bị từ chối), Swagger `summary` và fallback. Đều pass.
- Prototype `api-config` và `api-config-edit` đã thêm hai ô, dòng `description` và popover `note`; `interact.py --all --strict` sạch.
- Bộ đếm ký tự cho Note (`n/2000`) đã thêm vào hai template và prototype.
- Còn lại: push/PR, chuyển QUYIT-960 sang Done sau khi merge.

## 4c. Cập nhật (2026-10-27)

- Jira: [QUYIT-960](https://nhquydev.atlassian.net/browse/QUYIT-960), Task trong QUYIT, Sprint 35, version `Tháng 10/2026`, ước lượng 5h, label `webhook-mockapi`/`github-35`, liên kết "Relates" tới QUYIT-162 (Webhook MockAPI). Không gắn epic vì chưa có epic phù hợp.
- Trạng thái Jira: **In Progress** (code xong nhưng chưa commit/PR; chuyển Done sau khi merge).
- Việc còn lại ngoài plan: push/PR, chuyển QUYIT-960 sang Done sau khi merge. Đã ghi xác nhận vào GitHub issue #35 (xem mục 4d).

## 4d. Xác nhận với người đề xuất (2026-10-27)

- Đã comment vào GitHub issue #35 ([link](https://github.com/nguyenquy0710/Webhook-MockAPI/issues/35#issuecomment-6433714985)): tóm tắt phần đã làm và hỏi giữ hai trường (`description` + `note`) hay chỉ một. Mặc định giữ cả hai nếu không có ý kiến khác.
- Trạng thái: **chờ phản hồi**. Nếu chọn một trường thì bỏ `note` (migration V4 chưa phát hành nên có thể sửa trực tiếp).
- Đã commit cục bộ (2 commit), chưa push. QUYIT-960 giữ **In Progress**, chuyển Done sau khi merge.

## 4e. Hoàn tất (2026-10-27)

- PR [#37](https://github.com/nguyenquy0710/Webhook-MockAPI/pull/37) đã merge vào `main` (`47a2d44`) và đã deploy.
- QUYIT-960 chuyển **Done**; GitHub issue #35 đã đóng (completed).
- Câu hỏi hai trường ở #35 không nhận phản hồi trước khi merge: giữ cả hai trường. Muốn đổi sang một trường thì mở issue/PR mới (cần migration mới, không sửa V4 vì đã deploy).

## 5. Success Criteria

- [x] Tạo và sửa endpoint với `description`/`note` hoặc để trống đều thành công. (đã kiểm tra thủ công trên app thật)
- [x] Danh sách hiển thị `description` dưới path và icon popover cho `note`; bản ghi cũ không có hai trường này vẫn hiển thị bình thường. (đã kiểm tra trên app thật và ảnh chụp)
- [x] Backup rồi restore giữ nguyên hai trường; restore file backup cũ không lỗi. (có 2 unit test và đã kiểm tra thủ công)
- [x] Swagger `summary` lấy từ `description`, fallback giữ nguyên như hiện tại. (có unit test và kiểm tra `/swagger/@admin`)
- [x] Migration chạy được trên database mới và database đã có dữ liệu. (DB cũ (v4) và DB trống (4 migration) đều SUCCESS)
- [x] Nội dung chứa HTML/script được hiển thị dạng chữ, không thực thi. (`<script>` được escape thành `&lt;script&gt;`)
- [x] Vượt giới hạn ký tự (500/2000) báo lỗi validate bằng tiếng Anh, trang vẫn render đúng. (có unit test cho 500/2000; thông báo lỗi tiếng Anh, trang vẫn render, đã kiểm tra với 501 ký tự)

## 6. Questions / Dependencies (Tùy chọn)

- Xác nhận với người đề xuất issue #35: cần hai trường `description` và `note` như plan, hay một trường là đủ?
- Phụ thuộc `plans/2026-04-16-webhook-retry-planning.md` về số phiên bản migration: đã thống nhất plan này = V4, retry = V5.
