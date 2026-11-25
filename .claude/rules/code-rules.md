# Code rules

## Testing

- Số điện thoại test mặc định: `0909090909`, `0765418062`. Không dùng số khác.

## Comment Rule

1. Chỉ comment khi logic phức tạp, workaround tạm, hoặc edge case đặc biệt.
2. Comment giải thích WHY, không giải thích WHAT.
3. Sửa code → sửa/xóa comment cũ theo, không để lỗi thời.
4. Code tự rõ nghĩa (CRUD đơn giản, tên biến/hàm rõ ràng) → không comment.
5. Comment ngắn: mặc định một dòng (tối đa khoảng 100 ký tự); không viết nhiều đoạn, không kể lịch sử thay đổi (đã có git).

### Comment label (per developer)

1. Read `.claude/dev.local.json` → `label`; if present, use it without asking.
2. Otherwise suggest a label from `git config user.name` / `user.email`, ask the dev to confirm or override once, then save it to `.claude/dev.local.json` (not committed).
3. Format: `<comment-syntax> [<label>] <WHY explanation>`, e.g. `// [<label>] ...`, `# [<label>] ...`, `-- [<label>] ...`.

---

## Database Execution Policy

NOT run by Claude — cần DBA review

1. Claude/agents never execute SQL directly against any database (dev, staging, production), by any means (CLI, ORM migration tool, MCP DB connector).
2. Applies to all DDL (`CREATE`/`ALTER`/`DROP`/`TRUNCATE`), DML (`INSERT`/`UPDATE`/`DELETE`) and schema migrations.
3. Write the statement into the chat session only, prefix it with `-- NOT run by Claude — cần DBA review`, then stop. A human DBA reviews and executes it.
4. Read-only `SELECT` for diagnostics/inspection is the only exemption.

---

## Git Commit Policy

1. KHÔNG tự ý `git commit` (kể cả `--amend`) — luôn hỏi và chờ dev xác nhận rõ ràng trước khi commit.
2. Khi hỏi, nêu rõ: danh sách file sẽ commit + message dự kiến.
3. Xác nhận cho 1 lần commit không áp dụng cho các lần sau — mỗi lần commit hỏi lại.
4. Không tự `git push`, `git push --force`, `git reset --hard`, tạo/xóa branch, tag nếu dev chưa yêu cầu.

---

## Development Workflow Policy

0. Yêu cầu tính năng mới, sửa bug hoặc chuẩn bị release: nếu plugin `nqdev` đã cài, đề xuất chạy `/nqdev:nqdev-task-flow <feature|bugfix|release>` trước khi phân tích hay code; người dùng từ chối thì làm theo các mục dưới.
1. Trong session trao đổi: chỉ phân tích yêu cầu mới và hỏi làm rõ — KHÔNG viết/sửa code.
2. Sau khi phân tích xong: viết plan (phạm vi, file sẽ đổi, các bước) rồi dừng lại chờ duyệt. Ưu tiên dùng skill `/nqdev-write-plan` để viết plan (nếu có); không có thì tự viết theo cấu trúc trên.
3. Chỉ bắt đầu code sau khi dev duyệt plan rõ ràng. Im lặng hoặc trả lời mơ hồ = chưa duyệt.
4. Tuyệt đối không viết code khi đang trao đổi mà chưa có sự cho phép.
5. Plan thay đổi đáng kể trong lúc làm → dừng lại, cập nhật plan, xin duyệt lại.
6. Viết code xong (từng bước hoặc toàn bộ) → cập nhật trạng thái vào plan (bước nào xong / đang làm / còn lại) rồi báo dev.

## Task Execution Policy

1. Mỗi lần chỉ làm một task trong plan đã duyệt; không tự mở rộng phạm vi.
2. Làm trên nhánh riêng; mỗi task một commit nhỏ, đúng phạm vi. Tạo nhánh và commit theo Git Commit Policy (hỏi và chờ dev xác nhận).
3. Trước khi code: nêu file sẽ sửa. Cần sửa file ngoài danh sách đó hoặc thêm thư viện mới → dừng và hỏi; chưa được duyệt thì không làm.
4. Sau mỗi task: báo những gì đã đổi và task tiếp theo trong plan (cập nhật trạng thái plan theo mục 6 của Development Workflow Policy).
5. Gặp mơ hồ hoặc xung đột với plan: dừng và hỏi, không tự quyết.
