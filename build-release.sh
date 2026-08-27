#!/usr/bin/env bash
set -euo pipefail

# Build release APK cho hc.manager.datapp
# Yêu cầu: openjdk@17 (Java 21 không tương thích với kapt Kotlin 1.5.31)
#   brew install openjdk@17

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JAVA17_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
if [ ! -x "$JAVA17_HOME/bin/java" ]; then
  echo "Không tìm thấy openjdk@17 tại $JAVA17_HOME"
  echo "Cài đặt: brew install openjdk@17"
  exit 1
fi

# AGP 7.3.1 (ide.common.resources.NodeUtils) crash khi merge resources trên openjdk@17
# bản mới (17.0.20): "Cannot invoke org.w3c.dom.Node.getLocalName() because node is null".
# JDK 21 của Android Studio (JBR) không dính lỗi này, nên merge resources chạy bằng JBR 21
# trước, rồi assembleRelease chạy bằng JDK 17 (kapt Kotlin 1.5.31 cần JDK 17)
# và dùng lại kết quả merge đã up-to-date.
JBR_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

stop_daemons() {
  # Kotlin daemon giữ nguyên JVM đã khởi động; nếu còn daemon của JDK 21 thì kapt sẽ
  # "Internal compiler error" ở lần chạy JDK 17 kế tiếp.
  ./gradlew --stop >/dev/null 2>&1 || true
  pkill -f KotlinCompileDaemon >/dev/null 2>&1 || true
}

run_with_jdk() {
  local jdk_home="$1"
  shift
  JAVA_HOME="$jdk_home" PATH="$jdk_home/bin:$PATH" ./gradlew "$@"
}

export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Java: $(java -version 2>&1 | head -1)"
echo "Bắt đầu build release..."
echo

run_with_jdk "$JAVA17_HOME" clean

if [ -x "$JBR_HOME/bin/java" ]; then
  echo "Merge resources bằng JBR: $("$JBR_HOME/bin/java" -version 2>&1 | head -1)"
  stop_daemons
  run_with_jdk "$JBR_HOME" mergeReleaseResources
  stop_daemons
else
  echo "Không tìm thấy JBR tại $JBR_HOME, thử merge resources bằng JDK 17"
fi

run_with_jdk "$JAVA17_HOME" assembleRelease

APK_PATH="app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK_PATH" ]; then
  echo "Build không sinh ra APK tại $APK_PATH"
  exit 1
fi

echo
echo "Build thành công"
echo "APK: $SCRIPT_DIR/$APK_PATH"
ls -lh "$APK_PATH"

AAPT2="$(find "${ANDROID_HOME:-$HOME/Library/Android/sdk}/build-tools" -name aapt2 2>/dev/null | sort -V | tail -1)"
if [ -x "$AAPT2" ]; then
  echo
  "$AAPT2" dump badging "$APK_PATH" | grep -E "package|versionCode|versionName" | head -1
fi
