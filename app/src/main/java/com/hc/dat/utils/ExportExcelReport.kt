package com.hc.dat.utils

import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import com.hc.dat.model.database.entity.GPSSignalEntity
import com.hc.dat.model.database.entity.RiderSessionEntity
import com.hc.dat.model.database.entity.StudentAuthenticationEntity
import com.hc.dat.viewmodel.VerifyResult
import com.lws.type.Logger
import hc.manager.datapp.BuildConfig
import hc.manager.datapp.utils.DateUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.ClientAnchor
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.Path

object ExportExcelReport {
    private var hcReportFolder: File =
        File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_REPORT")

    init {
        if (!hcReportFolder.exists()) {
            hcReportFolder.mkdirs()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun exportSessionData(
        riderSessionEntity: RiderSessionEntity,
        listAuthenData: List<StudentAuthenticationEntity>,
        listGPSSignalData: List<GPSSignalEntity>,
        channel: Channel<Any>?
    ) {
        Logger.d("exportSessionData")
        Logger.d("exportSessionData riderSessionEntity: $riderSessionEntity")
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, _ ->
                Logger.w("Error in progress -> exportSessionData")
            }
        ) {
            if (listAuthenData.isEmpty() || listGPSSignalData.isEmpty()) {
                channel?.send("")
            } else {
                val wb = HSSFWorkbook()
                val sheet: Sheet = wb.createSheet("Phiên học")
                var rowIndex = 0
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue("Thông tin phiên học: ${riderSessionEntity.sessionId}")
                }
                ++rowIndex
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue("Tên học viên: ${riderSessionEntity.studentName}")
                }
                ++rowIndex
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue("Mã học viên: ${riderSessionEntity.studentCode}")
                }
                ++rowIndex
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue("Biển số xe: ${riderSessionEntity.plateSlug}")
                }
                ++rowIndex
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue("Seri thiết bị: ${riderSessionEntity.imei}")
                }
                ++rowIndex
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue("Mã giáo viên: ${riderSessionEntity.teacherCode}")
                }
                ++rowIndex
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue("Tổng thời gian: ${DateUtil.ConvertHms(riderSessionEntity.totalTime)}")
                }
                ++rowIndex
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue(
                        String.format(
                            "Tổng quãng đường: %.2f KM",
                            riderSessionEntity.totalDistance / 1000f
                        )
                    )
                }
                rowIndex += 2
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue("Thông tin điểm danh")
                }
                ++rowIndex
//            val loginImageRow = sheet.createRow(rowIndex)
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue("Đăng nhập: ${Utils.convertTimeStampToDateTime(riderSessionEntity.loginTime.toLong())} (${riderSessionEntity.gpsLatStart}, ${riderSessionEntity.gpsLongStart})")
                }
//            loginImageRow.createCell(10).apply {
//                setCellValue("Vị trí ảnh")
//            }
                ++rowIndex
                val loginImageRow = sheet.createRow(rowIndex)
                loginImageRow.createCell(0).apply {
                    setCellValue("Vị trí ảnh")
                    loginImageRow.height = 3000
                    insertImage(
                        col1 = 0,
                        row1 = rowIndex,
                        col2 = 2,
                        row2 = rowIndex + 1,
                        imagePath = riderSessionEntity.loginImagePath,
                        wb = wb,
                        sheet = sheet
                    )
                }
                rowIndex += 2
//            val logoutImageRow = sheet.createRow(rowIndex)
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue(
                        "Đăng xuất: ${
                            riderSessionEntity.logoutTime?.let {
                                Utils.convertTimeStampToDateTime2(
                                    it.toLong()
                                )
                            }
                        } (${riderSessionEntity.gpsLatEnd}, ${riderSessionEntity.gpsLongEnd})"
                    )
                }
                ++rowIndex
                val logoutImageRow = sheet.createRow(rowIndex)
                logoutImageRow.createCell(0).apply {
                    setCellValue("Vị trí ảnh")
                    logoutImageRow.height = 3000
                    riderSessionEntity.logoutImagePath?.also {
                        insertImage(
                            col1 = 0,
                            row1 = rowIndex,
                            col2 = 2,
                            row2 = rowIndex + 1,
                            imagePath = it,
                            wb = wb,
                            sheet = sheet
                        )
                    }
                }
//            logoutImageRow.createCell(10).apply {
//                setCellValue("Vị trí ảnh")
//            }
                rowIndex += 2
                sheet.createRow(rowIndex).createCell(0).apply {
                    setCellValue("Thông tin điểm danh")
                }
                ++rowIndex
                val headerRow = sheet.createRow(rowIndex)
                sheet.setColumnWidth(0, 1500)
                headerRow.createCell(0).apply {
                    setCellValue("STT")
                }
                sheet.setColumnWidth(1, 7000)
                headerRow.createCell(1).apply {
                    setCellValue("Thời gian")
                }
                sheet.setColumnWidth(2, 15000)
                headerRow.createCell(2).apply {
                    setCellValue("Tọa độ")
                }
                sheet.setColumnWidth(3, 7000)
                headerRow.createCell(3).apply {
                    setCellValue("Vận tốc(km/h)")
                }
                sheet.setColumnWidth(4, 7000)
                headerRow.createCell(4).apply {
                    setCellValue("Xác thực")
                }
                sheet.setColumnWidth(5, 7000)
                headerRow.createCell(5).apply {
                    setCellValue("Ảnh")
                }
                listAuthenData.forEach { authenData ->
                    Logger.d("exportSessionData authenData: $authenData")
                    ++rowIndex
                    val authenDataRow = sheet.createRow(rowIndex)
                    authenDataRow.height = 2000
                    authenDataRow.createCell(0)
                        .apply { setCellValue("${listAuthenData.indexOf(authenData) + 1}") }
                    authenDataRow.createCell(1).apply {
                        setCellValue(
                            "${
                                authenData.time
                                    .let { Utils.convertTimeStampToDateTime2(it) }
                            }"
                        )
                    }
                    authenDataRow.createCell(2)
                        .apply { setCellValue("${authenData.gpsLat}, ${authenData.gpsLong}") }
                    authenDataRow.createCell(3).apply { setCellValue("${authenData.gpsSpeed}") }
                    val recogniResult: String =
                        if (authenData.recognitionResult == VerifyResult.VERIFY_SUCCESS.code) "Thành công" else "Thất bại"
                    authenDataRow.createCell(4).apply { setCellValue(recogniResult) }
//                authenDataRow.createCell(5).apply { setCellValue("-/-") }
                    insertImage(
                        col1 = 5,
                        row1 = rowIndex,
                        col2 = 6,
                        row2 = rowIndex + 1,
                        imagePath = authenData.authenImagePath,
                        wb = wb,
                        sheet = sheet
                    )
                }
                createGpsSignalSheet(
                    listGPSSignalData = listGPSSignalData,
                    wb = wb
                )
                val convertSdfNew = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(
                    Date(listAuthenData.last().time)
                )
                val versionApp = BuildConfig.VERSION_NAME
                val fileName =
                    "${riderSessionEntity.studentCode}_${riderSessionEntity.sessionId}_$versionApp.csv"

                val dateFolder = File(hcReportFolder, convertSdfNew.format(Date()))
                if (!dateFolder.exists()) {
                    dateFolder.mkdirs()
                }

                val fileExport = File(dateFolder, fileName)
                fileExport.apply {
                    if (exists()) delete()
                    createNewFile()
                    val fos = FileOutputStream(this)
                    wb.write(fos)
                    fos.close()
                    wb.close()
                    channel?.send("")
                }
            }
        }
    }

    private fun createGpsSignalSheet(
        listGPSSignalData: List<GPSSignalEntity>,
        wb: HSSFWorkbook
    ) {
        val sheet: Sheet = wb.createSheet("Hành trình xe")
        var rowIndex = 0
        val headerRow = sheet.createRow(rowIndex)
        sheet.setColumnWidth(0, 1500)
        headerRow.createCell(0).apply {
            setCellValue("STT")
        }
        sheet.setColumnWidth(1, 7000)
        headerRow.createCell(1).apply {
            setCellValue("Thời gian")
        }
        sheet.setColumnWidth(2, 15000)
        headerRow.createCell(2).apply {
            setCellValue("Tọa độ")
        }
        sheet.setColumnWidth(3, 7000)
        headerRow.createCell(3).apply {
            setCellValue("Vận tốc(km/h)")
        }
        listGPSSignalData.forEach { gpsSignalData ->
            Logger.d("exportSessionData gpsSignalData: $gpsSignalData")
            ++rowIndex
            val authenDataRow = sheet.createRow(rowIndex)
            authenDataRow.height = 1000
            authenDataRow.createCell(0)
                .apply { setCellValue("${listGPSSignalData.indexOf(gpsSignalData) + 1}") }
            authenDataRow.createCell(1).apply {
                setCellValue(
                    "${
                        gpsSignalData.time.let {
                            Utils.convertTimeStampToDateTime2(it)
                        }
                    }"
                )
            }
            authenDataRow.createCell(2)
                .apply { setCellValue("${gpsSignalData.gpsLat}, ${gpsSignalData.gpsLong}") }
            authenDataRow.createCell(3).apply { setCellValue("${gpsSignalData.gpsSpeed}") }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun insertImage(
        col1: Int,
        row1: Int,
        col2: Int,
        row2: Int,
        imagePath: String,
        wb: HSSFWorkbook,
        sheet: Sheet
    ) {
        Logger.d("insertImage imagePath: $imagePath")
        try {
            val imageData: ByteArray = Files.readAllBytes(Path(imagePath))
            val pictureId = wb.addPicture(imageData, Workbook.PICTURE_TYPE_PNG)
            val drawing = sheet.createDrawingPatriarch()
            var anchor: ClientAnchor = wb.creationHelper.createClientAnchor()
            anchor.apply {
                setCol1(col1)
                setRow1(row1)
                setCol2(col2)
                setRow2(row2)
            }
            drawing.createPicture(anchor, pictureId)
        } catch (ex: IOException) {
            Logger.w("insertImage Error: ${ex.message}")
        }
    }
}
