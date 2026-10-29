---
type: feature
complexity: high
status: in_progress
related_issues: [QUYIT-989]
related_prs: []
estimated_hours: ~340-575 (43-72 person-days, gồm QA và PM; đã tính thêm UI ở mục "Thiết kế UI")
---

# Kế hoạch: MCP Server trên app hiện tại (phương án A: tự cài MCP trên Spring MVC)

> **Ngày lập kế hoạch:** 2026-10-29
> **Scope dự kiến:** gói mới `vn.autobot.webhook.mcp` (controller, dispatcher JSON-RPC, tools, token), `SecurityConfig`, `RequestLogRepository`, migration V7, trang quản lý token, Admin > Settings
> **Priority:** medium

---

## 1. Phân tích / Bối cảnh

Cho phép AI agent (client MCP) thao tác thay user trên chính app này, **không mở thêm port**, qua các path:

- `/mcp/@<username>/mcp`: Streamable HTTP (transport hiện hành của MCP)
- `/mcp/@<username>/sse`: HTTP+SSE (transport cũ, giữ để tương thích client cũ), kèm endpoint POST `message`

Việc AI được phép làm: (1) thêm mới và cập nhật API Configurations, (2) thống kê Request Logs, (3) xem Request Logs của một API có phân trang. Không gồm xóa API, backup/restore.

Hiện trạng liên quan:
- Spring Boot 2.7.9, Java 17, Spring MVC (javax). `SecurityConfig` dùng `WebSecurityConfigurerAdapter`, CSRF tắt, `/api/**` permitAll, còn lại cần đăng nhập form.
- Service dùng lại: `ApiMockService.saveApiConfig/getApiConfigs/getApiConfig`, `RequestLogService.searchRequestLogs/countRequestLogsSince`.
- `RequestLog.path` lưu cả URI (`/api/@user/orders/1`), còn `ApiConfig.path` lưu phần sau `@user` (`/orders/1`); khớp chính xác, không có path param. Log chứa header và body của người gọi (có thể có token, dữ liệu cá nhân).

Giả định (chưa được khách xác nhận):
- Xác thực bằng **Bearer token do user tự tạo**, token khớp `@username` trong path; không dùng OAuth.
- Token đọc-ghi (chưa phân quyền theo từng token).
- Chỉ dùng **tools** của MCP (không resources, prompts, sampling).
- 1 senior dev, đã quen codebase.

## 2. Approach / Strategy

**Phương án chọn: A. Tự cài giao thức MCP (JSON-RPC 2.0) trên Spring MVC, giữ Spring Boot 2.7.**

Vì sao không chọn B (MCP Java SDK chính thức): transport servlet của SDK dùng Jakarta Servlet, `mcp-spring-webmvc` nhiều khả năng nhắm Spring 6, trong khi app đang dùng `javax`. Dùng SDK nhiều khả năng phải nâng Spring Boot 3 (javax → jakarta, viết lại `SecurityConfig`, Hibernate 6...), là một dự án riêng. Điều này **chưa được xác nhận từ tài liệu**, nên có một bước spike ngắn (mục 3, bước 0) trước khi cam kết. Phương án C (tiến trình trung gian) loại vì trái yêu cầu không mở thêm port.

Thiết kế giao thức (theo spec MCP 2025-06-18):
- Một endpoint `POST /mcp/@{username}/mcp` nhận một JSON-RPC message mỗi request. Request thì trả `application/json` (không cần SSE); notification/response thì trả `202` không body.
- `GET` trên endpoint này trả `405` (không cung cấp luồng SSE ở đây). `DELETE` trả `405` (stateless, không dùng `Mcp-Session-Id`).
- Phương thức hỗ trợ: `initialize` (thỏa thuận phiên bản giao thức, trả capability `tools`), `notifications/initialized`, `ping`, `tools/list`, `tools/call`. Phương thức lạ trả lỗi JSON-RPC `-32601`.
- Header `MCP-Protocol-Version` không hợp lệ trả `400`; thiếu header thì coi là `2025-03-26`.
- Kiểm tra header `Origin` (bắt buộc theo spec): cho phép khi không có `Origin` (client không phải trình duyệt) hoặc khi nằm trong danh sách cho phép (Public base URL trong Settings).
- Transport cũ: `GET /mcp/@{username}/sse` mở `SseEmitter`, gửi event `endpoint` trỏ tới `POST /mcp/@{username}/message?sessionId=...`; phản hồi gửi lại qua luồng SSE; registry session trong bộ nhớ, heartbeat, dọn kết nối, giới hạn số kết nối.

Bộ tool (mô tả đủ để AI dùng đúng, JSON schema đầu vào chặt):

| Tool | Việc làm |
|---|---|
| `list_api_configs` | Danh sách API của user, phân trang |
| `get_api_config` | Chi tiết một API |
| `create_api_config` | Thêm mới; dùng lại validation của `ApiConfigDto` |
| `update_api_config` | Cập nhật theo `id`; có kiểm tra phiên bản (`updatedAt`) để không ghi đè thay đổi mới hơn |
| `get_request_log_stats` | Thống kê theo khoảng thời gian: tổng, theo method, theo nhóm status, theo ngày, top path |
| `list_request_logs` | Log của một API (theo path+method), phân trang, lọc status; body dài bị rút gọn; che `Authorization`, `Cookie` |

Lỗi nghiệp vụ (validation, không tìm thấy, không có quyền) trả trong kết quả tool với `isError: true` để AI đọc và tự sửa; chỉ lỗi giao thức mới dùng lỗi JSON-RPC.

Bảo mật:
- Bảng `mcp_tokens` (migration V7): lưu **băm** token, tên gọi, user, thời điểm tạo/dùng gần nhất, trạng thái thu hồi. Token chỉ hiện đầy đủ một lần khi tạo.
- Filter xác thực riêng cho `/mcp/**` (permitAll ở Spring Security, tự xác thực bằng token): sai/thiếu token trả `401` kèm `WWW-Authenticate: Bearer`; token không thuộc `@username` trong path trả `403`.
- Công tắc bật/tắt MCP toàn cục trong Admin > Settings (mặc định tắt); giới hạn tốc độ và kích thước request; token ghi nhật ký lần dùng.
- Mọi tool chỉ thao tác dữ liệu của user sở hữu token.

Đánh đổi đã cân nhắc:
- Stateless (không `Mcp-Session-Id`) cho transport mới: đơn giản, chạy được sau proxy và nhiều instance; mất khả năng server chủ động gửi tin cho client (chưa cần).
- Transport cũ cần registry session trong bộ nhớ: không chia sẻ giữa nhiều instance và mất khi khởi động lại; chấp nhận vì chỉ để tương thích.
- Không làm OAuth: client chỉ hỗ trợ OAuth sẽ không kết nối được (rủi ro 1).

## 3. Công việc cần thực hiện (Todo)

Phase 0: Spike (0,5–1 ngày, chưa tính vào estimate)
- [~] Xác nhận SDK chính thức có chạy được trên Spring Boot 2.7 hay không (artifact, namespace servlet, Jackson); ghi kết quả vào plan. Nếu có và gọn hơn thì dừng và lập lại plan.
- [~] Gọi thử endpoint tối giản bằng MCP Inspector hoặc Claude Code để chốt `Accept`, `MCP-Protocol-Version`, hình dạng phản hồi `initialize`.

Phase 1: MVP (base 15–23 ngày, sau hệ số 19–32)
- [x] M1 – Lõi MCP + Streamable HTTP (4–6): dispatcher JSON-RPC, `initialize`/`ping`/`tools/list`/`tools/call`, mã lỗi, `202`, `405`, `Origin`, thỏa thuận phiên bản
- [x] M3 – Token Bearer (4–6): migration V7, băm token, trang tạo/thu hồi (có ví dụ cấu hình client), filter xác thực, kiểm tra `@username`
- [x] M4 – Tool cấu hình API (4–5): `list_api_configs`, `get_api_config`, `create_api_config`, `update_api_config`, JSON schema, ánh xạ lỗi, kiểm tra `updatedAt`
- [x] M6 – Tool xem log một API (2–4): `list_request_logs`, phân trang, lọc, rút gọn, che header nhạy cảm
- [x] M7 – Card "MCP Server" trong Admin > Settings: bật/tắt, giới hạn tốc độ, page size tối đa, origin cho phép thêm, số liệu (2–3; thiết kế ở mục "Thiết kế UI")

Phase 2 (base 7–10, sau hệ số 9–14)
- [x] M5 – Tool thống kê (3–4): truy vấn gom nhóm mới trong `RequestLogRepository`, giới hạn khoảng thời gian, chỉ mục nếu cần
- [x] M2 – Transport cũ SSE (4–6): `/sse` + `message`, registry session, heartbeat, dọn kết nối, giới hạn kết nối

Phase 3 (base 2–3, sau hệ số 3–4)
- [x] M8 – Nhật ký thao tác MCP kèm trang MCP Activity (bảng, lọc, phân trang, panel chi tiết) (4–6; thiết kế ở mục "Thiết kế UI")

Xuyên suốt
- [x] Test đơn vị cho dispatcher, từng tool, filter token; test tích hợp bằng `MockMvc`; thử với ít nhất hai client MCP thật
- [~] Cập nhật plan, Jira, tài liệu ngắn "Kết nối MCP" trong trang token

## 4. Risks & Unknowns

- **Risk 1 (Cao): xác thực và tương thích client.** Client chỉ hỗ trợ OAuth không dùng được token tĩnh. → **Mitigation:** hỏi khách client nào sẽ dùng trước phase 1; OAuth không có baseline, nếu cần thì lập estimate riêng.
- **Risk 2 (Cao): lộ dữ liệu qua log.** Header/body trong log có thể chứa token hoặc dữ liệu cá nhân. → **Mitigation:** che header nhạy cảm, rút gọn body, giới hạn theo user; khách xác nhận phạm vi dữ liệu.
- **Risk 3 (Cao): SDK chính thức khác Spring Boot 2.7.** → **Mitigation:** spike phase 0.
- **Risk 4 (Trung bình–Cao): AI ghi đè cấu hình.** → **Mitigation:** kiểm tra `updatedAt`, không có tool xóa, nhật ký ở phase 3.
- **Risk 5 (Trung bình): SSE sau proxy** (nginx, Cloudflare, localtunnel dễ buffer/cắt kết nối). → **Mitigation:** heartbeat, hướng dẫn cấu hình proxy; cấu hình hạ tầng nằm ngoài scope.
- **Risk 6 (Trung bình): spec đổi.** Một số nguồn thứ cấp nhắc bản sửa đổi mới hơn với thay đổi ở Streamable HTTP, chưa xác nhận từ nguồn chính thức. → **Mitigation:** thỏa thuận phiên bản linh hoạt, kiểm tra lại spec trước khi code.
- **Risk 7 (Trung bình): liên kết log với API theo đường dẫn.** Path có tham số hoặc đã đổi path sẽ không khớp. → **Mitigation:** ghi rõ giới hạn trong mô tả tool.
- **Risk 8 (Thấp–Trung bình): hiệu năng thống kê** trên bảng log lớn (đã thử 80 nghìn dòng). → **Mitigation:** bắt buộc khoảng thời gian, chỉ mục, giới hạn số dòng.
- **Unknown:** client MCP nào sẽ dùng và gắn được header `Authorization` tĩnh không → hỏi khách.

## 5. Success Criteria

- Một client MCP thật (Claude Code hoặc Cursor) kết nối được tới `/mcp/@<username>/mcp` bằng token, thấy 4 tool phase 1, tạo và cập nhật được một API, và xem log phân trang của API đó.
- Sai/thiếu/thu hồi token trả `401`; token của user khác trả `403`; MCP tắt trong Settings thì mọi request trả lỗi rõ ràng.
- Header nhạy cảm trong log không xuất hiện trong kết quả tool.
- Không mở thêm port; không đổi hành vi `/api/**`, `/admin/**` hiện có.
- Toàn bộ test (cũ và mới) pass; phase 2 thêm thống kê và `/sse`.

## 6. Questions / Dependencies (Tùy chọn)

1. Client nào sẽ dùng và có gắn được header `Authorization` tĩnh không, hay bắt buộc OAuth? (đổi estimate nhiều nhất)
2. Token đọc-ghi hay tách quyền theo từng token?
3. Dữ liệu log đưa cho AI: có che mọi header/body nhạy cảm, giới hạn kích thước và số ngày?
4. Có cần thêm tool xóa API, backup/restore, hoặc xem log toàn user không?
5. Chấp nhận nâng Spring Boot 3 nếu spike cho thấy SDK chính thức là đường ngắn hơn?

Estimate (person-days, 1 senior dev): phase 1 là 24–41 (6–10 tuần), phase 2 là 11–18, phase 3 là 4–5; tổng 38–65, timeline 9,5–16 tuần cho 1 dev (khoảng 5–8 tuần cho 2 dev).

Quy ước dự án: nhánh `feature/*` mới tách từ `coding` và pull trước khi viết code; PR vào `coding`.

---

## Cập nhật 2026-10-29 (quyết định xác thực và đợt đầu: card dashboard + token + endpoint tối thiểu)

- **Quyết định của người dùng:** chọn phương án A; xác thực bằng `Authorization: Bearer <token>`; **một token mỗi user**, renew thì token cũ hết hạn ngay; card MCP đặt ở dashboard, phía trên Quick Start Guide, gồm link MCP của user và token (có nút Renew).
- **Token:** dạng `mcp_` + 32 byte ngẫu nhiên (base64url), chỉ lưu **băm SHA-256** (bảng `mcp_tokens`, migration **V7**, xóa user thì xóa token theo). Vì chỉ lưu băm nên token đầy đủ chỉ hiện **một lần** ngay sau Generate/Renew (kèm đoạn cấu hình client sẵn để dán); các lần sau card chỉ hiện `mcp_••••••••abcd`, ngày tạo và lần dùng gần nhất (ghi tối đa mỗi phút một lần). Phương án lưu mã hóa để luôn xem lại được đã được đề xuất và **không chọn** (rủi ro cao hơn khi lộ DB hoặc backup).
- **Đã làm (nhánh `feature/mcp-server`, tách từ `coding` sau khi PR #47 merge):**
  - [x] M3 một phần: `McpToken`, `McpTokenRepository`, `McpTokenService` (renew, authenticate), `POST /mcp-token/renew`, card trên dashboard (prototype rồi template thật), hộp xác nhận Generate/Renew.
  - [x] M1 bản tối thiểu: `McpController` `POST /mcp/@{username}/mcp`, `McpService` (JSON-RPC: `initialize`, `ping`, `tools/list`, `tools/call`; thông báo trả 202; GET/DELETE trả 405), kiểm tra `Origin` (cùng host hoặc Public base URL) và `MCP-Protocol-Version`, token phải khớp `@username` (401 kèm `WWW-Authenticate`, 403), `/mcp/**` mở ở Spring Security vì tự xác thực bằng token.
  - [x] Một tool đọc: `list_api_configs` (phân trang, tối đa 50/trang, sắp xếp id giảm dần, có `url` đầy đủ).
- **Kiểm thử:** 68 test pass (mới 25: `McpTokenServiceTest`, `McpServiceTest`, `McpControllerTest`). Chạy app thật: card hiện đúng chỗ, Generate/Renew, token hiện một lần rồi chỉ còn dạng che, renew làm token cũ thành 401; `curl` đủ các tình huống (không token 401, token cũ 401, token user khác 403, `initialize`, `notifications/initialized` 202, `tools/list`, `tools/call`, GET 405, Origin lạ 403); không tràn ngang ở 390px; không lỗi JS; migration V7 chạy trên DB có dữ liệu.
- **Chỉ hiện URL `/mcp`:** link `/sse` chưa có trong card vì transport SSE (M2) chưa làm, tránh trỏ tới link chết.
- **Chưa làm (còn trong plan):** M4 tool thêm/sửa API; M6 tool xem log một API; M5 thống kê; M2 transport SSE; M7 công tắc bật/tắt MCP trong Settings (hiện MCP luôn bật, mặc định có token mới dùng được); M8 nhật ký thao tác; thử với client MCP thật (MCP Inspector, Claude Code); spike SDK chính thức (không còn cần nếu giữ phương án A).
- **Hạn chế đã biết:** chưa giới hạn tốc độ và kích thước request; chưa che header nhạy cảm vì chưa có tool đọc log; token chưa có quyền tách đọc/ghi (hiện chỉ có tool đọc).
- **Chưa commit.** Việc cần người dùng: chạy thử với client MCP thật.

---

## Thiết kế UI (2026-10-29)

Thiết kế trong prototype `docs/prototype/` (chưa áp dụng vào template thật), theme Bootstrap như các màn khác. Ba màn, đều bấm thử được:

### S1. Dashboard: card "MCP Server" (`dashboard`)
Đã có bản thật (đợt đầu); thiết kế mới sửa lại như sau, **chưa áp dụng vào template thật**:
- Tiêu đề card có liên kết **Activity** tới S3.
- Trạng thái: chưa có token (nút Generate Token) → đã có token (`mcp_••••••••abcd`, ngày tạo, lần dùng gần nhất, nút Renew Token) → vừa tạo/renew (token đầy đủ, cảnh báo chỉ hiện một lần) → **MCP bị quản trị viên tắt** (khung thông báo xám phía trên; nút vẫn dùng được để chuẩn bị token).
- Phần mới **Connect a client** (thu gọn mặc định, tự mở khi vừa tạo token) có ba tab: Claude Code (`claude mcp add --transport http …`), Cursor (JSON `mcpServers`), cURL (gọi `tools/list`). Ví dụ dùng token thật khi vừa tạo, còn lại dùng `<your-token>`. Phần này **thay** khối `<pre>` cấu hình đang nằm ngay dưới token trong template thật.
- Cú pháp lệnh/JSON của từng client lấy theo hiểu biết hiện có, **cần kiểm tra lại với tài liệu của client trước khi áp dụng** (vì client đổi nhanh).

### S2. Admin > Settings: card "MCP Server" (`admin-settings`)
Nằm giữa General và Database maintenance. Gồm: huy hiệu Enabled/Disabled; công tắc **Enable MCP server** (tắt thì mọi request MCP bị từ chối); **Rate limit** (1–600 request/phút/token, mặc định 60); **Max page size** (1–200, mặc định 50, thay hằng số 50 trong `McpService`); **Extra allowed origins** (mỗi dòng một origin, chỉ cần cho client chạy trong trình duyệt; host của app và Public base URL luôn được phép); số liệu **Tokens issued** và **Calls in the last 24 hours** (kèm số lỗi); nút **Save MCP Settings** riêng, kiểm tra giá trị ngay khi nhập.

### S3. MCP Activity (`mcp-activity`, màn mới)
Từ card dashboard và breadcrumb Dashboard > MCP Activity. Ba ô thống kê (Calls today, Errors today, Changes made today); thanh công cụ một hàng (tìm trong tham số/client, lọc tool, lọc kết quả OK/Error); bảng Time (UTC), Tool (tool ghi có nhãn **write**), Arguments (cắt gọn), Client, Duration, Result; phân trang kèm chọn số dòng; trạng thái rỗng; bấm hàng mở panel bên phải với thời gian, client, token (đã che), thời lượng, request và response dạng JSON. Chỉ hiện dữ liệu của chính user; quản trị viên chưa có chế độ xem tất cả (để ngỏ).

### Nhu cầu dữ liệu và backend kéo theo
- **M7:** thêm khóa `mcp.enabled`, `mcp.rateLimitPerMinute`, `mcp.maxPageSize`, `mcp.extraOrigins` vào `SettingsService` (mặc định bật, 60, 50, trống); `McpController` kiểm tra `enabled` và giới hạn tốc độ theo token; `Origin` đọc thêm danh sách origin bổ sung; card dashboard cần biết MCP đang bật hay tắt.
- **M8:** bảng `mcp_calls` (migration mới sau V7): user, token, tool, tham số (cắt gọn), kết quả OK/Error, thời lượng, tên client (lấy từ `clientInfo` của `initialize` hoặc User-Agent), thời điểm, trích đoạn phản hồi; chỉ mục theo (user, thời điểm); ghi trong `tools/call` của `McpService`; trang `/mcp-activity` có phân trang, lọc theo tool/kết quả/từ khóa, thống kê trong ngày. Tham số và phản hồi lưu rút gọn và **che các trường nhạy cảm** (không ghi token hay header).
- Con số "Tokens issued" và "Calls in the last 24 hours" ở S2 lấy từ `mcp_tokens` và `mcp_calls`.

### Ảnh hưởng đến estimate
Thiết kế UI làm M7 tăng từ 1–2 lên 2–3 ngày (card có bốn trường, kiểm tra, số liệu) và M8 từ bản tối giản 2–3 lên **4–6** ngày (đúng dòng "audit log + history" của baseline, gồm cả trang xem). Tính lại theo công thức (hệ số security +15–20% và E2E +10–15% như trước):

| Phase | Base | Dev sau hệ số | Gồm QA 15–20% và PM 10% |
|---|---|---|---|
| 1 – MVP (M1, M3, M4, M6, M7) | 16–24 | 20–33 | 25–43 |
| 2 (M5, M2) | 7–10 | 9–14 | 11–18 |
| 3 (M8 kèm trang Activity) | 4–6 | 5–8 | 6–11 |
| **Tổng** | 27–40 | 34–55 | **43–72 person-days** |

Timeline: 1 dev khoảng 11–18 tuần, 2 dev khoảng 5,5–9 tuần; riêng phase 1 với 1 dev khoảng 6–11 tuần. Có thể kéo M7 vào phase 1 và hoãn M8 sang phase 3 như trên; nếu cần bản nhỏ hơn thì dùng nhật ký tối giản không có trang xem (M8 2–3 ngày).

### Việc UI còn lại
- [x] Áp dụng S1 vào `dashboard.html` thật (liên kết Activity, Connect a client, trạng thái MCP tắt); cần M7 để biết trạng thái bật/tắt
- [x] Áp dụng S2 vào `admin/settings.html` thật cùng backend M7
- [x] Áp dụng S3 thành template `mcp-activity.html` thật cùng backend M8
- [ ] Kiểm tra cú pháp lệnh/JSON từng client trong tab Connect với tài liệu hiện hành
- [ ] Cập nhật `docs/specs/ui-ux.md` nếu thêm quy ước mới (huy hiệu `write`, khối mã trong card)

---

## Cập nhật 2026-10-29 (thực thi toàn bộ plan: phase 1, 2 và 3)

- **Đã làm (nhánh `feature/mcp-server`, chưa commit):**
  - **M7 Settings:** card "MCP Server" trong Admin > Settings (`McpSettingsDto`, `POST /admin/settings/mcp`): bật/tắt (tắt thì mọi request MCP trả 503 và card dashboard hiện "MCP is turned off by an administrator"), giới hạn tốc độ theo token (mặc định 60/phút, trả 429 kèm `Retry-After`; `McpRateLimiter` cửa sổ cố định theo phút, trong bộ nhớ), page size tối đa (mặc định 50, tool áp dụng), origin bổ sung (kiểm tra từng dòng dạng `http(s)://host`), số liệu token đã cấp và số lần gọi 24 giờ.
  - **M4 tool cấu hình:** `get_api_config`, `create_api_config`, `update_api_config`. Tạo/sửa dùng lại `ApiMockService.saveApiConfig` và validation của `ApiConfigDto`, thêm kiểm tra method hợp lệ, path bắt đầu bằng `/`, `responseHeaders` là JSON object, không trùng method+path (báo `id` đang giữ), tên trường lạ và sai kiểu đều được giải thích để AI tự sửa; mọi lỗi trả dạng tool error (`isError`) liệt kê đủ các vấn đề. `update_api_config` nhận `expectedUpdatedAt` (lấy từ `get_api_config`) và từ chối nếu endpoint đã đổi từ lần đọc; `null` xóa description/note. `updatedAt` được làm tròn về micro-giây vì đó là độ chính xác DB lưu (phát hiện khi chạy thật: giá trị nano-giây trả về lúc tạo không khớp giá trị đọc lại).
  - **M6, M5 tool log:** `list_request_logs` (theo `apiId` hoặc `path`+`method`, lọc `statusClass`, phân trang; khớp **chính xác** đường dẫn nên log của `/orders/1` không lẫn vào `/orders`; header nhạy cảm bị che, body cắt ở 2000 ký tự) và `get_request_log_stats` (mặc định 7 ngày, tối đa 90; tổng, theo method, nhóm status, ngày, 10 path nhiều nhất, số lỗi; có thể giới hạn một API). Truy vấn gom nhóm theo ngày dùng `year/month/day` thay vì giá trị date vì giá trị date bị lệch theo múi giờ JVM (phát hiện bằng test trên H2 thật).
  - **M2 SSE:** `GET /mcp/@u/sse` (gửi event `endpoint`) và `POST /mcp/@u/message?sessionId=` (trả 202, câu trả lời đi qua luồng SSE); `McpSessionRegistry` trong bộ nhớ, tối đa 5 luồng mỗi user và 200 tổng (429 kèm `Retry-After: 30`), heartbeat 25 giây, session của user khác trả 404.
  - **M8 nhật ký:** bảng `mcp_calls` (migration **V8**), ghi mọi lần gọi tool (tên, tham số đã che và cắt 2000 ký tự, kết quả OK/Error, trích đoạn phản hồi, thời lượng, client lấy từ User-Agent, 4 ký tự cuối token); lỗi ghi log không làm hỏng tool; giữ 90 ngày, dọn trong lần bảo trì database; trang `/mcp-activity` (thống kê trong ngày, lọc tool/kết quả/từ khóa phía server, phân trang 10/25, panel chi tiết); mục Dashboard trên thanh điều hướng vẫn sáng ở trang này.
  - **UI:** S1 dashboard (liên kết Activity, phần "Connect a client" với tab Claude Code/Cursor/cURL/SSE, trạng thái MCP tắt; thay khối `<pre>` cũ), S2, S3 đã áp dụng vào template thật; prototype có thêm tab SSE.
  - **Che dữ liệu nhạy cảm:** `McpRedactor` thay giá trị của khóa chứa authorization, cookie, token, password, secret, api-key bằng `***` ở mọi cấp (dùng cho log trả về AI và nhật ký thao tác).
- **Kiểm thử:** 112 test pass (mới 69 so với trước đợt này, gồm `McpToolsTest`, `McpSupportTest`, `McpCallServiceTest`, `McpQueriesTest` chạy JPQL trên H2 trong bộ nhớ, mở rộng `McpControllerTest` với SSE/rate limit/origin/tắt MCP, `AdminSettingsControllerTest`, `SettingsServiceTest`). Chạy app thật: card và token; `curl` đủ sáu tool (tạo, trùng, sai dữ liệu, sửa, sửa với `updatedAt` cũ, mock endpoint vừa tạo trả đúng và mô phỏng lỗi đúng, log, thống kê); SSE bằng `curl` (nhận event `endpoint`, ba POST đều 202, hai câu trả lời đến trên luồng, session lạ 404, token sai 401); Settings (lỗi kiểm tra, lưu, giới hạn 3/phút trả 200,200,200,429, tắt MCP trả 503 và dashboard báo tắt, bật lại); trang Activity (lọc, phân trang, panel chi tiết); không tràn ngang ở 390px; không lỗi JS; migration V7 và V8 chạy trên DB có dữ liệu.
- **Hạn chế đã biết:** bộ đếm tốc độ và session SSE nằm trong bộ nhớ (mất khi khởi động lại, không chia sẻ giữa nhiều instance); token chưa tách quyền đọc/ghi (một token làm được mọi tool); log của endpoint chỉ khớp theo đường dẫn chính xác (path có tham số hoặc đã đổi path không gắn đúng); nhật ký chỉ lưu thông tin rút gọn nên panel chi tiết có thể hiện JSON bị cắt; trang Activity chỉ hiện dữ liệu của chính user (quản trị viên chưa xem được của người khác); client dùng OAuth không kết nối được vì chỉ có token Bearer.
- **Chưa làm / việc còn lại:**
  - [ ] Thử với client MCP thật (MCP Inspector, Claude Code, Cursor); chỉ mới thử bằng `curl` và test tự động
  - [ ] Kiểm tra cú pháp lệnh/JSON trong tab Connect với tài liệu hiện hành của từng client
  - [ ] Spike SDK chính thức: không còn cần vì đã chọn và hoàn thành phương án A
  - [ ] Commit, push và mở PR vào `coding`
  - [ ] Tùy chọn: quyền đọc/ghi theo token, quản trị viên xem activity của mọi user, OAuth nếu client yêu cầu

## Cập nhật 2026-10-29 (Jira)

- Jira: [QUYIT-989](https://nhquydev.atlassian.net/browse/QUYIT-989) (Task, Sprint 35, version Tháng 10/2026, ước lượng gốc 344h, tức cận dưới 43 person-days của plan; Relates QUYIT-963), trạng thái **In Progress**; đã comment tiến độ và việc còn lại. Chưa chuyển Done vì code chưa commit/PR và chưa thử với client MCP thật; chuyển Done khi người dùng yêu cầu hoặc sau khi PR được merge.
- Trạng thái plan đổi về `in_progress` cho khớp: phần triển khai xong nhưng còn các việc cần người ở mục trước.
