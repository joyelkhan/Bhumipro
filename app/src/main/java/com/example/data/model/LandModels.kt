package com.example.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

object MoshiProvider {
    val moshi: Moshi by lazy { Moshi.Builder().build() }
}

@JsonClass(generateAdapter = true)
data class PointF(
    val x: Float = 0f,
    val y: Float = 0f
)

@JsonClass(generateAdapter = true)
data class BoundarySegment(
    val startLabel: String = "",
    val endLabel: String = "",
    val lengthFeet: Double = 0.0,
    val formattedLength: String = "",
    val bearingDegrees: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class LandPlot(
    val id: String,
    val name: String,
    val colorHex: String = "#2563EB",
    val points: List<PointF> = emptyList(),
    val areaShotangsho: Double = 0.0,
    val areaSqFt: Double = 0.0,
    val perimeterFeet: Double = 0.0,
    val segments: List<BoundarySegment> = emptyList(),
    val isRoad: Boolean = false,
    val isHidden: Boolean = false,
    val isHighlighted: Boolean = false,
    val annotationText: String = "",
    val ownerName: String = "",
    val dagNumber: String = "",
    val khatianRef: String = "",
    val notes: String = "",
    val isLocked: Boolean = false,
    val status: String = "সক্রিয়",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val labelStyle: PlotLabelStyle? = null
)

enum class GisLayerType {
    BASE_MAP,
    GRID,
    CAD_OVERLAY,
    PLOTS,
    ROADS,
    MEASUREMENTS,
    LABELS,
    NOTES
}

@JsonClass(generateAdapter = true)
data class GisLayerItem(
    val id: String,
    val type: GisLayerType = GisLayerType.PLOTS,
    val nameBengali: String,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val opacity: Float = 1.0f,
    val colorHex: String = "#2563EB",
    val zOrder: Int = 0
)

fun defaultGisLayers(): List<GisLayerItem> = listOf(
    GisLayerItem("layer_base", GisLayerType.BASE_MAP, "মূল মৌজা ম্যাপ", visible = true, opacity = 0.9f, colorHex = "#1E293B", zOrder = 0),
    GisLayerItem("layer_grid", GisLayerType.GRID, "সার্ভে গ্রিড", visible = false, opacity = 0.35f, colorHex = "#133C2D", zOrder = 1),
    GisLayerItem("layer_cad", GisLayerType.CAD_OVERLAY, "ক্যাড/সাবেক ওভারলে", visible = true, opacity = 0.75f, colorHex = "#0EA5E9", zOrder = 2),
    GisLayerItem("layer_plots", GisLayerType.PLOTS, "প্লট পলিগন", visible = true, opacity = 1.0f, colorHex = "#2563EB", zOrder = 3),
    GisLayerItem("layer_roads", GisLayerType.ROADS, "রাস্তা", visible = true, opacity = 1.0f, colorHex = "#64748B", zOrder = 4),
    GisLayerItem("layer_measure", GisLayerType.MEASUREMENTS, "দৈর্ঘ্য ও পরিমাপ", visible = true, opacity = 1.0f, colorHex = "#133C2D", zOrder = 5),
    GisLayerItem("layer_labels", GisLayerType.LABELS, "প্লট ও দাগ লেবেল", visible = true, opacity = 1.0f, colorHex = "#0F172A", zOrder = 6),
    GisLayerItem("layer_notes", GisLayerType.NOTES, "টীকা ও নোট", visible = true, opacity = 1.0f, colorHex = "#9333EA", zOrder = 7)
)

enum class WorkspaceMode(val id: String, val titleBn: String) {
    MOUZA_SURVEY("mouza_survey", "মৌজা ম্যাপে জমি পরিমাপ"),
    MAP_JOINT("map_joint", "ম্যাপ জয়েন্ট"),
    COMPARATIVE_PANTOGRAPH("comparative_pantograph", "তুলনামূলক প্যান্টোগ্রাফ"),
    ALL_MOUZA_MAPS("all_mouza_maps", "সব মৌজা ম্যাপ"),
    CAD_DWG("cad_dwg", "ক্যাড / ডিডব্লিউজি ওভারলে"),
    SATELLITE_MOUZA("satellite_mouza", "স্যাটেলাইট ও মৌজা");

    companion object {
        fun fromId(id: String?): WorkspaceMode =
            entries.firstOrNull { it.id == id } ?: MOUZA_SURVEY
    }
}

enum class DivisionType(val labelBn: String) {
    EQUAL("সমান ভাগ"),
    PERCENTAGE("শতাংশ অনুযায়ী"),
    CUSTOM_AREA("কাস্টম শতক")
}

data class DivisionPartSummary(
    val name: String,
    val areaSqFt: Double,
    val areaShotok: Double,
    val percentage: Double
)

data class DivisionReport(
    val originalAreaSqFt: Double = 0.0,
    val originalAreaShotok: Double = 0.0,
    val roadAreaSqFt: Double = 0.0,
    val roadAreaShotok: Double = 0.0,
    val netAreaSqFt: Double = 0.0,
    val netAreaShotok: Double = 0.0,
    val parts: List<DivisionPartSummary> = emptyList(),
    val warningMessage: String? = null
)

data class RoadLayoutOption(
    val id: String,
    val labelBn: String,
    val groupBn: String
)

val roadOptionsList: List<RoadLayoutOption> = listOf(
    RoadLayoutOption("none_vertical", "রাস্তা নেই (খাড়া বিভাজন)", "সাধারণ"),
    RoadLayoutOption("none_horizontal", "রাস্তা নেই (আড়াআড়ি বিভাজন)", "সাধারণ"),
    RoadLayoutOption("cross", "ক্রস (+) রাস্তা", "মধ্যাঞ্চল"),
    RoadLayoutOption("middle_vertical", "মাঝখান দিয়ে খাড়া রাস্তা", "মধ্যাঞ্চল"),
    RoadLayoutOption("middle_horizontal", "মাঝখান দিয়ে আড়াআড়ি রাস্তা", "মধ্যাঞ্চল"),
    RoadLayoutOption("side_top", "উপরের পাশে রাস্তা", "পার্শ্ব রাস্তা"),
    RoadLayoutOption("side_bottom", "নিচের পাশে রাস্তা", "পার্শ্ব রাস্তা"),
    RoadLayoutOption("side_left", "বাম পাশে রাস্তা", "পার্শ্ব রাস্তা"),
    RoadLayoutOption("side_right", "ডান পাশে রাস্তা", "পার্শ্ব রাস্তা"),
    RoadLayoutOption("t_bottom", "T-আকৃতির রাস্তা (নিচে)", "T-আকৃতি"),
    RoadLayoutOption("t_top", "T-আকৃতির রাস্তা (উপরে)", "T-আকৃতি"),
    RoadLayoutOption("t_right", "T-আকৃতির রাস্তা (ডানে)", "T-আকৃতি"),
    RoadLayoutOption("t_left", "T-আকৃতির রাস্তা (বামে)", "T-আকৃতি"),
    RoadLayoutOption("l_bottom_left", "L-আকৃতির রাস্তা (নিচে-বামে)", "L-আকৃতি"),
    RoadLayoutOption("l_bottom_right", "L-আকৃতির রাস্তা (নিচে-ডানে)", "L-আকৃতি"),
    RoadLayoutOption("l_top_left", "L-আকৃতির রাস্তা (উপরে-বামে)", "L-আকৃতি"),
    RoadLayoutOption("l_top_right", "L-আকৃতির রাস্তা (উপরে-ডানে)", "L-আকৃতি"),
    RoadLayoutOption("both_sides_longitudinal", "দুই পাশে লম্বালম্বি রাস্তা", "দ্বিপার্শ্বিক"),
    RoadLayoutOption("both_sides_transverse", "দুই পাশে আড়াআড়ি রাস্তা", "দ্বিপার্শ্বিক")
)

data class MouzaScalePreset(
    val id: String,
    val labelBn: String,
    val shortLabelBn: String,
    val feetPerInch: Double
)

val mouzaScalePresets = listOf(
    MouzaScalePreset("scale_16", "১৬ ইঞ্চি = ১ মাইল (৩৩০ ফিট/ইঞ্চি)", "১৬ ইঞ্চি", 330.0),
    MouzaScalePreset("scale_32", "৩২ ইঞ্চি = ১ মাইল (১৬৫ ফিট/ইঞ্চি)", "৩২ ইঞ্চি", 165.0),
    MouzaScalePreset("scale_64", "৬৪ ইঞ্চি = ১ মাইল (৮২.৫ ফিট/ইঞ্চি)", "৬৪ ইঞ্চি", 82.5),
    MouzaScalePreset("scale_80", "৮০ ইঞ্চি = ১ মাইল (৬৬ ফিট/ইঞ্চি)", "৮০ ইঞ্চি", 66.0)
)

const val SCAN_DPI_AUTO = 0
const val SCAN_DPI_CUSTOM = -1

data class ScanDpiPreset(
    val dpi: Int,
    val labelBn: String
)

val scanDpiPresets = listOf(
    ScanDpiPreset(SCAN_DPI_AUTO, "স্বয়ংক্রিয় (ফাইল মেটাডেটা থেকে)"),
    ScanDpiPreset(72, "৭২ DPI (স্ক্রিন)"),
    ScanDpiPreset(96, "৯৬ DPI (ওয়েব/স্ক্রিন)"),
    ScanDpiPreset(150, "১৫০ DPI (ড্রাফট স্ক্যান)"),
    ScanDpiPreset(200, "২০০ DPI (মাঝারি স্ক্যান)"),
    ScanDpiPreset(300, "৩০০ DPI (স্ট্যান্ডার্ড স্ক্যান)"),
    ScanDpiPreset(400, "৪০০ DPI (হাই স্ক্যান)"),
    ScanDpiPreset(600, "৬০০ DPI (আল্ট্রা স্ক্যান)"),
    ScanDpiPreset(SCAN_DPI_CUSTOM, "কাস্টম DPI…")
)

enum class PaperSize(val labelBn: String, val widthPt: Float, val heightPt: Float) {
    A4("A4", 595f, 842f),
    A3("A3", 842f, 1191f),
    A2("A2", 1191f, 1684f),
    LEGAL("লিগ্যাল", 612f, 1008f),
    CUSTOM("কাস্টম", 595f, 842f)
}

enum class PrintOrientation(val labelBn: String) {
    PORTRAIT("পোর্ট্রেট"),
    LANDSCAPE("ল্যান্ডস্কেপ")
}

enum class PrintLayoutPreset(val labelBn: String) {
    STANDARD("স্ট্যান্ডার্ড"),
    DEMARCATION_CHITHA("ডিমার্কেশন চিঠা"),
    CLEAN_SURVEY("ক্লিন সার্ভে"),
    SHARE_PACK("শেয়ার প্যাক")
}

enum class PrintLineColorMode(val labelBn: String, val hex: String?) {
    PLOT_OWN("প্লটের নিজ রং", null),
    BLUE("নীল", "#2563EB"),
    RED("লাল", "#DC2626"),
    BLACK("কালো", "#111827"),
    GREEN("সবুজ", "#16A34A"),
    BROWN("বাদামি", "#92400E"),
    ORANGE("কমলা", "#EA580C"),
    GRAY("ধূসর", "#4B5563")
}

enum class ReportSectionId(val labelBn: String) {
    HEADER("হেডার ও পরিচিতি"),
    TITLE("শিরোনাম ও তারিখ"),
    MAP("ম্যাপ ও স্কেল"),
    CERTIFIER("সনাক্তকারী ও স্বাক্ষর"),
    DISCLAIMER("দাবিত্যাগ"),
    AREA_TABLE("পরিমাপ টেবিল"),
    LEGEND("লিজেন্ড"),
    FOOTER("ফুটার")
}

@JsonClass(generateAdapter = true)
data class ReportSectionLayout(
    val order: List<ReportSectionId> = ReportSectionId.entries.toList(),
    val hidden: Set<ReportSectionId> = emptySet(),
    val showLogo: Boolean = true,
    val showInspectorBox: Boolean = true,
    val showQrHeader: Boolean = true,
    val showInset: Boolean = true,
    val showDimensionLabels: Boolean = true,
    val showFrontageDepth: Boolean = true,
    val showTotalRow: Boolean = true
) {
    fun isVisible(id: ReportSectionId): Boolean = id !in hidden

    fun orderedInfoColumns(): List<ReportSectionId> {
        val infoSet = setOf(ReportSectionId.DISCLAIMER, ReportSectionId.AREA_TABLE, ReportSectionId.LEGEND)
        val filtered = order.filter { it in infoSet }
        return if (filtered.isEmpty()) listOf(ReportSectionId.DISCLAIMER, ReportSectionId.AREA_TABLE, ReportSectionId.LEGEND) else filtered
    }
}

@JsonClass(generateAdapter = true)
data class PrintConfig(
    val layoutPreset: PrintLayoutPreset = PrintLayoutPreset.STANDARD,
    val headerTitle: String = "ডিজিটাল ডিমার্কেশন",
    val subtitleNote: String = "",
    val mouzaName: String = "",
    val jlNumber: String = "",
    val showScaleBar: Boolean = true,
    val showDate: Boolean = true,
    val surveyorName: String = "Abu Naser Khan • NaserX Labs",
    val noticeText: String = "ভূমি প্রো (BhumiPro) দ্বারা তৈরি জরিপ চিত্র — সরকারিভাবে সত্যায়িত নথি নয়",
    val paperSize: PaperSize = PaperSize.A4,
    val orientation: PrintOrientation = PrintOrientation.PORTRAIT,
    val marginMm: Float = 12f,
    val lineColorMode: PrintLineColorMode = PrintLineColorMode.PLOT_OWN,
    val labelFontSizeSp: Float = 11f,
    val dimensionFontSizeSp: Float = 10f,
    val dagNumberFontSizeSp: Float = 11f,
    val strokeWidthPx: Float = 3.5f,
    val percentageLineWidthPx: Float = 2f,
    val vertexSizePx: Float = 6f,
    val baseMapOpacity: Float = 0.25f,
    val grayscale: Boolean = false,
    val showHeaderAndInfo: Boolean = true,
    val showFooterAndPage: Boolean = true,
    val showNorthArrow: Boolean = true,
    val showBaseMap: Boolean = false,
    val showEdgeLengths: Boolean = true,
    val showPlotNumbers: Boolean = true,
    val showRoadsLayer: Boolean = true,
    val showColoredFill: Boolean = true,
    val showWatermark: Boolean = false,
    val watermarkText: String = "BHUMIPRO SURVEY",
    val showQrCode: Boolean = true,
    val showBarcode: Boolean = true,
    val showLegend: Boolean = true,
    val showAreaSummary: Boolean = true,
    val signatureUri: String? = null,
    val fontProfile: String = "BENGALI_PRIMARY",
    val sectionLayout: ReportSectionLayout = ReportSectionLayout(),
    val customPaperWidthPt: Float = 0f,
    val customPaperHeightPt: Float = 0f,
    val showPageNumbers: Boolean = true,
    val individualPlotPages: Boolean = false,
    // v1.6 additions
    val mapContentMode: String = "full",
    val showVertexNumbers: Boolean = false,
    val plotFillOpacity: Float = 1f,
    val labelCollisionAvoidance: Boolean = true,
    val showPerimeterLabel: Boolean = true,
    val showTotalAreaLabel: Boolean = true,
    val showSelectedAreaLabel: Boolean = true,
    val scaleBarUnitId: String = "inherit",
    val northArrowSizePercent: Int = 100,
    val areaUnitId: String = AreaDisplayUnit.SHOTOK.id
)

data class PrintJobState(
    val isReady: Boolean = true,
    val blockedReasonBn: String? = null,
    val warningBn: String? = null,
    val isExporting: Boolean = false,
    val exportProgressPage: Int = 0,
    val exportTotalPages: Int = 0,
    val lastExportMessageBn: String? = null
)

data class SurveyProject(
    val id: String = "draft_raujan_4",
    val title: String = "35-PASCHIM RAUJAN-4_result_rev",
    val mouzaName: String = "৩৫-পশ্চিম রাউজান-৪",
    val scale: String = "১৬ ইঞ্চি",
    val scaleId: String = "scale_16",
    val scanDpi: Int = 300,
    val isPdfSource: Boolean = true,
    val sqFeetPerShotok: Double = 435.6,
    val feetPerCanvasUnit: Double = 0.55,
    val labelFontSizeSp: Float = 11f,
    val strokeWidthPx: Float = 4.5f,
    val plots: List<LandPlot> = emptyList(),
    val activePlotIndex: Int = 0,
    val selectedPlotIds: Set<String> = emptySet(),
    val ownerName: String = "",
    val district: String = "চট্টগ্রাম",
    val upazila: String = "রাউজান",
    val jlNumber: String = "৩৫",
    val khatianNo: String = "",
    val dagNo: String = "",
    val surveyDate: String = "",
    val notes: String = "",
    val referenceCode: String = "",
    val calibrationRmsePx: Double = 0.0,
    val isCalibrated: Boolean = false,
    val calibrationMethod: String = "PRESET_SCALE_DPI",
    val calibrationPixelDistance: Double = 0.0,
    val calibrationGroundFeet: Double = 0.0,
    val calibrationUpdatedAt: Long = 0L,
    val layers: List<GisLayerItem> = defaultGisLayers(),
    val canvasSettings: CanvasDisplaySettings = CanvasDisplaySettings(),
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val status: String = "সক্রিয়",
    val updatedAt: Long = 0L
) {
    val activePlot: LandPlot?
        get() = plots.getOrNull(activePlotIndex)

    val totalLandAreaSqFt: Double
        get() = plots.filter { !it.isRoad && !it.isHidden }.sumOf { it.areaSqFt }

    val totalLandAreaShotangsho: Double
        get() = totalLandAreaSqFt / sqFeetPerShotok.coerceAtLeast(1.0)
}

data class ControlPoint(
    val id: String,
    val label: String,
    val sourcePoint: PointF,
    val targetPoint: PointF,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class GeorefQualityReport(
    val pointCount: Int,
    val rmsePx: Double,
    val qualityLabelBn: String
)

enum class ExportFormat(val labelBn: String, val extension: String, val mimeType: String) {
    PDF("PDF সার্ভে রিপোর্ট", "pdf", "application/pdf"),
    PNG("PNG ছবি", "png", "image/png"),
    JPEG("JPEG ছবি", "jpg", "image/jpeg"),
    SVG("SVG ভেক্টর", "svg", "image/svg+xml"),
    GEOJSON("GeoJSON", "geojson", "application/geo+json"),
    KML("KML (Google Earth)", "kml", "application/vnd.google-earth.kml+xml"),
    CSV("CSV পরিমাপ তালিকা", "csv", "text/csv"),
    WKT("WKT জ্যামিতি", "wkt", "text/plain"),
    DXF("DXF ক্যাড", "dxf", "application/dxf")
}
