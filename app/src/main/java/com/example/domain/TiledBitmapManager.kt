package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

data class LoadedMapRaster(
    val bitmap: Bitmap,
    val pageCount: Int = 1,
    val detectedDpi: Int = 0
)

class TiledBitmapManager {
    private var cachedUri: String? = null
    private var cachedPage: Int = -1
    private var cachedBitmap: Bitmap? = null

    fun loadFromUri(
        context: Context,
        uriString: String,
        pageIndex: Int = 0,
        maxDimension: Int = 2400
    ): LoadedMapRaster? {
        if (uriString == cachedUri && pageIndex == cachedPage && cachedBitmap != null && !cachedBitmap!!.isRecycled) {
            return LoadedMapRaster(cachedBitmap!!, 1, 0)
        }
        val uri = Uri.parse(uriString)
        val mime = context.contentResolver.getType(uri).orEmpty()
        val isPdf = mime.contains("pdf", ignoreCase = true) || uriString.endsWith(".pdf", ignoreCase = true)
        if (isPdf) {
            return renderPdfPage(context, uri, pageIndex, maxDimension)?.also {
                cachedUri = uriString
                cachedPage = pageIndex
                cachedBitmap = it.bitmap
            }
        }
        return decodeRasterImage(context, uri, maxDimension)?.also {
            cachedUri = uriString
            cachedPage = 0
            cachedBitmap = it.bitmap
        }
    }

    fun loadFromFile(file: File, maxDimension: Int = 2400): LoadedMapRaster? {
        if (!file.exists()) return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        val detectedDpi = ImportMetadataReader.detectDpiFromBytes(bytes)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
        return LoadedMapRaster(bmp, 1, detectedDpi)
    }

    private fun decodeRasterImage(context: Context, uri: Uri, maxDimension: Int): LoadedMapRaster? {
        return runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val dpi = ImportMetadataReader.detectDpiFromBytes(bytes)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
            LoadedMapRaster(bmp, 1, dpi)
        }.getOrNull()
    }

    private fun renderPdfPage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        maxDimension: Int
    ): LoadedMapRaster? {
        return runCatching {
            val tempFile = File(context.cacheDir, "imported_mouza_${uri.hashCode()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount.coerceAtLeast(1)
            val safeIdx = pageIndex.coerceIn(0, count - 1)
            val page = renderer.openPage(safeIdx)
            val scale = minOf(
                maxDimension.toFloat() / page.width.coerceAtLeast(1),
                maxDimension.toFloat() / page.height.coerceAtLeast(1),
                2.5f
            ).coerceAtLeast(0.5f)
            val w = (page.width * scale).toInt().coerceAtLeast(100)
            val h = (page.height * scale).toInt().coerceAtLeast(100)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            LoadedMapRaster(bmp, pageCount = count, detectedDpi = (72f * scale).toInt())
        }.getOrNull()
    }

    private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        while (width / sample > maxDim || height / sample > maxDim) {
            sample *= 2
        }
        return sample
    }

    fun generateBuiltInSampleMouzaSheet(width: Int = 1400, height: Int = 1000): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#FAF8F2"))

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8D99AE")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val gridPaint = Paint().apply {
            color = Color.parseColor("#E5E0D3")
            strokeWidth = 1f
        }
        for (x in 0..width step 80) c.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        for (y in 0..height step 80) c.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)

        // Draw subtle cadastral parcel lines resembling a mouza sheet
        val parcelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0A899")
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7D766B")
            textSize = 20f
        }
        val cols = listOf(140f, 420f, 700f, 980f, 1260f)
        val rows = listOf(130f, 380f, 640f, 880f)
        for (r in 0 until rows.size - 1) {
            for (col in 0 until cols.size - 1) {
                val x0 = cols[col] + ((r % 2) * 14f)
                val x1 = cols[col + 1] + (((r + 1) % 2) * 10f)
                val y0 = rows[r]
                val y1 = rows[r + 1]
                c.drawRect(x0, y0, x1, y1, parcelPaint)
                val dagNum = BengaliFormatters.toBengaliDigits((r * 4 + col + 101).toString())
                c.drawText(dagNum, (x0 + x1) * 0.5f - 18f, (y0 + y1) * 0.5f, textPaint)
            }
        }
        c.drawRect(40f, 40f, width - 40f, height - 40f, borderPaint)
        return bmp
    }

    fun clear() {
        cachedBitmap = null
        cachedUri = null
        cachedPage = -1
    }
}
