package com.hc.dat.utils

import android.content.Context
import android.graphics.Bitmap
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.lws.type.DocumentUtils
import okhttp3.OkHttpClient
import java.io.File

object ImageLoader {
    var imageLoader: coil.ImageLoader? = null
    private const val IMAGE_CACHE_FOLDER = "image_cache"

    fun storeImageCache(context: Context, inputBitmap: Bitmap, nameFile: String): Boolean {
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

    fun buildImageLoader(context: Context, token: String? = null) {
        imageLoader = buildWithAuthenticate(context, token)
    }

    private fun buildWithAuthenticate(context: Context, token: String? = null): coil.ImageLoader {
        return coil.ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.3)
                    .build()
            }.diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(IMAGE_CACHE_FOLDER))
                    .maxSizePercent(0.2)
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .authenticator { _, response ->
                        response.request.newBuilder()
//                    .header("Authorization", token ?: "")
                            .build()
                    }.build()
            }
            .build()
    }
}
