package com.example.domain

import kotlin.math.roundToInt

object ImportMetadataReader {
    fun detectDpiFromBytes(bytes: ByteArray): Int {
        if (bytes.size < 24) return 0
        // Check PNG pHYs chunk
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) {
            val physDpi = parsePngPhys(bytes)
            if (physDpi in 50..2400) return physDpi
        }
        // Check JPEG JFIF APP0
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
            val jfifDpi = parseJpegJfif(bytes)
            if (jfifDpi in 50..2400) return jfifDpi
        }
        return 0
    }

    private fun parsePngPhys(bytes: ByteArray): Int {
        var idx = 8
        while (idx + 12 < bytes.size) {
            val len = ((bytes[idx].toInt() and 0xFF) shl 24) or
                ((bytes[idx + 1].toInt() and 0xFF) shl 16) or
                ((bytes[idx + 2].toInt() and 0xFF) shl 8) or
                (bytes[idx + 3].toInt() and 0xFF)
            val type = String(bytes, idx + 4, 4, Charsets.US_ASCII)
            if (type == "pHYs" && len == 9 && idx + 17 < bytes.size) {
                val dataOffset = idx + 8
                val ppuX = ((bytes[dataOffset].toInt() and 0xFF) shl 24) or
                    ((bytes[dataOffset + 1].toInt() and 0xFF) shl 16) or
                    ((bytes[dataOffset + 2].toInt() and 0xFF) shl 8) or
                    (bytes[dataOffset + 3].toInt() and 0xFF)
                val unit = bytes[dataOffset + 8].toInt() and 0xFF
                if (unit == 1 && ppuX > 0) {
                    return (ppuX * 0.0254).roundToInt()
                }
                return 0
            }
            if (len < 0) break
            idx += 12 + len
        }
        return 0
    }

    private fun parseJpegJfif(bytes: ByteArray): Int {
        var idx = 2
        while (idx + 18 < bytes.size) {
            if ((bytes[idx].toInt() and 0xFF) != 0xFF) break
            val marker = bytes[idx + 1].toInt() and 0xFF
            if (marker == 0xDA || marker == 0xD9) break
            val segLen = ((bytes[idx + 2].toInt() and 0xFF) shl 8) or (bytes[idx + 3].toInt() and 0xFF)
            if (marker == 0xE0 && segLen >= 16 && idx + 18 < bytes.size) {
                val tag = String(bytes, idx + 4, 5, Charsets.US_ASCII)
                if (tag == "JFIF\u0000") {
                    val units = bytes[idx + 11].toInt() and 0xFF
                    val xDensity = ((bytes[idx + 12].toInt() and 0xFF) shl 8) or (bytes[idx + 13].toInt() and 0xFF)
                    return when (units) {
                        1 -> xDensity // dots per inch
                        2 -> (xDensity * 2.54).roundToInt() // dots per cm
                        else -> 0
                    }
                }
            }
            if (segLen < 2) break
            idx += 2 + segLen
        }
        return 0
    }
}
