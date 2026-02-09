# HC_DAT_APP_DEVICE
# Clean Project:
chmod 777 gradlew
./gradlew clean && ./gradlew cleanBuildCache
rm -rf ~/.gradle/caches/
        
# Build Wifi
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
adb tcpip 5555
adb connect 192.168.1.80:5555
./gradlew clean && ./gradlew cleanBuildCache