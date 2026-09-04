package com.hc.dat.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.renderscript.*
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.hc.dat.service.model.UploadDeviceInfoRequest
import com.lws.type.Logger
import hc.manager.datapp.BuildConfig
import hc.manager.datapp.utils.MathUtil
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

object Utils {
    private const val ONE_HOUR_IN_MILLIS = 60 * 60 * 1000
    private const val ONE_DAY_IN_MILLIS = 24L * ONE_HOUR_IN_MILLIS
    private const val MAX_NIGHT_WINDOW_DAYS = 366L

    const val JAPAN_TIME_VIEW_FORMAT = "HH:mm"
    const val RIDER_SESSION_DATE_FORMAT = "dd-MM-yyyy\nHH:mm:ss"
    const val DATE_RECEIPT_NUMBER_FORMAT = "yyyyMMdd"
    const val LOCAL_DATE_FORMAT = "dd-MM-yyyy"
    const val LOCAL_TIME_FORMAT = "dd-MM-yyyy HH:mm"
    const val LOCAL_DATE_LIST = "yyyy/MM/dd"
    const val YEAR = "yyyy"
    const val MONTH = "MM"
    const val DAY = "dd"
    const val IMAGE_CACHE_PATH = "/cache/"
    const val DROP_HEIGHT_DEFAULT = 250

    /**
     * For cargo trucks of kilograms counter type = 1 and for trucks of liters counter type = 0
     */
    const val COUNTER_TYPE_KG = 1
    const val COUNTER_TYPE_L = 0
    var hcConfigFolder = File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_CONFIG")

    /*
    Calculates the estimated brightness of an Android Bitmap.
    pixelSpacing tells how many pixels to skip each pixel. Higher values result in better performance, but a more rough estimate.
    When pixelSpacing = 1, the method actually calculates the real average brightness, not an estimate.
    This is what the calculateBrightness() shorthand is for.
    Do not use values for pixelSpacing that are smaller than 1.
    */
    fun getBrightnessFromBitmap(input: Bitmap, pixelSpacing: Int = 1): Int {
        var R = 0
        var G = 0
        var B = 0
        val height = input.height
        val width = input.width
        var n = 0
        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)
        var i = 0
        while (i < pixels.size) {
            val color = pixels[i]
            R += Color.red(color)
            G += Color.green(color)
            B += Color.blue(color)
            n++
            i += pixelSpacing
        }
        return (R + B + G) / (n * 3)
    }

    fun nv21ToBitmap(context: Context, nv21: ByteArray, width: Int, height: Int): Bitmap? {
        val rs = RenderScript.create(context)
        val yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        val yuvType = Type.Builder(rs, Element.U8(rs)).setX(nv21.size)
        val `in` = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)
        val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
        val out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)
        `in`.copyFrom(nv21)
        yuvToRgbIntrinsic.setInput(`in`)
        yuvToRgbIntrinsic.forEach(out)
        val bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.copyTo(bmpout)
        return bmpout
    }
//    fun getBrightnessFromBitmap(input: Bitmap, pixelSpacing: Int = 1): Int {
//        var R = 0
//        var G = 0
//        var B = 0
//        val height = input.height
//        val width = input.width
//        var n = 0
//        val pixels = IntArray(width * height)
//        input.getPixels(pixels, 0, width, 0, 0, width, height)
//        var i = 0
//        while (i < pixels.size) {
//            val color = pixels[i]
//            R += Color.red(color)
//            G += Color.green(color)
//            B += Color.blue(color)
//            n++
//            i += pixelSpacing
//        }
//        return (R + B + G) / (n * 3)
//    }
@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
@SuppressLint("MissingPermission")
fun getDeviceInfo(context: Context): UploadDeviceInfoRequest {
    var seri = ""
    var imei1 = ""
    var imei2 = ""
    var simSerialNumber = ""
    var versionAppDat = ""
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
        val dataConfig = readDataConfig()
        if(dataConfig.size >= 3){
            dataConfig[0].let {
                seri = it
            }
            dataConfig[1].let {
                imei1 = it
            }
            dataConfig[2].let {
                imei2 = it
            }
        }
        versionAppDat = BuildConfig.VERSION_NAME

        return UploadDeviceInfoRequest(
            seri = seri,
            imei1 = imei1,
            imei2 = imei2,
            simReal = simSerialNumber,
            versionAppDat = versionAppDat
        )

    }
    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Logger.i("getImeiDevice: ${Build.MODEL} | Build.getSerial(): ${Build.getSerial()}")
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isCellular =
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        if (isCellular) {
            val subscriptionManager =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

            val defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId()

            for (subscriptionInfo in activeSubscriptionInfoList) {
                val subscriptionId = subscriptionInfo.subscriptionId
                val simSlotIndex = subscriptionInfo.simSlotIndex

                if (subscriptionId == defaultDataSubscriptionId) {
                    Logger.d("SIM in slot $simSlotIndex is using mobile data")
                } else {
                    Logger.d("SIM in slot1 ")
                }
            }
        } else {
            Logger.d(" No active cellular data connection")
        }

        seri = Build.getSerial()
        try {
            imei1 = telephonyManager.getImei(0)
            imei2 = telephonyManager.getImei(1)
        } catch (e: Exception) {
            Logger.e("error: version android not supported!")
        }
        simSerialNumber = telephonyManager.simSerialNumber ?: "Không lắp SIM"
        versionAppDat = BuildConfig.VERSION_NAME
        return UploadDeviceInfoRequest(
            seri = seri,
            imei1 = imei1,
            imei2 = imei2,
            simReal = simSerialNumber,
            versionAppDat = versionAppDat
        )
    } else throw RuntimeException("Version Android not supported!")
}
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @SuppressLint("MissingPermission")
    fun getImeiDevice(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Logger.i("getImeiDevice: ${Build.MODEL} | Build.getSerial(): ${Build.getSerial()}")
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.deviceId
            Logger.i("telephonyManager.deviceId: ${telephonyManager.deviceId}")
            return if (Build.MODEL.contains("FP") && !"0123456789ABCDEF".equals(
                    Build.getSerial(),
                    ignoreCase = true
                ) || telephonyManager.deviceId == null
            ) {
                Build.getSerial()
            } else {
                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.deviceId
            }
        } else throw RuntimeException("Version Android not supported!")
    }
     fun saveDataConfigToExternalStorage(serialNumber: String, imei1: String, imei2: String) {
         val textData = "$serialNumber\n$imei1\n$imei2"

         if (!hcConfigFolder.exists()) {
             hcConfigFolder.mkdirs()
         }
         val configFile = File(hcConfigFolder, "config.txt")
         try {
             val writer = FileWriter(configFile, false)
             writer.write(textData)
             writer.flush()
             writer.close()
         } catch (e: IOException) {
             e.printStackTrace()
         }
    }
    fun readDataConfig(): List<String> {
        if (isExternalStorageReadable()) {

            val file = File(hcConfigFolder, "config.txt")
            try {
                FileInputStream(file).use { input ->
                    return input.bufferedReader().readLines()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return emptyList()
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    private fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
    }
    fun getTextContentFromRes(file: File): String {
        var string: String? = ""
        val stringBuilder = StringBuilder()
        val inputStream: InputStream = FileInputStream(file)
        val reader = BufferedReader(InputStreamReader(inputStream))
        while (true) {
            try {
                if (reader.readLine().also { string = it } == null) break
            } catch (e: IOException) {
                e.printStackTrace()
            }
            stringBuilder.append(string).append("\n")
        }
        inputStream.close()
        return stringBuilder.toString()
    }

    fun getDeliveryDate(type: String = JAPAN_TIME_VIEW_FORMAT): String {
        val sdf = SimpleDateFormat(type, Locale.JAPANESE)
        return sdf.format(Date())
    }

    fun getCurrentDateTime(): String {
        val convertSdfNew = SimpleDateFormat(RIDER_SESSION_DATE_FORMAT, Locale.getDefault())
        return convertSdfNew.format(Date())
    }

    fun convertDateToString(date: Date): String {
        val convertSdfNew = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return convertSdfNew.format(date)
    }

    fun convertTimeStampToDateTime(time: Long): String {
        val convertSdfNew = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return convertSdfNew.format(Date(time * 1000))
    }
    fun convertTimeStampToTime(time: Long): String {
        val convertSdfNew = SimpleDateFormat("HH.mm", Locale.getDefault())
        return convertSdfNew.format(Date(time))
    }
    fun convertServerTimeToMilliSecond(time: String?): Long? {
        return time?.let {
            val convertSdfNew =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSZ", Locale.getDefault())
            convertSdfNew.parse(time)?.time
        } ?: let { null }
    }

    fun convertServerTimeToDate(time: String?): Date? {
        return time?.let {
//            val convertSdfNew = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSZ", Locale.getDefault())
            val convertSdfNew = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            convertSdfNew.parse(time)
        } ?: let { null }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNextDate(currentDate: Date): Date {
        val nextDate = LocalDate.parse(convertDateToString(currentDate), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).plusDays(1)
        return Date.from(nextDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    fun convertTimeStampToDateTime2(time: Long): String {
        val convertSdfNew = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return convertSdfNew.format(Date(time))
    }

//    fun getTimeStamp(): Long =
//        (Calendar.getInstance().timeInMillis / 1000L)

    /**
     * Số giây trong khoảng thời gian [startMillis, endMillis] nằm trong khung giờ đêm.
     *
     * Khung giờ đêm vắt qua nửa đêm (ví dụ 18h đến 5h sáng hôm sau) nên không thể trừ giờ
     * trực tiếp, phải dựng khung của từng ngày rồi lấy phần giao. Quét từ ngày trước ngày
     * bắt đầu vì khung mở từ tối hôm trước có thể phủ sang sáng hôm sau.
     *
     * @param startMillis mốc bắt đầu, epoch milli
     * @param endMillis mốc kết thúc, epoch milli; nhỏ hơn hoặc bằng startMillis thì trả 0
     * @param fromHour giờ bắt đầu khung đêm trong ngày, nhận cả giờ lẻ như 18.5 = 18h30
     * @param toHour giờ kết thúc khung đêm trong ngày; bằng fromHour coi như khung rỗng
     * @return số giây nằm trong khung đêm, luôn không âm
     */
    fun nightTimeSecondsBetween(
        startMillis: Long,
        endMillis: Long,
        fromHour: Double,
        toHour: Double
    ): Double {
        if (endMillis <= startMillis || fromHour == toHour) return 0.0
        val day = Calendar.getInstance().apply {
            timeInMillis = startMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val fromMillis = (fromHour * ONE_HOUR_IN_MILLIS).toLong()
        val toMillis = ((if (fromHour > toHour) toHour + 24 else toHour) * ONE_HOUR_IN_MILLIS).toLong()
        var nightMillis = 0L
        // Quét đúng số ngày mà khoảng cần tính trải qua, cộng 2 cho ngày trước ngày bắt đầu
        // và ngày cuối bị cắt dở. Không chốt cứng theo độ dài phiên tối đa vì không chỗ nào
        // bảo đảm giả định đó, mà tính thiếu giờ đêm thì âm thầm không ai thấy.
        // Chặn trên đề phòng mốc thời gian rác không làm vòng lặp chạy vô tận.
        val dayCount = ((endMillis - day.timeInMillis) / ONE_DAY_IN_MILLIS + 2)
            .coerceIn(1L, MAX_NIGHT_WINDOW_DAYS)
            .toInt()
        repeat(dayCount) {
            val dayStart = day.timeInMillis
            val overlap = minOf(endMillis, dayStart + toMillis) -
                maxOf(startMillis, dayStart + fromMillis)
            if (overlap > 0) nightMillis += overlap
            day.add(Calendar.DAY_OF_YEAR, 1)
        }
        return nightMillis / 1000.0
    }

    /**
     * Thời điểm [timeMillis] có nằm trong khung giờ đêm hay không.
     *
     * Dùng lại [nightTimeSecondsBetween] với khoảng một giây để hai hàm không thể lệch nhau
     * về định nghĩa khung đêm.
     */
    fun isInNightWindow(timeMillis: Long, fromHour: Double, toHour: Double): Boolean =
        nightTimeSecondsBetween(timeMillis, timeMillis + 1000L, fromHour, toHour) > 0

    // Todo keep old logic + 25200
    fun getTimeStamp(): Long =
        (Calendar.getInstance().timeInMillis / 1000L) + 25200

    fun getRealTimeStamp(): Long = Calendar.getInstance().timeInMillis

    fun convertLocationSpeed(location: Location): Double {
        var speed: Double = 0.0
        speed = (location.speed * 18 / 5).toDouble()
        speed = MathUtil.round(speed, 1, BigDecimal.ROUND_HALF_UP)
        val parseSpeed: BigDecimal = BigDecimal(speed)
        speed = parseSpeed.setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
        return speed
    }

    fun convertLocationSpeedCalculate(speedCalculate: Float): Double {
        var speed: Double = 0.0
        speed = (speedCalculate * 18 / 5).toDouble()
        speed = MathUtil.round(speed, 1, BigDecimal.ROUND_HALF_UP)
        val parseSpeed: BigDecimal = BigDecimal(speed)
        speed = parseSpeed.setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
        return speed
    }

    fun isValidGpsSpeed(location: Location, calculatedSpeed: Float): Boolean {

        if (!location.hasSpeed()) return false

        val gpsSpeed = location.speed // m/s

        // Reject speed âm hoặc quá lớn (> 180 km/h)
        if (gpsSpeed < 0f || gpsSpeed > 50f) return false

        // Accuracy quá tệ
        if (location.accuracy > 50f) return false

        // Nếu tốc độ thấp (<5 m/s ~18km/h) thì bỏ qua so sánh %
        if (calculatedSpeed > 5f) {
            val diff = kotlin.math.abs(gpsSpeed - calculatedSpeed)
            if (diff / calculatedSpeed > 0.4f) return false
        }

        return true
    }

    fun formatToDateHistory(tittle: String): String {
        val convertSdfNew = SimpleDateFormat(LOCAL_DATE_FORMAT, Locale.JAPANESE)
        val day = convertSdfNew.parse(tittle)
        val convertSdfOld = SimpleDateFormat(LOCAL_DATE_LIST, Locale.JAPANESE)
        return convertSdfOld.format(day)
    }

    fun formatToTimeString(viewDate: String): String {
        Logger.i("viewDate: $viewDate")
        val convertSdfOld = SimpleDateFormat(LOCAL_TIME_FORMAT, Locale.JAPANESE)
        val japanDate = convertSdfOld.parse(viewDate)
        Logger.i("japanDate: $japanDate")
        val convertSdfNew = SimpleDateFormat(JAPAN_TIME_VIEW_FORMAT, Locale.JAPANESE)
        Logger.i("formatToLocalDate japanDate :$japanDate")
        return convertSdfNew.format(japanDate)
    }

    fun formatToTime(viewDate: String): Date {
        Logger.i("viewDate: $viewDate")
        val convertSdfOld = SimpleDateFormat(JAPAN_TIME_VIEW_FORMAT, Locale.JAPANESE)
        val time = convertSdfOld.parse(viewDate)
        Logger.i("japanDate: $time")
        return time
    }
}
