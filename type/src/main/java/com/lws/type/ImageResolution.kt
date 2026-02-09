package com.lws.type


enum class ImageResolution(val width: Int, val height: Int, val mean: String? = "") {
    R480P(480, 600),
    R600P(600, 800),
    R720P(720, 1280, "HD"),
    R1080P(1080, 1920, "FHD"),
    R1440P(1440, 2560, "QHD"),
    R2160P(2160, 3840, "4K"),
    R4320P(4320, 7680, "8K")
}