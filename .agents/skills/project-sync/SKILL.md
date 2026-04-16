---
name: project-sync
description: Đồng bộ hóa toàn diện tài liệu dự án (README.md, AGENTS.md, CHANGELOG.md) và các file cấu hình dựa trên thực tế mã nguồn WebHookMock.
license: MIT
metadata:
  - author: QuyIT Team
  - version: 1.0.1
  - created: 2026-01-01
  - updated: 2026-04-16
subskills:
  - generate-changelog
---

# Skill: Đồng Bộ Hóa Dự Án (Project Sync) - WebHookMock

Mục tiêu: Đảm bảo "Nguồn Sự Thật" (Source of Truth) của dự án luôn nhất quán. Đồng bộ hóa giữa mã nguồn thực tế với các tài liệu hướng dẫn (README.md, AGENTS.md, .github/copilot-instructions.md) và nhật ký thay đổi (CHANGELOG.md).

## Khi dùng

- Sau khi hoàn thành một tính năng, sửa lỗi hoặc refactor lớn.
- Khi có sự thay đổi về kiến trúc, database schema, dependency hoặc tech stack.
- Trước khi release phiên bản mới.
- Người dùng yêu cầu: "Cập nhật tiến độ dự án", "Đồng bộ lại tài liệu", hoặc "Kiểm tra xem tài liệu còn đúng không".

## Đầu vào

- **context**: Trạng thái hiện tại của repo (Git logs, file changes).
- **docs**: Các file tài liệu chính: README.md, AGENTS.md, CHANGELOG.md, .github/copilot-instructions.md.

## Đầu ra

- Các file tài liệu được cập nhật trạng thái mới nhất.
- Một báo cáo tóm tắt về các phần đã đồng bộ.

## Quy trình Thực hiện

1. **Grounding (Rà soát Thực tế)**:
   - Sử dụng `git status` và `git diff` để xem các thay đổi chưa được ghi nhận.
   - Quét thư mục `src/main/java/vn/autobot/webhook/controller/` để tìm API endpoints mới.
   - Quét `src/main/java/vn/autobot/webhook/model/` để tìm thay đổi Entity/Schema.
   - Quét `src/main/resources/db/migration/` để tìm migration mới.
   - Kiểm tra `pom.xml` để xác định dependency mới.

2. **Cập nhật AGENTS.md**:
   - Cập nhật Dev Commands nếu có command mới.
   - Cập nhật Architecture nếu có thay đổi entry point, path, port, database.
   - Cập nhật Key Files nếu có module/controller/service mới.

3. **Cập nhật .github/copilot-instructions.md**:
   - Cập nhật build/test/run commands nếu có thay đổi.
   - Cập nhật architecture overview nếu có thay đổi lớn.
   - Cập nhật key conventions nếu có pattern mới.

4. **Ghi Nhật ký Thay đổi (CHANGELOG.md)**:
   - Sử dụng skill `generate-changelog` để tạo entry mới.
   - Đảm bảo entry tuân thủ Keep a Changelog format.

5. **Cập nhật README.md**:
   - Cập nhật phần Features nếu có tính năng mới.
   - Cập nhật Technology Stack nếu có dependency mới.
   - Cập nhật What's New nếu có thay đổi đáng kể.

6. **Kiểm tra Nhất quán (Final Validation)**:
   - Đảm bảo không có sự mâu thuẫn giữa các file tài liệu.
   - Đảm bảo CHANGELOG.md, README.md, AGENTS.md đều phản ánh đúng thực tế code.

---

## Lưu ý Đặc biệt cho WebHookMock

1. **Spring Boot Project**: Đây là dự án Spring Boot 2.7.9, không phải monorepo Next.js. Không có cấu trúc `apps/`, `packages/`.
2. **Kiến trúc phân tầng**: Controller → Service → Repository → Model/Entity.
3. **Database**: H2 file-based với Flyway migration.
4. **Webhook Pattern**: `/api/@{username}/{endpoint}` là pattern đặc trưng.
5. **Tech Stack**: Java 17, Spring Boot, Spring Security, Thymeleaf, Bootstrap 5, WebSocket (STOMP), Apache POI.
6. **Định dạng Changelog**: Tuân thủ Keep a Changelog và Semantic Versioning.
7. **CI/CD**: GitHub Actions với Maven wrapper (`./mvnw`).

## Công cụ Hỗ trợ

- Sử dụng `grep_search` để tìm các TODO hoặc FIXME còn sót lại.
- Sử dụng `run_shell_command` với `git log --pretty=format:"%s" -n 20` để lấy danh sách thay đổi nhanh.
- Sử dụng `git diff` để xem chi tiết thay đổi giữa các commit.
