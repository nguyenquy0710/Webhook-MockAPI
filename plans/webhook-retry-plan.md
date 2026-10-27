# 📋 Kế Hoạch: Webhook Retry Configuration

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

**Migration**: `src/main/resources/db/migration/V4__Add_retry_configuration.sql`

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
| NEW | `V4__Add_retry_configuration.sql` | Database migration |
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
