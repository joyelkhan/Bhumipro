package com.example.domain.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.example.data.model.PrintConfig
import com.example.data.model.ReportSectionLayout
import com.example.domain.GeometryUtils
import com.example.domain.PdfAndPrintHelper
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

sealed class ReportPageSpec {
    data object FullSheet : ReportPageSpec()
    data class SinglePlot(val plot: ReportPlot) : ReportPageSpec()
}

object ReportPdfExporter {
    private const val TAG = "ReportPdfExporter"
    const val CANCELLED_MESSAGE_BN = "রপ্তানি বাতিল করা হয়েছে"

    fun buildPagePlan(
        data: SurveyReportData,
        individualPlotPages: Boolean
    ): List<ReportPageSpec> {
        val list = mutableListOf<ReportPageSpec>(ReportPageSpec.FullSheet)
        if (individualPlotPages && data.landPlots.isNotEmpty()) {
            data.landPlots.forEach { plot ->
                list.add(ReportPageSpec.SinglePlot(plot))
            }
        }
        return list
    }

    fun generateSurveyPadPdf(
        context: Context,
        data: SurveyReportData,
        template: ReportTemplate,
        typeface: Typeface,
        qrBitmap: Bitmap? = null,
        logoBitmap: Bitmap? = null,
        signatureBitmap: Bitmap? = null,
        insetBitmap: Bitmap? = null,
        sections: ReportSectionLayout = ReportSectionLayout(),
        printConfig: PrintConfig = PrintConfig(),
        outFile: File,
        isCancelled: AtomicBoolean = AtomicBoolean(false),
        onProgress: (page: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<File> {
        val pageLayout = PageLayout.from(printConfig)
        val plan = buildPagePlan(data, printConfig.individualPlotPages)
        val total = plan.size.coerceAtLeast(1)
        outFile.parentFile?.mkdirs()

        val vectorResult = tryVectorPdf(
            data = data,
            template = template,
            typeface = typeface,
            qrBitmap = qrBitmap,
            logoBitmap = logoBitmap,
            signatureBitmap = signatureBitmap,
            insetBitmap = insetBitmap,
            sections = sections,
            printConfig = printConfig,
            pageLayout = pageLayout,
            plan = plan,
            total = total,
            outFile = outFile,
            isCancelled = isCancelled,
            onProgress = onProgress
        )

        if (vectorResult.isSuccess) return vectorResult

        val vectorErr = vectorResult.exceptionOrNull()
        if (vectorErr is ExportCancelledException) {
            outFile.delete()
            return Result.failure(vectorErr)
        }

        Log.w(TAG, "vector PDF path failed: ${vectorErr?.message}", vectorErr)

        // Raster fallback using the SAME BengaliSurveyReportRenderer
        return tryRasterFallbackPdf(
            data = data,
            template = template,
            typeface = typeface,
            qrBitmap = qrBitmap,
            logoBitmap = logoBitmap,
            signatureBitmap = signatureBitmap,
            insetBitmap = insetBitmap,
            sections = sections,
            printConfig = printConfig,
            pageLayout = pageLayout,
            plan = plan,
            total = total,
            outFile = outFile,
            isCancelled = isCancelled,
            onProgress = onProgress
        )
    }

    private fun tryVectorPdf(
        data: SurveyReportData,
        template: ReportTemplate,
        typeface: Typeface,
        qrBitmap: Bitmap?,
        logoBitmap: Bitmap?,
        signatureBitmap: Bitmap?,
        insetBitmap: Bitmap?,
        sections: ReportSectionLayout,
        printConfig: PrintConfig,
        pageLayout: PageLayout,
        plan: List<ReportPageSpec>,
        total: Int,
        outFile: File,
        isCancelled: AtomicBoolean,
        onProgress: (Int, Int) -> Unit
    ): Result<File> = runCatching {
        val pdf = PdfDocument()
        try {
            val w = pageLayout.widthPt.toInt().coerceAtLeast(100)
            val h = pageLayout.heightPt.toInt().coerceAtLeast(100)
            plan.forEachIndexed { idx, spec ->
                if (isCancelled.get()) throw ExportCancelledException()
                val pageNum = idx + 1
                onProgress(pageNum, total)
                val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageNum).create()
                val page = pdf.startPage(pageInfo)
                val (pageData, overrideBounds) = resolveSpec(data, spec)
                BengaliSurveyReportRenderer.drawReport(
                    canvas = page.canvas,
                    pageW = w.toFloat(),
                    pageH = h.toFloat(),
                    data = pageData,
                    template = template,
                    typeface = typeface,
                    qrBitmap = qrBitmap,
                    logoBitmap = logoBitmap,
                    signatureBitmap = signatureBitmap,
                    insetBitmap = insetBitmap,
                    sections = sections,
                    pageLayout = pageLayout,
                    overrideBounds = overrideBounds
                )
                if (printConfig.showPageNumbers && total > 1) {
                    drawPageNumberFooter(page.canvas, w.toFloat(), h.toFloat(), pageLayout.marginPt, pageNum, total, typeface)
                }
                pdf.finishPage(page)
            }
            if (isCancelled.get()) throw ExportCancelledException()
            FileOutputStream(outFile).use { out ->
                pdf.writeTo(out)
                out.flush()
            }
            if (!outFile.exists() || outFile.length() <= 10L) {
                throw IOException("Empty PDF output")
            }
            outFile
        } finally {
            runCatching { pdf.close() }
        }
    }

    private fun tryRasterFallbackPdf(
        data: SurveyReportData,
        template: ReportTemplate,
        typeface: Typeface,
        qrBitmap: Bitmap?,
        logoBitmap: Bitmap?,
        signatureBitmap: Bitmap?,
        insetBitmap: Bitmap?,
        sections: ReportSectionLayout,
        printConfig: PrintConfig,
        pageLayout: PageLayout,
        plan: List<ReportPageSpec>,
        total: Int,
        outFile: File,
        isCancelled: AtomicBoolean,
        onProgress: (Int, Int) -> Unit
    ): Result<File> = runCatching {
        val jpegPages = mutableListOf<Triple<ByteArray, Int, Int>>()
        val w = pageLayout.widthPt.toInt().coerceAtLeast(100)
        val h = pageLayout.heightPt.toInt().coerceAtLeast(100)

        plan.forEachIndexed { idx, spec ->
            if (isCancelled.get()) {
                outFile.delete()
                throw ExportCancelledException()
            }
            val pageNum = idx + 1
            onProgress(pageNum, total)
            val (pageData, overrideBounds) = resolveSpec(data, spec)
            val bmp = BengaliSurveyReportRenderer.renderPreviewBitmap(
                data = pageData,
                template = template,
                typeface = typeface,
                qrBitmap = qrBitmap,
                logoBitmap = logoBitmap,
                signatureBitmap = signatureBitmap,
                insetBitmap = insetBitmap,
                sections = sections,
                pageLayout = pageLayout,
                overrideBounds = overrideBounds
            )
            if (printConfig.showPageNumbers && total > 1) {
                drawPageNumberFooter(Canvas(bmp), w.toFloat(), h.toFloat(), pageLayout.marginPt, pageNum, total, typeface)
            }
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            jpegPages.add(Triple(baos.toByteArray(), bmp.width, bmp.height))
            bmp.recycle()
        }
        if (isCancelled.get()) {
            outFile.delete()
            throw ExportCancelledException()
        }
        writeMinimalJpegPdf(jpegPages, w, h, outFile)
        outFile
    }.onFailure { err ->
        if (err is ExportCancelledException) outFile.delete()
    }

    private fun resolveSpec(data: SurveyReportData, spec: ReportPageSpec): Pair<SurveyReportData, ReportBounds?> {
        return when (spec) {
            is ReportPageSpec.FullSheet -> data to null
            is ReportPageSpec.SinglePlot -> {
                val box = GeometryUtils.boundingBox(spec.plot.points)
                val singleBounds = ReportBounds(box.minX, box.minY, box.maxX, box.maxY)
                val singleData = data.copy(
                    plots = listOf(spec.plot),
                    landPlots = if (spec.plot.isRoad) emptyList() else listOf(spec.plot),
                    roadPlots = if (spec.plot.isRoad) listOf(spec.plot) else emptyList(),
                    bounds = singleBounds
                )
                singleData to singleBounds
            }
        }
    }

    private fun drawPageNumberFooter(
        canvas: Canvas,
        pageW: Float,
        pageH: Float,
        marginPt: Float,
        pageNum: Int,
        total: Int,
        typeface: Typeface
    ) {
        val label = PrintPageMath.pageNumberLabel(pageNum, total)
        if (label.isBlank()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = 0xFF56575C.toInt()
            textSize = 8.2f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, pageW * 0.5f, pageH - (marginPt * 0.45f).coerceAtLeast(6f), paint)
    }

    private fun writeMinimalJpegPdf(
        pages: List<Triple<ByteArray, Int, Int>>,
        pageW: Int,
        pageH: Int,
        outFile: File
    ) {
        val baos = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()
        fun writeStr(s: String) = baos.write(s.toByteArray(Charsets.US_ASCII))

        writeStr("%PDF-1.4\n")
        // Object 1: Catalog, Object 2: Pages
        val pageObjIds = mutableListOf<Int>()
        var nextObjId = 3
        val pageEntries = pages.map { (jpeg, imgW, imgH) ->
            val pageId = nextObjId++
            val contentsId = nextObjId++
            val imgId = nextObjId++
            pageObjIds.add(pageId)
            Triple(Triple(pageId, contentsId, imgId), jpeg, imgW to imgH)
        }

        offsets.add(baos.size())
        writeStr("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        offsets.add(baos.size())
        val kids = pageObjIds.joinToString(" ") { "$it 0 R" }
        writeStr("2 0 obj\n<< /Type /Pages /Kids [ $kids ] /Count ${pageObjIds.size} >>\nendobj\n")

        for ((ids, jpeg, dims) in pageEntries) {
            val (pageId, contentsId, imgId) = ids
            val (imgW, imgH) = dims
            offsets.add(baos.size())
            writeStr(
                "$pageId 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageW $pageH] " +
                    "/Resources << /XObject << /Im0 $imgId 0 R >> >> /Contents $contentsId 0 R >>\nendobj\n"
            )
            val contentStream = "q $pageW 0 0 $pageH 0 0 cm /Im0 Do Q\n"
            offsets.add(baos.size())
            writeStr("$contentsId 0 obj\n<< /Length ${contentStream.length} >>\nstream\n$contentStream\nendstream\nendobj\n")

            offsets.add(baos.size())
            writeStr(
                "$imgId 0 obj\n<< /Type /XObject /Subtype /Image /Width $imgW /Height $imgH " +
                    "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>\nstream\n"
            )
            baos.write(jpeg)
            writeStr("\nendstream\nendobj\n")
        }

        val xrefStart = baos.size()
        val totalObjs = offsets.size + 1
        writeStr("xref\n0 $totalObjs\n0000000000 65535 f \n")
        for (off in offsets) {
            writeStr(String.format(java.util.Locale.US, "%010d 00000 n \n", off))
        }
        writeStr("trailer\n<< /Size $totalObjs /Root 1 0 R >>\nstartxref\n$xrefStart\n%%EOF\n")

        FileOutputStream(outFile).use { out ->
            out.write(baos.toByteArray())
            out.flush()
        }
    }

    fun saveSurveyPadPdfToDownloads(
        context: Context,
        data: SurveyReportData,
        template: ReportTemplate,
        typeface: Typeface,
        qrBitmap: Bitmap? = null,
        logoBitmap: Bitmap? = null,
        signatureBitmap: Bitmap? = null,
        insetBitmap: Bitmap? = null,
        sections: ReportSectionLayout = ReportSectionLayout(),
        printConfig: PrintConfig = PrintConfig(),
        isCancelled: AtomicBoolean = AtomicBoolean(false),
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Pair<File, String>> {
        val baseName = PrintPageMath.buildFileName(
            mapName = data.mouzaName,
            title = printConfig.headerTitle,
            date = data.ticket.dateText
        )
        val fileName = "$baseName.pdf"
        val cacheFile = File(context.cacheDir, "exports/$fileName")
        val pdfResult = generateSurveyPadPdf(
            context = context,
            data = data,
            template = template,
            typeface = typeface,
            qrBitmap = qrBitmap,
            logoBitmap = logoBitmap,
            signatureBitmap = signatureBitmap,
            insetBitmap = insetBitmap,
            sections = sections,
            printConfig = printConfig,
            outFile = cacheFile,
            isCancelled = isCancelled,
            onProgress = onProgress
        )
        val generatedFile = pdfResult.getOrElse { return Result.failure(it) }
        val savedLocation = PdfAndPrintHelper.copyFileToDownloads(
            context = context,
            sourceFile = generatedFile,
            fileName = fileName,
            mimeType = "application/pdf"
        ).getOrElse { return Result.failure(it) }
        return Result.success(generatedFile to savedLocation)
    }

    fun mapExportError(t: Throwable): String {
        if (t is ExportCancelledException) return CANCELLED_MESSAGE_BN
        val msg = t.message.orEmpty()
        return when {
            t is OutOfMemoryError ->
                "মেমরি কম — বড় ম্যাপ/বেশি প্লটে রপ্তানি ব্যর্থ। কম প্লট নির্বাচন করে চেষ্টা করুন। (low memory)"
            t is SecurityException ->
                "স্টোরেজ অনুমতি নেই। (permission denied)"
            t is IOException ->
                "স্টোরেজ লেখা যায়নি — স্টোরেজ পূর্ণ বা অনুমতি নেই। (storage write failed)"
            t is IllegalStateException && msg.contains("closed", ignoreCase = true) ->
                "পিডিএফ ইঞ্জিন হঠাৎ বন্ধ হয়েছে — আবার চেষ্টা করুন। (pdf engine closed)"
            else ->
                "রপ্তানি ব্যর্থ: ${msg.ifBlank { t.javaClass.simpleName }}"
        }
    }
}

class ExportCancelledException : RuntimeException(ReportPdfExporter.CANCELLED_MESSAGE_BN)
