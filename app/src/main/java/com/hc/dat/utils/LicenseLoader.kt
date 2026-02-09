package com.hc.dat.utils

import android.os.Environment
import com.hc.dat.viewmodel.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream

object LicenseLoader {

    const val zipLicenseFileName = "licenses.zip"
    const val url = "http://datversion.hcsky.vn/face-recognization_cer.zip";
    var hcLicenseFolder =
        File(Environment.getExternalStorageDirectory().toString() + "/HC_DAT_LICENSE")

    fun createFolder() {
            if (!hcLicenseFolder.exists()) {
                hcLicenseFolder.mkdirs()
            }
    }

    suspend fun downloadZipFile() {
        return withContext(Dispatchers.IO) {
            try {
                    val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                val file = File(hcLicenseFolder, zipLicenseFileName)
                val inputStream: InputStream = response.body!!.byteStream()
                val outputStream: OutputStream = FileOutputStream(file)

                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unzip(
    ) {
        val zipFilePath = "$hcLicenseFolder/$zipLicenseFileName"
        val destDirectory = hcLicenseFolder.toString()
        val buffer = ByteArray(1024)
        try {
            // Tạo thư mục đích nếu nó chưa tồn tại
            val destDir = File(destDirectory)
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            // Tạo một luồng đọc cho tệp ZIP
            val zipInputStream = ZipInputStream(FileInputStream(zipFilePath))
            // Đọc từng entry từ tệp ZIP
            var zipEntry = zipInputStream.nextEntry
            while (zipEntry != null) {
                val filePath = destDirectory + File.separator + zipEntry.name
                // Nếu entry là một thư mục, tạo thư mục mới
                if (zipEntry.isDirectory) {
                    val dir = File(filePath)
                    dir.mkdirs()
                } else {
                    // Nếu entry là một tệp, giải nén nó
                    val fos = FileOutputStream(filePath)
                    var len: Int
                    while (zipInputStream.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                    }
                    fos.close()
                }
                // Đọc entry tiếp theo trong tệp ZIP
                zipEntry = zipInputStream.nextEntry
            }
            // Đóng luồng đọc
            zipInputStream.closeEntry()
            zipInputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteAllZipFiles(callback: (action: DownloadStatus) -> Unit) {
        val directory = File(hcLicenseFolder.toString())
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile && file.name.endsWith(".zip")) {
                        file.delete()
                    }
                }
            }
            callback(
                DownloadStatus.LICENSE_READY
            )
        }
    }

    fun deleteFilesInDirectory() {
        val directory = File(hcLicenseFolder.toString())
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    file.delete()
                }
            }
        }
    }

}