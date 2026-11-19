package com.example.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object BarcodeAndQrHelper {

    fun generateQrBitmap(content: String, sizePx: Int = 180): Bitmap? {
        if (content.isBlank() || sizePx <= 8) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (y in 0 until sizePx) {
                for (x in 0 until sizePx) {
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        }.getOrNull()
    }

    fun generateCode128Bitmap(content: String, widthPx: Int = 260, heightPx: Int = 56): Bitmap? {
        val safe = content.filter { it.code in 32..126 }.ifBlank { "BHUMIPRO" }
        return runCatching {
            val matrix = MultiFormatWriter().encode(safe, BarcodeFormat.CODE_128, widthPx, heightPx)
            val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            for (y in 0 until heightPx) {
                for (x in 0 until widthPx) {
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        }.getOrNull()
    }
}
