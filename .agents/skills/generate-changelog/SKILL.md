---
name: generate-changelog
description: Tự động phân tích các thay đổi (commits/tasks), đề xuất entry mới cho `CHANGELOG.md` tuân thủ quy chuẩn Keep a Changelog của dự án WebHookMock.
license: MIT
metadata:
  - author: QuyIT Team
  - version: 1.0.1
  - created: 2026-01-01
  - updated: 2026-04-16
---

# Skill: Ghi Nhật Ký Thay Đổi (Generate Changelog) - WebHookMock

Mục tiêu: Tự động hóa việc tạo bản ghi thay đổi (changelog entry) dựa trên các task đã thực hiện, đảm bảo tính minh bạch, chuyên nghiệp và tuân thủ tuyệt đối định dạng Keep a Changelog tại `CHANGELOG.md`.

## Khi dùng

- Sau khi hoàn thành một tính năng (`feat`), sửa lỗi (`fix`), hoặc tái cấu trúc (`refactor`).
- Trước khi tạo Pull Request (PR) hoặc kết thúc một phiên làm việc.
- Người dùng yêu cầu "cập nhật changelog" hoặc "viết changelog cho các thay đổi vừa rồi".

## Đầu vào

- **tasks_completed**: Danh sách các công việc, commits hoặc thay đổi đã thực hiện.
- **branch_name**: Tên branch hiện tại (mặc định: `main` hoặc tự động lấy từ git).
- **author**: Tên người thực hiện (mặc định: `Antigravity (AI Assistant)` hoặc tên user).

## Đầu ra

- Một đoạn text định dạng Markdown chuẩn để chèn vào `CHANGELOG.md`.
- (Tùy chọn) Thực hiện cập nhật trực tiếp vào file `CHANGELOG.md` tại vị trí quy định.

## Quy trình Thực hiện

1. **Grounding (Thu thập dữ liệu)**:
   - Đọc phần đầu của `CHANGELOG.md` để xác định vị trí chèn (ngay sau dòng `## [Unreleased]` hoặc tạo mới nếu chưa có).
   - Tổng hợp các thay đổi từ `tasks_completed`.

2. **Phân loại (Classification)**:
   Xác định `type` phù hợp nhất:
   - `feat`: Tính năng mới.
   - `fix`: Sửa lỗi.
   - `refactor`: Tái cấu trúc code (không đổi tính năng).
   - `perf`: Tối ưu hiệu năng.
   - `docs`: Cập nhật tài liệu.
   - `chore`: Bảo trì, config, dependency.

3. **Soạn thảo (Drafting)**:
   Sử dụng Template chuẩn Keep a Changelog của dự án:

   ```markdown
   ## [Unreleased]

   ### Added
   - Tính năng mới 1
   - Tính năng mới 2

   ### Changed
   - Thay đổi 1
   - Thay đổi 2

   ### Fixed
   - Lỗi đã sửa 1
   - Lỗi đã sửa 2
   ```

   Hoặc nếu là release chính thức:

   ```markdown
   ## [1.0.X](https://github.com/nguyenquy0710/Webhook-MockAPI/compare/1.0.Y...1.0.X) - YYYY-MM-DD

   ### Added
   - Tính năng mới

   ### Changed
   - Thay đổi

   ### Fixed
   - Lỗi đã sửa
   ```

4. **Cập nhật (Action)**:
   - Chèn entry mới vào ngay phía dưới section `## [Unreleased]` (hoặc tạo section mới nếu là release).

---

## Lưu ý Đặc biệt cho WebHookMock

1. **Ngôn ngữ**: Luôn viết bằng **Tiếng Việt** chuyên nghiệp, súc tích.
2. **Định dạng**: Tuân thủ [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) với các mục: Added, Changed, Deprecated, Removed, Fixed, Security.
3. **Thứ tự**: Entry mới nhất luôn ở trên cùng (sau `## [Unreleased]`).
4. **Trung thực**: Chỉ ghi lại những thay đổi thực tế đã được thực thi và kiểm chứng.
5. **Semantic Versioning**: Tuân thủ [Semantic Versioning](https://semver.org/spec/v2.0.0.html) cho số phiên bản.
6. **Liên kết**: Luôn include GitHub commit hash links và PR links khi có thể.

## Ví dụ Sử dụng

**Input:**
- tasks: "Thêm API đăng nhập bằng Google, sửa lỗi hiển thị avatar ở Sidebar, cập nhật types cho User"
- branch: "feat/google-auth"

**Output Entry:**
```markdown
## [Unreleased]

### Added
- **Tích hợp Google Auth**: Triển khai Passport Google Strategy và route `/api/auth/google` cho phép người dùng đăng nhập bằng tài khoản Google.

### Changed
- **UI Sidebar**: Cập nhật giao diện Sidebar sau khi đăng nhập.

### Fixed
- **Avatar component**: Sửa lỗi hiển thị avatar trên Sidebar.
```
