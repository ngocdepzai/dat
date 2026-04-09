package com.lws.type

import java.lang.StringBuilder
import java.math.BigInteger

fun String.convertHexToByteArray(): ByteArray {
    val filtered = filterHex()
    return ByteArray(filtered.length / 2) { index ->
        filtered.substring(2 * index, 2 * index + 2).toInt(16).toByte()
    }
}

/**
 * Remove all characters that are not hexadecimal digits.
 */
fun String.filterHex() = filter { it.isHex }

val Char.isHex: Boolean
    get() = this in '0'..'9' || this in 'A'..'F' || this in 'a'..'f'

fun String.calculateLengthFieldFromValue(): String =
    (this.length / 2).convertToHex()

/**
 * Convert Binary string to Hex string
 *
 */
fun String.convertBinaryToHex(): String {
    return BigInteger(this, 2).toString(16).padStart(2, '0').toUpperCase()
}

fun Int.convertToHex(): String =
    this.toString(16).toUpperCase().padStart(2, '0')
fun Long.convertToHex(): String =
    this.toString(16).toUpperCase().padStart(2, '0')

fun ByteArray.convertToString(): String =
    java.lang.String.format("%0" + (this.size * 2).toString() + "X", BigInteger(1, this))

fun String.convertHexToString(): String {
    var result = StringBuilder()
    this.chunked(2).forEach {
        result.append(Integer.parseInt(it, 16).toChar())
    }
    return result.toString()
}

fun String.toBinaryString(): String? = this.toIntOrNull(16)?.let {
    Integer.toBinaryString(it)
}

fun String.convertHexToLong(): Long = this.toLong(16)
fun String.convertHexToInt(): Int = this.toInt(16)
fun String.convertBinaryToDecimal(): Int = this.toInt(2)
