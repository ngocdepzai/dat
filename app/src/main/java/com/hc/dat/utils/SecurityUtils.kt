import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.Location
import android.provider.Settings
import io.sentry.Sentry
import io.sentry.SentryLevel

object SecurityUtils {

    fun checkNonWhitelistApps(context: Context): List<ApplicationInfo> {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val unauthorizedApps = mutableListOf<ApplicationInfo>()

        for (app in installedApps) {
            if (app.packageName == context.packageName) continue

            // Loại bỏ các app hệ thống và các app update của hệ thống
            val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) ||
                    (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)

            // Nếu KHÔNG PHẢI app hệ thống THÌ MỚI KIỂM TRA
            if (!isSystemApp) {
                // Chỉ thêm vào danh sách đen nếu nó KHÔNG nằm trong whitelist an toàn
                if (!isWhiteListed(app.packageName)) {
                    unauthorizedApps.add(app)
                }
            }
        }
        return unauthorizedApps
    }

    // Danh sách package các app fake GPS phổ biến
    private val FAKE_GPS_PACKAGES = listOf(
            // Nhóm phổ biến nhất
            "com.lexa.fakegps",
            "org.itstools.fakegps",
            "com.blogspot.newapphorizons.fakegps",
            "com.kvassyu.fake.gps",
            "com.gsmartstudio.fakegps",
            "com.incorporateapps.fakegps.fre",
            "com.incorporateapps.fakegps.vipro", // Bản trả phí
            "com.fakegps.jolewu",

            // Nhóm App Ninjas (Rất mạnh, có tính năng che giấu)
            "com.theappninjas.fakegpsjoystick",
            "com.theappninjas.gpsjoystick",

            // Nhóm Fly GPS & Fake Location chuyên sâu
            "com.fly.gps",
            "com.guoshi.location", // Fake Location (Rất phổ biến với dân lách luật)
            "com.lkr.fakegps",
            "com.mocker.fakegps",
            "fake.gps.location",
            "com.clonemy.device", // Các app nhân bản để fake
            "com.location.spoof",
            "com.fake.gps.location",
            "com.fakegps.location",

            // Nhóm dùng cho Grab/Uber cũ vẫn còn dùng được
            "com.hola.gpslocation",
            "com.kingwaytec.naviking"
    )

    private fun isWhiteListed(pkgName: String): Boolean {
        val safeApps = listOf(
                "com.google.android.inputmethod.latin", // Bàn phím Gboard
                "com.vinput.labankey",                  // Bàn phím Laban Key
                "com.android.chrome",                   // Trình duyệt Chrome
                "com.google.android.apps.maps",         // Google Maps
                "com.zing.zalo",                        // Zalo
                "com.facebook.orca",                    // Facebook Messenger (Bản chuẩn)
                "com.facebook.mlite",                    // Facebook Messenger Lite (Dành cho máy cấu hình yếu)
                "com.wakdev.nfctools.pro",
                "com.wakdev.wdnfc",
        )
        return safeApps.contains(pkgName)
    }

    // Thêm hàm này nếu chưa có để lấy package đang chiếm quyền Mock Location
    fun getActiveMockApp(context: Context): String? {
        return try {
            Settings.Secure.getString(context.contentResolver, "mock_location_app")
        } catch (e: Exception) {
            null
        }
    }

    // 1. Kiểm tra xem có app Fake GPS nào đang cài trên máy không
    fun getInstalledFakeGpsApp(context: Context): String? {
        val pm = context.packageManager
        for (pkg in FAKE_GPS_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg // Trả về tên package phát hiện được
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    // 2. Kiểm tra xem "Mock Location App" có đang được chọn trong Developer Options không
    fun isMockLocationAppSet(context: Context): Boolean {
        return try {
            val mockApp = Settings.Secure.getString(context.contentResolver, "mock_location_app")
            !mockApp.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // 3. Kiểm tra vị trí từ Location object có phải là giả không
    fun isLocationMock(location: Location): Boolean {
        return location.isFromMockProvider
    }

    // 4. Log vi phạm lên Sentry
    fun logFakeGpsToSentry(reason: String, detail: String, userCode: String?) {
        Sentry.withScope { scope ->
            scope.level = SentryLevel.WARNING
            scope.setTag("security_violation", "fake_gps")
            scope.setTag("user_code", userCode ?: "unknown")
            scope.setContexts("Violation Detail", detail)
            Sentry.captureMessage("Phát hiện Fake GPS: $reason")
        }
    }

    fun killFakeGpsApps(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (pkg in FAKE_GPS_PACKAGES) {
            am.killBackgroundProcesses(pkg)
        }
    }

    fun killPackage(context: Context, packageName: String) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        am.killBackgroundProcesses(packageName)
    }
}