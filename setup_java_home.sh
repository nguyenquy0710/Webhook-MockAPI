#!/bin/bash

# Tìm tất cả thư mục bắt đầu bằng 17.x.x trong /home/codespace/java/
JAVA_DIR="/home/codespace/java"
JAVA_17_DIRS=($(ls -d $JAVA_DIR/17.* 2>/dev/null))

if [ ${#JAVA_17_DIRS[@]} -eq 0 ]; then
  echo "Không tìm thấy phiên bản Java 17 nào trong $JAVA_DIR"
  exit 1
fi

# Sắp xếp theo phiên bản giảm dần để lấy bản mới nhất
LATEST_JAVA_17=$(printf "%s\n" "${JAVA_17_DIRS[@]}" | sort -Vr | head -n 1)

# Thiết lập JAVA_HOME và cập nhật PATH
export JAVA_HOME="$LATEST_JAVA_17"
export PATH="$JAVA_HOME/bin:$PATH"

chmod +x "./mvnw"
chmod +x "$JAVA_HOME/bin/java"
# Lưu thiết lập vào file .bashrc để tự động áp dụng khi mở terminal mới
echo "export JAVA_HOME=\"$JAVA_HOME\"" >> ~/.bashrc
echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\"" >> ~/.bashrc
# Nạp lại file .bashrc để áp dụng thiết lập ngay lập tức
source ~/.bashrc

# In ra để xác nhận
echo "Đã thiết lập JAVA_HOME: $JAVA_HOME"
echo "PATH hiện tại: $PATH"
