package com.lws.device.camerapreview

import com.lws.device.camerapreview.sample.CameraPreviewData


/**
 * Created by Duc Bui on 2023/07.
 * Author: Duc Bui
 * Email: ducbui1890@gmail.com
 * Hanoi, VN.
 */
data class Nv21ImageData(
    var nv21Data: ByteArray,
    var width: Int,
    var height: Int,
    var rotation: Int,
    var mirror: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Nv21ImageData

        if (!nv21Data.contentEquals(other.nv21Data)) return false

        return true
    }

    override fun hashCode(): Int {
        return nv21Data.contentHashCode()
    }

    fun clone(): Nv21ImageData {
        return Nv21ImageData(
            nv21Data = nv21Data.clone(),
            width = width,
            height = height,
            rotation = rotation,
            mirror = mirror,
        )
    }
}

fun CameraPreviewData.convertToNv21ImageData() = Nv21ImageData(
    nv21Data = this.nv21Data,
    width = this.width,
    height = this.height,
    rotation = this.rotation,
    mirror = this.mirror,
)
