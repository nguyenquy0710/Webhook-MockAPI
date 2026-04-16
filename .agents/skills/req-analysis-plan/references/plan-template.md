# Plan: [Task/Feature Name - e.g., Tối ưu hóa FFmpeg restream]

[Mô tả ngắn gọn về yêu cầu và ngữ cảnh cụ thể]

## 1. Objectives
- [Mục tiêu chính - Ví dụ: Tăng độ ổn định của luồng restream]
- [Ràng buộc kỹ thuật - Ví dụ: Giữ mức CPU < 10%]

## 2. Streaming & FFmpeg Configuration
### 2.1. FFmpeg Parameters
- **Source**: [TikTok HLS/M3U8]
- **Target**: [YouTube RTMP]
- **Video Codec**: [`-c:v copy` hoặc `libx264`]
- **Audio Codec**: [`-c:a aac`]
- **Bitrate/Settings**: [Ví dụ: `-b:v 3000k -maxrate 3000k -bufsize 6000k`]

### 2.2. Protocol handling
- [Cấu hình reconnect hoặc timeout: `-reconnect 1 -rw_timeout 15000000`]

## 3. Python Implementation (src/)
### 3.1. Core Script Changes
- [File/Function]: [Mô tả chi tiết thay đổi logic]
- [Exception handling]: [Cách xử lý lỗi kết nối hoặc FFmpeg exit code]

### 3.2. Configuration & Env
- [Biến môi trường mới trong `.env.example`]

## 4. Implementation Steps
1. **Phase 1: Foundation & Utils**
   - [Nhiệm vụ 1.1]
2. **Phase 2: Core Streaming Logic**
   - [Nhiệm vụ 2.1]
3. **Phase 3: Reconnection & Monitoring**
   - [Nhiệm vụ 3.1]
4. **Phase 4: Testing & Validation**
   - [Nhiệm vụ 4.1]

## 5. Verification Plan
- **[Test Case 1 - CLI Verification]**: Chạy `python src/01_...py --version` hoặc kiểm tra tham số CLI.
- **[Test Case 2 - Stream Stability]**: Kiểm tra log FFmpeg sau 1 giờ chạy liên tục.
- **[Test Case 3 - Reconnection]**: Ngắt kết nối mạng giả lập để kiểm tra logic reconnect.
