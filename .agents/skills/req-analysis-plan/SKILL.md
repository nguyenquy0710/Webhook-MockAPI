---
name: req-analysis-plan
description: Phân tích yêu cầu người dùng thành bản kế hoạch triển khai chi tiết (decision-complete) cho dự án WebHookMock (Spring Boot, Java 17), lưu tại `plans/<slug>-plan.md`.
license: MIT
metadata:
  - author: QuyIT Team
  - version: 1.0.1
  - created: 2026-01-01
  - updated: 2026-04-16
subskills:
  - requirements-plan
  - project-sync
  - generate-changelog
---

# Skill: Phân Tích Yêu Cầu & Lập Kế Hoạch - WebHookMock

Mục tiêu: Chuyển đổi yêu cầu từ người dùng thành một bản kế hoạch triển khai hoàn chỉnh (decision-complete), có cấu trúc rõ ràng, bao quát các khía cạnh Backend API, Frontend Web, Database Schema và các ràng buộc kỹ thuật đặc thù của WebHookMock. Kế hoạch này sẽ được lưu trữ tại `plans/`.

## Khi dùng

- Người dùng đưa ra yêu cầu về tính năng mới, cải tiến, hoặc sửa lỗi cần được phân tích và lập kế hoạch triển khai.
- Cần làm rõ các yêu cầu về kiến trúc, database, API, và UI/UX trước khi bắt đầu code.
- Muốn tạo một tài liệu kế hoạch chi tiết, có thể dùng làm hướng dẫn cho các lập trình viên.

## Đầu vào

- **user_requirements**: Mô tả ban đầu về yêu cầu từ người dùng (ví dụ: "Tạo trang quản lý user có CRUD và phân trang").
- **context**: Trạng thái hiện tại của codebase (cấu trúc thư mục, models, API routes hiện có) và các ràng buộc đã được định nghĩa trong `README.md` hoặc `AGENTS.md`.

## Đầu ra

- Một file Markdown chi tiết, được sinh ra trong thư mục `plans/` với định dạng tên `<slug>-plan.md` (ví dụ: `admin-management-plan.md`).
- Nội dung file tuân thủ theo "Template Chuẩn" đã được định nghĩa trong skill `requirements-plan`.

## Quy trình Thực hiện

1.  **Grounding (Thu thập & Hiểu ngữ cảnh)**:
    -   Đọc kỹ `user_requirements` để nắm bắt mục tiêu nghiệp vụ.
    -   Sử dụng `grep_search` và `read_file` để rà soát các file liên quan trong `src/main/java/vn/autobot/webhook/model/`, `src/main/java/vn/autobot/webhook/controller/`, `src/main/java/vn/autobot/webhook/service/` để hiểu kiến trúc hiện tại.
    -   Kiểm tra các kế hoạch hiện có trong `plans/` và các tài liệu đặc tả trong `README.md` để đảm bảo tính nhất quán.

2.  **Phân tích & Thiết kế (Analysis & Design)**:
    -   **Database Schema (H2)**: Thiết kế hoặc cập nhật schema H2, bao gồm các fields, types, validators, và mối quan hệ (ví dụ: `@ManyToOne`, `@OneToMany`).
    -   **Backend API (Spring Boot)**: Xác định các API endpoints (`src/main/java/vn/autobot/webhook/controller/`), phương thức HTTP (GET, POST, PUT, DELETE), yêu cầu xác thực/phân quyền (Spring Security), và validation (Bean Validation).
    -   **Frontend Web (Thymeleaf)**: Phác thảo các UI templates (`src/main/resources/templates/`), fragments (`src/main/resources/templates/fragments/`), luồng điều hướng, và áp dụng tiêu chuẩn Bootstrap 5.
    -   **Service Layer**: Định nghĩa hoặc cập nhật các service methods (`src/main/java/vn/autobot/webhook/service/`) để xử lý business logic.

3.  **Lập kế hoạch Triển khai (Implementation Planning)**:
    -   Phân chia công việc thành các bước nhỏ, rõ ràng, có thứ tự ưu tiên.
    -   Xác định các file bị ảnh hưởng (`NEW`/`MODIFY`) và mô tả chi tiết thay đổi trong từng file.
    -   Lên kế hoạch kiểm thử (Unit/Integration Tests với JUnit/Spring Boot Test).

4.  **Tạo & Lưu File Kế Hoạch (Generate & Save Plan File)**:
    -   Tạo một file Markdown mới trong thư mục `plans/` với tên theo định dạng `<slug>-plan.md`.
    -   Điền nội dung kế hoạch theo "Template Chuẩn" được quy định trong skill `requirements-plan`.

---

## Template Chuẩn (Kế thừa từ `requirements-plan`)

```markdown
# 📋 Kế Hoạch: <Tên Tính Năng>

> **Mục tiêu**: Mô tả tóm tắt mục tiêu của tính năng.
> **Ưu tiên**: 🔴 Cao / 🟡 Trung bình / 🟢 Thấp
> **Cập nhật**: YYYY-MM-DD

---

## 📌 Tổng Quan
- Trạng thái hiện tại: ...
- Kết quả mong đợi: ...

---

## 🔧 Thay Đổi Kiến Trúc / Triển Khai (Proposed Changes)

### 1. Model & Database (`src/main/java/vn/autobot/webhook/model/`)
- **File**: `Entity.java`
- Bổ sung / Sửa đổi các Entity dùng chung.
- **Migration**: Cần thêm migration file `src/main/resources/db/migration/V{n}__Description.sql`

### 2. Backend – Controllers & Services (`src/main/java/vn/autobot/webhook/controller/`, `src/main/java/vn/autobot/webhook/service/`)
- **Controller**: Tạo mới endpoint ở `Controller.java`, tuân thủ pattern `/api/@{username}/{endpoint}`.
- **Service**: Bổ sung logic xử lý ở `Service.java`, xử lý delay, response template.
- **Security**: Cập nhật SecurityConfig nếu cần phân quyền.

### 3. Frontend – Templates & Assets (`src/main/resources/templates/`, `src/main/resources/static/`)
- **Templates**: Tạo mới template ở `templates/` hoặc cập nhật fragment ở `templates/fragments/`.
- **Assets**: Cập nhật CSS/JS ở `static/` nếu cần.

---

## 🔌 API Endpoints
| Method | Endpoint | Mô tả | Yêu cầu Quyền |
|--------|----------|-------|---------------|
| ...    | ...      | ...   | ...           |

---

## 🧪 Kế Hoạch Kiểm Thử (Verification Plan)
- **Unit Tests**: Viết Test cho Service ở `src/test/java/vn/autobot/webhook/service/`. Command: `./mvnw test`.
- **Integration Tests**: Viết Test cho Controller ở `src/test/java/vn/autobot/webhook/controller/`.
- **Manual UI Tests**: Các bước người dùng thao tác kiểm chứng trên giao diện Web, xử lý các luồng edge case...

---

## 📁 Files Affected (Dự Kiến)
| Loại | File | Mô tả |
|------|------|-------|
| NEW / MODIFY | ... | ... |
```

## Lưu ý Đặc biệt cho WebHookMock

1.  **"Decision-Complete"**: Mỗi kế hoạch phải đủ chi tiết để bắt đầu code ngay mà không cần thêm thông tin.
2.  **Spring Boot Architecture**: Luôn tham chiếu đúng đường dẫn trong Spring Boot (`src/main/java/vn/autobot/webhook/`, `src/main/resources/`).
3.  **Security & Authentication**: Các thiết kế liên quan đến dữ liệu và API phải tích hợp cơ chế Spring Security và xác thực User.
4.  **Bootstrap 5**: Các đề xuất về giao diện phải tuân thủ nghiêm ngặt Bootstrap 5 và Thymeleaf fragments.
5.  **Validation**: Kế hoạch phải bao gồm rõ ràng cách thức kiểm thử các thay đổi được đề xuất.
6.  **Flyway Migration**: Nếu có thay đổi database schema, luôn thêm migration file theo chuẩn `V{n}__Description.sql`.