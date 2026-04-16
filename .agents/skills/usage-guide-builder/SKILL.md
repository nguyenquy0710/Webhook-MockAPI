---
name: usage-guide-builder
description: Skill for generating high-quality, modular developer and user documentation (EN/VN) for Spring Boot projects like WebHookMock.
license: MIT
metadata:
  - author: QuyIT Team
  - version: 1.0.1
  - created: 2026-01-01
  - updated: 2026-04-16
---

# Skill: Xây Dựng Hướng Dẫn Sử Dụng (Usage Guide Builder) - WebHookMock

Mục tiêu: Cung cấp một quy trình có cấu trúc để tạo ra tài liệu hướng dẫn phát triển và sử dụng toàn diện, mô-đun, và song ngữ (Anh/Việt) cho dự án WebHookMock (Spring Boot, Java 17, H2, Thymeleaf). Đảm bảo tài liệu luôn phản ánh chính xác kiến trúc và quy trình làm việc hiện tại.

## Khi dùng

- Khi cần tạo tài liệu hướng dẫn mới cho một tính năng, một module, hoặc một quy trình cụ thể.
- Khi có sự thay đổi lớn về kiến trúc, công nghệ, hoặc quy trình phát triển cần được cập nhật trong tài liệu.
- Khi người dùng yêu cầu "viết tài liệu hướng dẫn" hoặc "chuẩn hóa các file tài liệu".

## Đầu vào

- **doc_target**: Chủ đề hoặc phạm vi tài liệu cần tạo (ví dụ: "hướng dẫn cài đặt", "kiến trúc API", "quy trình deployment").
- **context**: Toàn bộ ngữ cảnh dự án (cấu trúc thư mục, `pom.xml`, `application.properties`, `Dockerfile`, `docker-compose.yml`, v.v.).

## Đầu ra

- Các file Markdown (.md) được tạo hoặc cập nhật trong thư mục `docs/` hoặc `README.md` ở thư mục gốc.
- Nội dung tài liệu tuân thủ cấu trúc và nguyên tắc viết đã định.

## 📋 Kiến Trúc Tài Liệu (Documentation Architecture)

Bộ tài liệu cho WebHookMock nên được mô-đun hóa và lưu trữ trong thư mục `docs/`:

1.  **`README.md` (root)**: Điểm vào chính của dự án, tóm tắt tổng quan và dẫn link đến các tài liệu chi tiết.
2.  **`AGENTS.md`**: Hướng dẫn cho AI agents về các lệnh dev, kiến trúc, và các file quan trọng.
3.  **`docs/INSTALLATION.md`**: Hướng dẫn cài đặt môi trường phát triển (Java 17, Maven, H2, Docker).
4.  **`docs/ARCHITECTURE.md`**: Tổng quan kiến trúc Spring Boot (controllers, services, repositories, models).
5.  **`docs/DEVELOPMENT.md`**: Quy trình phát triển cục bộ (chạy với Maven wrapper, testing, formatting).
6.  **`docs/DEPLOYMENT.md`**: Hướng dẫn triển khai (JAR standalone, Dockerization, CI/CD pipelines).
7.  **`CHANGELOG.md`**: Nhật ký thay đổi (sử dụng skill `generate-changelog`).

## 🔄 Quy trình Xây Dựng Tài Liệu (Workflow for Doc Generation)

### Phase 1: Trích xuất & Tổng hợp Kiến thức (Knowledge Extraction)
-   Sử dụng `list_directory`, `read_file`, `grep_search` để thu thập thông tin từ:
    -   `pom.xml`: Danh sách dependencies, plugins, build configuration.
    -   `src/main/resources/application.properties`: Cấu hình ứng dụng.
    -   `src/main/java/vn/autobot/webhook/`: Cấu trúc package, controllers, services, models.
    -   `Dockerfile`, `docker-compose.yml`: Chi tiết triển khai Docker.
    -   `.github/workflows/`: CI/CD pipelines.
    -   Các file `SKILL.md` khác: Tham khảo quy trình của các skill liên quan.

### Phase 2: Thiết kế & Cấu trúc (Design and Structure)
-   Xác định rõ ràng mục tiêu và đối tượng của từng tài liệu.
-   Sử dụng GitHub Flavored Markdown (bảng, khối code, cảnh báo, checklist).
-   Kết hợp emoji, ảnh chụp màn hình (nếu có thể tự tạo) và sơ đồ Mermaid để tăng tính trực quan.
-   Tổ chức tài liệu vào các thư mục con thích hợp trong `docs/`.

### Phase 3: Nguyên tắc Viết (Writing Principles)
-   **Ưu tiên hành động (Actionable)**: Cung cấp các bước rõ ràng, dễ thực hiện, với các lệnh copy-paste cụ thể.
-   **Maven Wrapper**: Luôn sử dụng `./mvnw` (hoặc `mvnw.cmd` trên Windows) thay vì `mvn` để đảm bảo tính nhất quán.
-   **Bảo mật (Security)**: Luôn cảnh báo về việc không commit database file (`./data/`) hoặc làm lộ các secrets.
-   **Bản địa hóa (Localization)**: Cung cấp phiên bản tiếng Việt đầy đủ, dịch thuật chính xác các thuật ngữ kỹ thuật và quy trình.
-   **Tính nhất quán**: Đảm bảo thuật ngữ, định dạng và phong cách xuyên suốt các tài liệu.

## 🧪 Best Practices
-   **Sử dụng Bảng (Tables)**: Để trình bày biến môi trường, scripts, dependencies.
-   **Liên kết Ngữ cảnh (Contextual Links)**: Liên kết chéo giữa các tài liệu để cung cấp thông tin liên quan mà không làm tài liệu bị dài dòng.
-   **Sơ đồ (Diagrams)**: Đề xuất sử dụng Mermaid để tạo sơ đồ kiến trúc, luồng dữ liệu hoặc cây phụ thuộc.
-   **Thường xuyên cập nhật**: Tài liệu phải là một "living document", được cập nhật định kỳ cùng với mã nguồn.

## 📂 Kết quả Đầu ra Dự kiến
-   Tất cả tài liệu được lưu trữ trong thư mục `docs/` (nếu cần) hoặc ở thư mục gốc.
-   `README.md` (gốc) đóng vai trò là điểm truy cập chính, có các liên kết rõ ràng đến các tài liệu chi tiết.
-   `AGENTS.md` đóng vai trò là hướng dẫn nhanh cho AI agents.
-   Mọi cập nhật mã nguồn hoặc quy trình phải dẫn đến việc rà soát và cập nhật các tài liệu liên quan.