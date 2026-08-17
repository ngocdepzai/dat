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

export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Java: $(java -version 2>&1 | head -1)"
echo "Bắt đầu build release..."
echo

./gradlew clean assembleRelease

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