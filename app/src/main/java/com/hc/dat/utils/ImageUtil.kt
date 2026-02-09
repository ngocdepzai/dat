package com.hc.dat.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import com.lws.type.DocumentUtils
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.OkHttpClient
import java.io.File

@SuppressLint("StaticFieldLeak")
object ImageUtil {
    var imageLoader: Picasso? = null
    private lateinit var context: Context
    const val IMAGE_CACHE_FOLDER = "image_cache"

    fun storeImageCache(inputBitmap: Bitmap, nameFile: String): Boolean {
        val folderPath = context.cacheDir.path + "/" + IMAGE_CACHE_FOLDER
        val folder = File(folderPath)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val filePath = "$folderPath/$nameFile"
        return DocumentUtils.saveImageFile(
            input = inputBitmap,
            path = filePath
        )
    }

    fun buildPicasso(context: Context, token: String? = null) {
        imageLoader = buildPicassoWithAuthenticate(context, token)
    }

    @SuppressLint("StaticFieldLeak")
    private fun buildPicassoWithAuthenticate(context: Context, token: String? = null): Picasso {
        ImageUtil.context = context
        val client = OkHttpClient.Builder()
            .authenticator { _, response ->
                response.request.newBuilder()
//                    .header("Authorization", token ?: "")
                    .build()
            }.build()
        return Picasso.Builder(context)
            .downloader(OkHttp3Downloader(client))
            .build()
    }
}
