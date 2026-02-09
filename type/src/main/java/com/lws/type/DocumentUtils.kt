package com.lws.type

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


object DocumentUtils {

    fun convertToMonochrome(inputBitmap: Bitmap): Bitmap {
        val width: Int = inputBitmap.width
        val height: Int = inputBitmap.height
        // create output bitmap
        // create output bitmap
        val bitmapOutput = Bitmap.createBitmap(width, height, inputBitmap.config)
        // color information
        // color information
        var a: Int
        var r: Int
        var g: Int
        var b: Int
        var pixel: Int

        // scan through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                // get pixel color
                pixel = inputBitmap.getPixel(x, y)
                a = Color.alpha(pixel)
                r = Color.red(pixel)
                g = Color.green(pixel)
                b = Color.blue(pixel)
                var gray = (0.2989 * r + 0.5870 * g + 0.1140 * b).toInt()

                // use 128 as threshold, above -> white, below -> black
                gray = if (gray > 128) 255 else 0
                // set new pixel color to output bitmap
                bitmapOutput.setPixel(x, y, Color.argb(a, gray, gray, gray))
            }
        }
        return bitmapOutput
    }

    fun resizeBitmapImage(input: Bitmap, sizeTarget: ImageResolution): Bitmap {
        val inputWidth = input.width
        val inputHeight = input.height
        Logger.i("inputWidth: $inputWidth | inputHeight: $inputHeight")
        return Bitmap.createScaledBitmap(
            input,
            sizeTarget.width,
            (sizeTarget.width * input.height) / input.width,
            false
        )
    }

    fun rotateBitmapImage (input: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270 -> 90F
            else -> 0F
        }
        val matrix = Matrix()
        matrix.preRotate(degrees)
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
    }

    fun saveImageFile(
        input: Bitmap,
        quality: Int = 100,
        path: String,
        sizeTarget: ImageResolution? = null
    ): Boolean {
        val imageBitmap: Bitmap = sizeTarget?.let {
            resizeBitmapImage(input, sizeTarget)
        } ?: input
        try {
            val file = File(path)
            val fileOutputStream = FileOutputStream(file)
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (ex: IOException) {
            Logger.e("Error: $ex")
            return false
        }
        return true
    }

    fun convertViewToBitmap (view: View, width: Int, height: Int): Bitmap {
        Logger.i("view.width: ${view.width} | view.height: ${view.height}" +
                "\nwidth: $width | height: $height")
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return if (width <= view.width)
            bitmap
        else {
            resizeBitmap(bitmap, width, height)
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)

        val ratioX: Float = newWidth / bitmap.width.toFloat()
        val ratioY: Float = newHeight / bitmap.height.toFloat()
        val middleX: Float = newWidth / 2.0f
        val middleY: Float = newHeight / 2.0f

        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)

        val canvas = Canvas(scaledBitmap)
//        canvas.matrix = scaleMatrix
        canvas.drawBitmap(
            bitmap,
            middleX - bitmap.width / 2,
            middleY - bitmap.height / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )

        return scaledBitmap
    }

    fun covertViewToPdf(view: View, width: Int, height: Int, pathSave: String): Boolean {
        val bitmap = convertViewToBitmap(view, width, height)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
//        val paint = Paint()
//        canvas.drawPaint(paint)
//
//        paint.color = Color.WHITE
        canvas.drawBitmap(bitmap, 0F, 0F, null)
        pdfDocument.finishPage(page)
        try {
            val file = File(pathSave)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
        } catch (ex: IOException) {
            Logger.e("Error: $ex")
            return false
        }
        return true
    }
}