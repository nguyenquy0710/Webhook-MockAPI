---
name: requirements-plan
description: Biến yêu cầu thành bản kế hoạch triển khai hoàn chỉnh (decision-complete) cho dự án WebHookMock (Spring Boot), thiết kế rõ Backend API, Frontend Web và tuân thủ định dạng lưu cấu trúc tại `plans/`.
license: MIT
metadata:
  - author: QuyIT Team
  - version: 1.0.1
  - created: 2026-01-01
  - updated: 2026-04-16
---

# Skill: Phân Tích Yêu Cầu & Lập Plan (Decision-Complete) - WebHookMock

Mục tiêu: Biến yêu cầu thành bản kế hoạch triển khai hoàn chỉnh (decision-complete) có thể bàn giao thực thi ngay và lưu vào `plans/` theo chuẩn đặt tên, tuân thủ đúng kiến trúc Spring Boot (Java 17) và các nguyên tắc thiết kế của dự án WebHookMock.

## Khi dùng

- Người dùng yêu cầu phân tích, lập kế hoạch cho một tính năng mới hoặc cập nhật module hệ thống.
- Cần thống nhất chi tiết về Database schema (H2), Backend API (Spring Boot), Frontend Web (Thymeleaf) và Kế hoạch kiểm thử tự động trước khi code.

## Đầu vào

- Mô tả ban đầu của người dùng.
- Hiện trạng của repository: cấu trúc thư mục (`src/main/java/vn/autobot/webhook/`, `src/main/resources/`), configs, controller hiện tại và H2 models.
- Các ràng buộc về phase dự án đã được mô tả tại `README.md` hoặc `AGENTS.md`.

## Đầu ra

- 1 file Markdown được sinh ra trong thư mục `plans/` với định dạng tên: `<slug>-plan.md` (ví dụ: `admin-management-plan.md`).

## Quy trình Thực hiện

1. **Grounding (Đọc hiểu repo)**
   - Quét qua cấu trúc thư mục và dùng công cụ xem nội dung file tham chiếu (`src/main/java/vn/autobot/webhook/model/`, `src/main/java/vn/autobot/webhook/controller/`, `src/main/java/vn/autobot/webhook/service/`).
   - Phân tích luồng dữ liệu giữa Controller, Service, Repository theo nguyên tắc: Controller xử lý request, Service xử lý business logic, Repository xử lý persistence.

2. **Elicitation (Làm rõ vấn đề)**
   - Chú ý các trade-off kỹ thuật: Bảo mật Spring Security, xác thực User, khả năng phân trang và tối ưu truy vấn H2.
   - Các giả định không thể hỏi người dùng ngay thì chọn phương án mặc định phù hợp nhất với Spring Security, Spring Data JPA.

3. **Phạm vi & Ràng buộc**
   - Phác thảo Rõ In-scope và Out-of-scope.
   - Đảm bảo giao diện theo bộ khung Bootstrap 5, thymeleaf fragments.

4. **Soạn plan (Decision-complete)**
   - Viết Plan theo chuẩn hệ thống WebHookMock, tách bạch các khu vực triển khai: JPA Entities, Spring Boot Controllers, Services, Thymeleaf Templates.

5. **Lưu file**
   - Viết trực tiếp nội dung vào file tại `plans/`.

---

## Template Chuẩn

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

## Lưu ý Đặc biệt

- Yêu cầu mọi thao tác Create/Update data ở backend phải xác thực Spring Security và kiểm tra quyền User.
- Các template sử dụng Thymeleaf fragments, Bootstrap 5.
- Không liệt kê thay đổi chung chung; mô tả cụ thể `Tên biến`, `Trường bắt buộc` và luồng nghiệp vụ.