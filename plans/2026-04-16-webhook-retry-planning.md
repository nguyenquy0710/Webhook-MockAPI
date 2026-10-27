---
type: feature
complexity: medium
status: completed
related_issues: [QUYIT-963]
related_prs: []
---

# 📋 Kế Hoạch: Webhook Retry Configuration

> **Trạng thái (2026-10-27):** ✅ Đã triển khai theo hướng **mô phỏng lỗi rồi thành công** (xem mục "Cập nhật 2026-10-27" ở cuối). Các mục bên dưới là kế hoạch gốc; tên trường, endpoint và migration thực tế khác kế hoạch gốc, bảng đối chiếu nằm ở mục cuối.

> **Mục tiêu**: Thêm khả năng tự động retry khi webhook request thất bại, giúp tăng độ tin cậy của hệ thống webhook.
> **Ưu tiên**: 🟡 Trung bình
> **Cập nhật**: 2026-04-16

---

## 📌 Tổng Quan
- **Trạng thái hiện tại**: Webhook không có cơ chế retry khi request thất bại
- **Kết quả mong đợi**: Webhook có thể tự động retry với cấu hình linh hoạt (số lần retry, khoảng cách giữa các lần retry, điều kiện retry)

---

## 🔧 Thay Đổi Kiến Trúc / Triển Khai (Proposed Changes)

### 1. Model & Database (`src/main/java/vn/autobot/webhook/model/`)
- **File**: `ApiConfig.java`
- **Thay đổi**: Thêm các trường mới:
  - `retryEnabled` (Boolean, default: false) - Bật/tắt retry
  - `maxRetries` (Integer, default: 3) - Số lần retry tối đa
  - `retryDelayMs` (Integer, default: 1000) - Khoảng cách giữa các lần retry (ms)
  - `retryOnStatusCodes` (String, CLOB) - Danh sách status codes trigger retry (JSON array)

**Migration**: `src/main/resources/db/migration/V6__Add_failure_simulation.sql` (V4 dành cho `V4__Add_description_column.sql`, xem `2026-10-27-api-config-description-planning.md`)

```sql
ALTER TABLE api_configs ADD COLUMN retry_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE api_configs ADD COLUMN max_retries INTEGER DEFAULT 3;
ALTER TABLE api_configs ADD COLUMN retry_delay_ms INTEGER DEFAULT 1000;
ALTER TABLE api_configs ADD COLUMN retry_on_status_codes CLOB DEFAULT '[408, 429, 500, 502, 503, 504]';
```

### 2. Backend – Controllers & Services (`src/main/java/vn/autobot/webhook/controller/`, `src/main/java/vn/autobot/webhook/service/`)
- **Controller**: `ApiConfigController.java`
  - Thêm endpoint `PUT /api/configs/retry/{id}` để cập nhật retry config
  - Thêm endpoint `GET /api/configs/retry/{id}` để lấy retry config

- **Service**: `ApiMockService.java`
  - Cập nhật `processWebhook()` để hỗ trợ retry logic
  - Thêm method `processWithRetry()` để xử lý retry
  - Thêm method `shouldRetry()` để kiểm tra điều kiện retry

- **DTO**: `ApiConfigDto.java`
  - Thêm fields: `retryEnabled`, `maxRetries`, `retryDelayMs`, `retryOnStatusCodes`

### 3. Frontend – Templates & Assets (`src/main/resources/templates/`, `src/main/resources/static/`)
- **Templates**: `api-config.html`, `api-config-edit.html`
  - Thêm section "Retry Configuration" với các fields:
    - Checkbox: "Enable Retry"
    - Number input: "Max Retries" (1-10)
    - Number input: "Delay (ms)" (100-60000)
    - Multi-select: "Retry on Status Codes" (408, 429, 500, 502, 503, 504)

- **Assets**: `static/js/webhook-config.js`
  - Thêm logic xử lý retry configuration form
  - Thêm validation cho retry fields

---

## 🔌 API Endpoints
| Method | Endpoint | Mô tả | Yêu cầu Quyền |
|--------|----------|-------|---------------|
| GET | /api/configs/retry/@{username} | Lấy danh sách retry configs | User |
| GET | /api/configs/retry/{id} | Lấy retry config chi tiết | User |
| PUT | /api/configs/retry/{id} | Cập nhật retry config | User |
| POST | /api/configs | Tạo config với retry (include retry fields) | User |

---

## 🧪 Kế Hoạch Kiểm Thử (Verification Plan)
- **Unit Tests**: 
  - `ApiMockServiceTest.testProcessWithRetrySuccess()`
  - `ApiMockServiceTest.testProcessWithRetryExhausted()`
  - `ApiMockServiceTest.testShouldRetryCondition()`

- **Integration Tests**:
  - Test retry logic end-to-end với mock external service
  - Test validation retry configuration

- **Manual UI Tests**:
  1. Tạo webhook config với retry enabled
  2. Configure maxRetries=3, delayMs=2000
  3. Simulate failure response (500)
  4. Verify 3 retries xảy ra với đúng delay
  5. Verify success sau retries

---

## 📁 Files Affected (Dự Kiến)
| Loại | File | Mô tả |
|------|------|-------|
| MODIFY | `ApiConfig.java` | Thêm retry fields |
| MODIFY | `ApiConfigDto.java` | Thêm retry DTO fields |
| MODIFY | `ApiMockService.java` | Thêm retry logic |
| MODIFY | `ApiConfigController.java` | Thêm retry endpoints |
| NEW | `V6__Add_failure_simulation.sql` | Database migration |
| MODIFY | `api-config-edit.html` | Thêm retry UI |
| NEW | `webhook-config.js` | Thêm retry JS logic |

---

## 📝 Business Rules
- Retry chỉ áp dụng khi `retryEnabled = true`
- `maxRetries` tối đa: 10 lần
- `retryDelayMs` tối thiểu: 100ms, tối đa: 60000ms
- Status codes mặc định trigger retry: [408, 429, 500, 502, 503, 504]
- Retry không áp dụng cho webhook đã có response thành công (2xx)

---

*Document Version: 1.0*  
*Last Updated: 2026-04-16*


---

## Cập nhật 2026-10-27 (triển khai: mô phỏng lỗi rồi thành công)

- **Vì sao đổi hướng:** sản phẩm là mock server, chỉ nhận request và trả response cấu hình sẵn, không có lời gọi HTTP ra ngoài nên không có "webhook thất bại" để retry. Theo lựa chọn của người dùng, tính năng được hiểu là để **client của người dùng thử logic retry của họ**: endpoint trả lỗi N lần đầu rồi mới trả response cấu hình. Hướng "gửi webhook ra ngoài kèm retry" (cần URL đích, HTTP client, chống SSRF) **không làm** và có thể mở plan riêng.
- **Cách hoạt động:** mỗi caller (header `Idempotency-Key`, hoặc `X-Request-Id`, hoặc IP) nhận `failureCount` (1-10) lỗi đầu, mã lỗi lấy lần lượt từ `failureStatusCodes` (400-599, mặc định `503`), rồi một response bình thường và vòng mới bắt đầu. Bộ đếm nằm trong bộ nhớ, hết hạn sau 10 phút không có call (mất khi khởi động lại app). Lỗi trả JSON `{"error":"Simulated failure","status","attempt","of"}` kèm `X-Mock-Failure-Attempt: i/N`, và `Retry-After` (giây) cho 429/503 khi `retryAfterSeconds` > 0. Độ trễ vẫn dùng `delayMs` có sẵn.
- **Đối chiếu với kế hoạch gốc:**

| Kế hoạch gốc | Thực tế | Lý do |
|---|---|---|
| `retryEnabled` | `failureEnabled` | Tên phản ánh đúng hành vi |
| `maxRetries` (1-10) | `failureCount` (1-10) | Là số lần lỗi, không phải số lần thử lại |
| `retryDelayMs` (100-60000) | `retryAfterSeconds` (0-60), gửi qua header `Retry-After` | Server mock không đợi giữa các lần thử; client mới là bên chờ, header này là cách server báo |
| `retryOnStatusCodes` (JSON array) | `failureStatusCodes` (chuỗi phân tách bằng dấu phẩy) | Mã trả về khi lỗi, không phải điều kiện kích hoạt retry; chuỗi đơn giản hơn JSON |
| `V6__Add_retry_configuration.sql` | `V6__Add_failure_simulation.sql` | Đổi tên theo trường |
| `PUT/GET /api/configs/retry/{id}`, `webhook-config.js` | Không làm | Cấu hình đi cùng form lưu API có sẵn, backup/restore, không cần endpoint riêng |
| `ApiMockService.processWithRetry/shouldRetry` | `FailureSimulationService.next(...)` | Logic tách thành service riêng, dễ test |

- **Đã làm:** entity/DTO/migration, `FailureSimulationService`, `ApiMockService.processWebhook` (quyết định lỗi một lần mỗi call, trước khi chờ delay), lưu/đọc/backup/restore, card "Failure simulation" ở trang sửa API (công tắc, số lần, mã lỗi kèm chip, Retry-After, dòng Failures ở Summary), nhãn "Fails ×N" ở danh sách API, prototype (`api-config-edit`, `api-config`). Form tạo mới trong modal không có mục này (dùng mặc định tắt); bật ở trang sửa.
- **Kiểm thử:** 43 test pass (mới: `FailureSimulationServiceTest` 9, `ApiMockFailureSimulationTest` 4, 2 test backup/restore). Chạy app thật: bật trên trang sửa, kiểm lỗi phía server (số lần > 10, mã 200), gọi `curl` 6 lần với `Idempotency-Key` cho ra 503 (Retry-After 4), 500, response cấu hình, rồi vòng mới; caller khác đếm riêng; không tràn ngang ở 390px; không lỗi JS; migration V6 chạy trên DB có sẵn dữ liệu.
- **Hạn chế đã biết:** bộ đếm mất khi khởi động lại và không chia sẻ giữa nhiều instance; nhiều client sau cùng một IP (NAT) không gửi `Idempotency-Key`/`X-Request-Id` sẽ dùng chung bộ đếm; log request của lần lỗi vẫn lưu response body cấu hình thay vì body lỗi giả lập (chỉ status đúng).
- **Việc còn lại:** commit/PR (chưa commit); Jira: [QUYIT-963](https://nhquydev.atlassian.net/browse/QUYIT-963) (Task, Sprint 35, version Tháng 10/2026, ước lượng 5h, Relates QUYIT-962), đang **In Progress**, chuyển Done sau khi merge; tùy chọn: thêm mục vào modal tạo mới, ghi header `X-Mock-Failure-Attempt` vào log, hiển thị bộ đếm hiện tại.
