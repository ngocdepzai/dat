package com.omi.face

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


/**
 * Created by Duc Bui on 2024/06.
 * Author: Duc Bui
 * Email: ducbui1890@gmail.com
 * Hanoi, VN.
 */
object Utils {

    // Rotate the given `source` by `degrees`.
    // See this SO answer -> https://stackoverflow.com/a/16219591/10878733
    fun rotateBitmap( source: Bitmap , degrees : Float ): Bitmap {
        val matrix = Matrix()
        matrix.postRotate( degrees )
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix , false )
    }

    /**
     * Crop the given bitmap with the given rect.
     * * Set nameSaving not null if you want to save to local storage
      */
    fun cropRectFromBitmap(activity: Context, source: Bitmap, rect: Rect, nameSaving: String? = null): Bitmap? {
        if (rect.width() <= 0 || rect.height() <= 0 || rect.top <= 0 || rect.top <= 0 || rect.left <= 0)  return null
        var width = rect.width()
        var height = rect.height()
        if ( (rect.left + width) > source.width ){
            width = source.width - rect.left
        }
        if ( (rect.top + height ) > source.height ){
            height = source.height - rect.top
        }
        val croppedBitmap = Bitmap.createBitmap( source , rect.left , rect.top , width , height )
        nameSaving?.also {
            saveBitmap(activity, croppedBitmap , it )
        }
        return croppedBitmap
    }

    fun saveBitmap(activity: Context, image: Bitmap, name: String) {
        val mimeType = "image/jpeg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/vncitizenid")
                }
            }
            val resolver = activity.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                var outputStream: OutputStream? = null
                try {
                    outputStream = resolver.openOutputStream(it)
                    image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    outputStream?.close()
                }
            }
        } else {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/vncitizenid")
            if (!file.exists()) {
                file.mkdirs()
            }

            val imageFile = File(file, name)
            var outputStream: FileOutputStream? = null
            try {
                outputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream!!)
                outputStream!!.flush()

                // Add to gallery
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                }

                val resolver = activity.contentResolver
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                outputStream?.close()
            }
        }
    }

    fun convertBitmapToNV21(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val yuv = ByteArray(width * height * 3 / 2)
        val argb = IntArray(width * height)

        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        encodeYUV420SP(yuv, argb, width, height)

        return yuv
    }

    private fun encodeYUV420SP(yuv: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height

        var yIndex = 0
        var uvIndex = frameSize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val rgb = argb[j * width + i]

                val r = Color.red(rgb)
                val g = Color.green(rgb)
                val b = Color.blue(rgb)

                // Convert RGB to YUV
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                    yuv[uvIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                }
            }
        }
    }
}