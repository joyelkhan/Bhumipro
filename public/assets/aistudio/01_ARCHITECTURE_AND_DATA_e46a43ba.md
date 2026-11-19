# 01 — Architecture & Data (every field, no omission)

## 1. Package layout

```
com.example
├── MainActivity.kt                    # single Activity, NavHost, all routes
├── data/
│   ├── local/
│   │   ├── Entities.kt                # 9 Room entities (see §3)
│   │   ├── LandProDao.kt              # 9 @Dao interfaces
│   │   ├── LandProDatabase.kt         # @Database(version=7), MIGRATION_2_3…6_7, SeedCallback
│   │   ├── PrintConfigStore.kt        # DataStore "print_config_prefs" — THE print persistence
│   │   └── SettingsDataStore.kt       # app-wide prefs (language/theme/shotok default…)
│   ├── model/
│   │   ├── LandModels.kt              # PointF, BoundarySegment, LandPlot, GisLayerItem,
│   │   │                              #   SurveyProject, PrintConfig, enums, presets
│   │   ├── CanvasModels.kt            # PlotLabelStyle, MeasurementUnit, MapTextPosition,
│   │   │                              #   ZoomTextMode, CanvasDisplaySettings, LengthFormatter,
│   │   │                              #   AreaDisplayUnit, UnitSanity
│   │   ├── SettingsModels.kt / SettingsCodec.kt / MouzaLibraryModels.kt
│   │   └── LandProRepository.kt       # entity ⇄ domain mapping (repository/)
│   └── repository/LandProRepository.kt
├── domain/
│   ├── GeometryUtils.kt               # area/perimeter/segments/shotok/MBB/buildPlot/recalculatePlot
│   ├── MeasurementEngine.kt           # PlotGeometryMetrics
│   ├── MapTransformMath.kt            # canvas view transform (zoom/pan/fit)
│   ├── JtsDivisionEngine.kt           # JTS-based division + validation
│   ├── SpatialIndex.kt                # STRtree hit-testing (2000+ plots)
│   ├── AnaGondaHelper.kt              # আনা-গন্ডা conversions
│   ├── BangladeshiUnitConverter.kt    # unit tables
│   ├── PdfAndPrintHelper.kt           # typeface resolve, bitmap decode, share, print
│   ├── ExportCenterHelper.kt          # PDF/PNG/JPEG/SVG/GeoJSON/KML/CSV/WKT/DXF export
│   ├── TiledBitmapManager.kt          # large-raster tiles
│   ├── GeoreferencingHelper.kt / ImportMetadataReader.kt / BarcodeAndQrHelper.kt / CadMapHelper.kt
│   └── report/
│       ├── ReportModels.kt            # ReportTemplate, ReportTicket, ReportPlot, SurveyReportData,
│       │                              #   ReportBounds, ReportMapTransform, SurveyReportDataMapper
│       ├── SurveyPadModel.kt          # v1.6: SurveyPadFidelity, ReportLayerState, ReportLabelStyle,
│       │                              #   PageLayout, PrintPageMath, BitmapMapFilters
│       ├── BengaliSurveyReportRenderer.kt   # THE single renderer
│       └── ReportPdfExporter.kt       # page plan, vector PDF, raster fallback, Downloads
├── ui/
│   ├── components/InteractiveMapCanvas.kt   # hot path (≈1150+ lines)
│   │   ├── WorkspaceBarsAndSheets.kt / WorkspaceV14Sheets.kt / MapModals.kt /
│   │   │   DivisionModal.kt / PostDivisionBottomSheet.kt / SaveSurveyDialog.kt /
│   │   │   GeorefOverlayPanel.kt / print/PrintDefaultsComponents.kt / print/PrintCustomizeComponents.kt
│   ├── screens/ MapWorkspaceScreen.kt, MouzaMapLibraryScreen.kt, MeasurementToolsScreens.kt,
│   │   ToolDashboardScreen.kt, SecondaryTabsScreens.kt, DigitalCompassScreen.kt,
│   │   ├── PrintPreviewScreen.kt (Export Center), print/PrintPreviewScreen.kt (Survey Pad preview),
│   │   └── print/PrintSurveyPadCustomize.kt (v1.6 customize tab), print/PrintTicketScreen.kt
│   ├── viewmodel/ LandSurveyViewModel.kt, PrintTicketViewModel.kt, DashboardViewModel.kt, SettingsViewModel.kt
│   ├── common/UiMessage.kt            # UiMessageHost — one-shot message channel
│   ├── settings/ (AdvancedSettingsScreen, SettingsRegistry, BpStrings, FeedbackController…)
│   └── theme/ Theme.kt, Color.kt, Type.kt, BhumiProPrintColors.kt
```

## 2. Domain models — EVERY field

### 2.1 `PointF` (Moshi)
`x: Float`, `y: Float` — canvas/pixel coordinates of the raster map space.

### 2.2 `BoundarySegment` (Moshi)
`startLabel: String`, `endLabel: String`, `lengthFeet: Double`, `formattedLength: String` (e.g. `"৫৫' ০\""` — canvas ground truth), `bearingDegrees: Double = 0.0`

### 2.3 `LandPlot` (Moshi) — the survey unit
| Field | Type | Default | Notes |
|---|---|---|---|
| id | String | — | `"plot_<6hex>"` |
| name | String | — | e.g. `"প্লট ১"` |
| colorHex | String | — | pill/polygon color |
| points | List\<PointF\> | — | polygon ring (canvas units) |
| areaShotangsho | Double | — | stored শতক (display; paper recomputes live) |
| areaSqFt | Double | — | **raw truth** used for live recompute |
| perimeterFeet | Double | — | |
| segments | List\<BoundarySegment\> | — | size MUST equal points.size for label reuse |
| isRoad | Boolean | false | road strips excluded from area table |
| isHidden | Boolean | false | |
| isHighlighted | Boolean | false | |
| annotationText | String | "" | |
| ownerName | String | "" | selection-by-owner |
| dagNumber | String | "" | দাগ নম্বর; selection-by-dag; printed as `(দাগ X)` |
| khatianRef | String | "" | |
| notes | String | "" | |
| isLocked | Boolean | false | |
| status | String | "সক্রিয়" | |
| createdAt / updatedAt | Long | 0L | |
| labelStyle | PlotLabelStyle? | null | v1.4; null = project defaults |

### 2.4 `PlotLabelStyle` (Moshi) — per-plot label customization (v1.4)
`fontSizeScale: Float = 1.0` (0.4–4.0) · `bold: Boolean = true` · `italic = false` · `underline = false` ·
`colorHex: String? = null` (null = plot color) · `backgroundHex: String? = null` · `backgroundOpacity: Float = 0.88` (0–1) ·
`opacity: Float = 1.0` (0.05–1) · `letterSpacingSp: Float = 0f` (may be negative) · `rotationDeg: Float = 0f` (±180) ·
`offsetX/offsetY: Float = 0f` (−1.5..1.5, fractions of plot bbox) · `manuallyPositioned: Boolean = false` (stops auto-centering) ·
`locked: Boolean = false`
Effective getters clamp ranges; `offsetClamped(v)` helper.

### 2.5 `MeasurementUnit` enum — 7 ground-length units
| id | বাংলা | English | Basis |
|---|---|---|---|
| ft_in | ফুট-ইঞ্চি | Feet + Inches | **default**; round(totalInches) split at 12 |
| ft | ফুট | Feet | `x′` |
| m | মিটার | Meters | ft ÷ 3.280839895 |
| cm | সেন্টিমিটার | Centimeters | ft ÷ 3.280839895 × 100 |
| link | লিঙ্ক | Links | ft ÷ 0.66 (Gunter) |
| chain | চেইন | Chains | ft ÷ 66.0 |
| hat | হাত | Hats | ft ÷ 1.5 |
Sanity: 1 chain = 100 links; 1 m ↔ ft inverse (UnitSanity).

### 2.6 `MapTextPosition` / `ZoomTextMode`
- MapTextPosition: `above`(লাইনের উপরে (প্রস্তাবিত), default) · `below`(লাইনের নিচে) · `inside`(ভিতরে)
- ZoomTextMode: `auto`(অটো (প্রস্তাবিত), default) · `always`(সর্বদা) · `hidden`(লুকান)

### 2.7 `CanvasDisplaySettings` (Moshi; Room v5 `canvasSettingsJson`) — EVERY field
| Field | Type | Default | Range |
|---|---|---|---|
| measurementUnit | String | "ft_in" | unit id |
| decimalPrecision | Int | 2 | 0..3 |
| measurementColorHex | String | "#133C2D" | |
| measurementStrokeScale | Float | 1.0 | 0.4..3 |
| gridColorHex | String | "#133C2D" | |
| gridSpacingUnits | Float | 40 | 8..160 |
| showCrosshair | Bool | false | |
| showCornerPoints | Bool | true | |
| brightness | Float | 0 | −0.5..0.5 |
| contrast | Float | 0 | −0.5..0.5 |
| grayscale | Bool | false | |
| mapOpacity | Float | 1 | 0.05..1 |
| fontScale | Float | 1.0 | 0.4..3 (v1.5 CAD) |
| lineScale | Float | 1.0 | 0.4..3 |
| outlineScale | Float | 0.3 | 0.1..2 (white CAD outline) |
| pointScale | Float | 1.0 | 0.4..3 |
| showMeasurementsOutside | Bool | true | ম্যাপের লাইন দেখান |
| showSplitTotalInside | Bool | false | বিভক্ত বাহুর মোট মাপ |
| mapTextPosition | String | "above" | |
| zoomTextMode | String | "auto" | |
| zoomPresetPercent | Int | 100 | 15..1600 preset chip memory |

CAD presets (`presetIdOrNull`): small = font 0.6/line 0.8/outline 0.3/point 1.0 → "ছোট"; normal = 0.8/1.0/0.3/1.0 → "স্বাভাবিক"; medium = 1.2/1.0/0.3/1.0 → "মাঝারি".

### 2.8 `GisLayerItem` + `defaultGisLayers()` — 8 layers, zOrder fixed
layer_base(মূল মৌজা ম্যাপ, visible, opacity .9, z0) · layer_grid(সার্ভে গ্রিড, **hidden**, .35, z1) · layer_cad(ক্যাড/সাবেক ওভারলে, visible, .75, z2) · layer_plots(প্লট পলিগন, 1.0, z3) · layer_roads(রাস্তা, z4) · layer_measure(দৈর্ঘ্য ও পরিমাপ, z5) · layer_labels(প্লট ও দাগ লেবেল, z6) · layer_notes(টীকা ও নোট, z7). Fields: id/type(GisLayerType)/nameBengali/visible/locked/opacity(0.05–1)/colorHex/zOrder.

### 2.9 `SurveyProject` — the workspace state
id `"draft_raujan_4"` default · title `"35-PASCHIM RAUJAN-4_result_rev"` · mouzaName `"৩৫-পশ্চিম রাউজান-৪"` · scale `"১৬ ইঞ্চি"` · scaleId `"scale_16"` · scanDpi 300 · isPdfSource true · **sqFeetPerShotok 435.6** · feetPerCanvasUnit 0.55 · labelFontSizeSp 11f · strokeWidthPx 4.5f · plots · activePlotIndex 0 · **selectedPlotIds: Set\<String\> = empty()** (v1.2 rule: empty ⇒ placeholder report, NEVER auto-select) · ownerName · district "চট্টগ্রাম" · upazila "রাউজান" · jlNumber "৩৫" · khatianNo · dagNo · surveyDate · notes · referenceCode · calibrationRmsePx 0.0 · isCalibrated false · calibrationMethod "PRESET_SCALE_DPI" · calibrationPixelDistance/…GroundFeet 0.0 · calibrationUpdatedAt 0L · layers = defaultGisLayers() · canvasSettings = CanvasDisplaySettings()

### 2.10 Scale & DPI presets
`mouzaScalePresets`: scale_16 = ১৬ ইঞ্চি = ১ মাইল (৩৩০ ফিট/ইঞ্চি) · scale_32 = ৩২ ইঞ্চি = ১ মাইল (১৬৫) · scale_64 = ৬৪ (৮২.৫) · scale_80 = ৮০ (৬৬)।
`SCAN_DPI_AUTO = 0` (read JFIF/pHYs metadata, fallback 300) · `SCAN_DPI_CUSTOM = −1`; scanDpiPresets: auto, 72, 96, 150, 200, **300 (স্ট্যান্ডার্ড স্ক্যান)**, 400, 600, custom. Range for custom: 50..2400.

### 2.11 `PrintConfig` (Moshi) — 46 fields (37 pre-v1.6 + 9 v1.6)
v1.1 core: layoutPreset(STANDARD/DEMARCATION_CHITHA/CLEAN_SURVEY/SHARE_PACK, লেবেল: স্ট্যান্ডার্ড/ডিমার্কেশন চিঠা/ক্লিন সার্ভে/শেয়ার প্যাক) · headerTitle "ডিজিটাল ডিমার্কেশন" · subtitleNote · mouzaName · jlNumber · showScaleBar · showDate · surveyorName "Abu Naser Khan • NaserX Labs" · noticeText "ভূমি প্রো (BhumiPro) দ্বারা তৈরি জরিপ চিত্র — সরকারিভাবে সত্যায়িত নথি নয়" · paperSize(A4 595×842 / A3 842×1191 / A2 1191×1684 / লিগ্যাল 612×1008 / কাস্টম) · orientation(পোর্ট্রেট/ল্যান্ডস্কেপ) · marginMm 12f · lineColorMode(PLOT_OWN প্লটের নিজ রং + নীল #2563EB/লাল #DC2626/কালো #111827/সবুজ #16A34A/বাদামি #92400E/কমলা #EA580C/ধূসর #4B5563) · labelFontSizeSp 11 · dimensionFontSizeSp 10 · dagNumberFontSizeSp 11 · strokeWidthPx 3.5 · percentageLineWidthPx 2 · vertexSizePx 6 · baseMapOpacity .25 · grayscale · showHeaderAndInfo · showFooterAndPage · showNorthArrow · showBaseMap false · showEdgeLengths · showPlotNumbers · showRoadsLayer · showColoredFill · showWatermark + watermarkText "BHUMIPRO SURVEY" · showQrCode · showBarcode · showLegend · showAreaSummary · signatureUri? · fontProfile "BENGALI_PRIMARY" · sectionLayout (see below) · customPaperWidthPt/customPaperHeightPt 0 · showPageNumbers true · individualPlotPages false.
**v1.6 additions**: mapContentMode "full"(full/selected/crop) · showVertexNumbers false · plotFillOpacity 1f · labelCollisionAvoidance true · showPerimeterLabel true · showTotalAreaLabel true · showSelectedAreaLabel true · scaleBarUnitId "inherit" · northArrowSizePercent 100 (60–160) · areaUnitId "shotok".

### 2.12 `ReportSectionLayout` (persisted inside PrintConfig)
order = all 8 ReportSectionId ids (header/title/map/certifier/disclaimer/area_table/legend/footer, বাংলা লেবেল সহ) · hidden = emptySet · showLogo/showInspectorBox/showQrHeader/showInset/showDimensionLabels/showFrontageDepth/showTotalRow = true. `orderedInfoColumns()` returns disclaimer/area_table/legend in user order.

## 3. Room schema v7 — full DDL (DB `landpro_surveying.db`)

Entities: `draft_projects`, `pinned_tools`, `imported_assets` (FK→draft_projects CASCADE), `project_snapshots` (FK, index projectId+createdAt), `boundary_documents` (FK), `conversion_history`, `export_records` (index projectId+createdAt), `mouza_library_items` (index divisionId+districtId), **`print_presets` (v7)**.

`draft_projects` columns (every one): id PK/title/mouzaName/scaleText/plotsJson/activePlotIndex/totalAreaShotangsho/updatedAt + v3: scaleId("scale_16"), scanDpi(300), feetPerCanvasUnit(0.55), sqFeetPerShotok(435.6), ownerName, district, upazila, jlNumber, khatianNo, dagNo, surveyDate, notes, referenceCode, calibrationRmsePx(0.0), isCalibrated(0), calibrationMethod(''), calibrationPixelDistance(0.0), calibrationGroundFeet(0.0), calibrationUpdatedAt(0) + layersJson("[]") + isFavorite/isPinned/isArchived/isDeleted(0), deletedAt(0), status('সক্রিয়') + **v5**: selectedPlotIdsJson(''), canvasSettingsJson('').

`print_presets` (v1.6/v7): `id TEXT PK`, `name TEXT NOT NULL`, `configJson TEXT NOT NULL` (whole PrintConfig JSON), `createdAt INTEGER NOT NULL DEFAULT 0`, `updatedAt INTEGER NOT NULL DEFAULT 0`.

**Migrations (all additive, lossless)**:
- 2→3: 15 ALTER TABLE draft_projects ADD COLUMN (defaults above)
- 3→4: calibration provenance 5 columns
- 4→5: selectedPlotIdsJson, canvasSettingsJson
- 5→6: CREATE TABLE mouza_library_items + index
- **6→7: CREATE TABLE print_presets** (SQL in §3 above)

DAO surfaces (methods to implement): DraftProjectDao (observeAll/list/getById/upsert/markDeleted/purge…), PinnedToolDao, ImportedAssetDao, ProjectSnapshotDao, BoundaryDocumentDao, ConversionHistoryDao, ExportRecordDao, MouzaLibraryDao (observeAll/getByKey/observeFavorites/upsert/setFavorite/setOfflineSaved/touchOpened/offlineSavedCount), **PrintPresetDao (v1.6)**: observeAll/`getAll`/getById/upsert(REPLACE)/deleteById/count.

## 4. DataStore keys
- `print_config_prefs`: `config_global` (PrintConfig JSON) · `config_project_<projectId>` (per-project override). `loadEffective = project ?: global`.
- `print_ticket_prefs` (PrintTicketViewModel): template_id, company_name, ticket_title("ডিজিটাল ডিমার্কেশন"), footer_line, surveyor_name, surveyor_mobile, logo_uri, designation, contact_lines, survey_type, upazila_field, report_name, qr_link, website.
- `SettingsDataStore`: language(বাংলা/English), themeMode(লাইট/ডার্ক/সিস্টেম), appDefaultShotok, dashboard template, pinned tools.

## 5. `LandSurveyViewModel` StateFlow inventory (exhaustive)
uiState(SurveyProject) · preferences · workspaceMode(MOUZA_SURVEY default; MAP_JOINT/COMPARATIVE_PANTOGRAPH/ALL_MOUZA_MAPS/CAD_DWG/SATELLITE_MOUZA exist in enum but THIS feature uses MOUZA_SURVEY) · activeTool("select") · selectedVertexIndex · canUndo/canRedo · dialog flags: showDivisionModal/showScaleModal/showChooseSourceModal/showRenameDialog/showProjectMetadataDialog/showTextLineSizeModal/showShotokStandardModal/showAnnotationDialog/showLayersSheet/showSnapshotsSheet · isCalibrating · calibrationPoints · showCalibrationDistanceDialog · division: divisionType(EQUAL/PERCENTAGE/CUSTOM_AREA)/divisionPartsCount(4)/selectedRoadOption("middle_horizontal")/roadWidthFeet(6f)/divisionPercentages[25×4]/customTargetShotoks/divisionSweepAngleDeg/useCustomRoadCenterline/previewDividedPlots/divisionReport · **print (v1.6)**: printConfig · canUndoPrint/canRedoPrint · printJobState(PrintJobState: isReady/blockedReasonBn/warningBn/isExporting/exportProgressPage/exportTotalPages/lastExportMessageBn) · printPresets(List\<PrintPresetEntity\>) · pdfBitmap(Bitmap?) · statusMessage(UiMessageHost).

## 6. Persistence contract
- Every plot edit → `pushUndoSnapshot()` (UndoSnapshot = plots + activePlotIndex + layers) → undo/redo stacks in VM.
- Save: `saveSurveyProjectToRoom()` serializes plots (Moshi, incl. labelStyle inside plotsJson), canvasSettings, selectedPlotIds into draft_projects.
- Recalculate on calibration change: `GeometryUtils.recalculatePlot(plot, newFeetPerUnit, newSqFeetPerShotok)` — the ONLY mass geometry rewrite path; shotsok change also recomputes every plot area (updateShotokStandard).
- Settings reset must never touch draft_projects (SettingsResetRoomSafetyTest).
