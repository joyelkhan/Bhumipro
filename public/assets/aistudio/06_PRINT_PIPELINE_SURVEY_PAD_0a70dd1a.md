# 06 — Print Pipeline & Survey Pad PDF (v1.6 full fidelity)

## 1. Data pipeline (single source of truth)
```
Canvas state ─┐
PrintConfig ──┼→ SurveyReportDataMapper.build(project, ticket, …, canvasSettings, layers, printConfig)
ReportTicket ─┘        └→ SurveyReportData { pad: SurveyPadFidelity, plots: ReportPlot[], … }
                            ├→ BengaliSurveyReportRenderer.renderPreviewBitmap(pageLayout)  ← preview tab
                            ├→ ReportPdfExporter.generateSurveyPadPdf(pageLayout, multi-page) ← PDF
                            └→ renderThumbnail() ← template cards
```
`LandSurveyViewModel.buildSurveyReportData(ticket)` is the ONLY builder the UI calls — preview and PDF cannot diverge.

## 2. `SurveyPadFidelity` (Moshi, all defaults) — everything captured
- Units: `unitId`(MeasurementUnit), `decimalPrecision`(0–3), `sqFeetPerShotok`(live factor), `areaUnitId`.
- CAD: measurementColorHex, measurementStrokeScale, fontScale, lineScale, outlineScale, pointScale, showMeasurementsOutside, showSplitTotalInside, mapTextPositionId, zoomTextModeId.
- Furniture: showGrid + gridColorHex + gridSpacingUnits, showCrosshair, showCornerPoints, **showVertexNumbers**, **plotFillOpacity**, **labelCollisionAvoidance**.
- Raster filters: brightness(−0.5..0.5), contrast, grayscale, mapOpacity — applied to the inset via `BitmapMapFilters` (identical math to canvas; passthrough returns SAME instance).
- Content: **mapContentMode** full/selected/crop (crop = paddingFraction 0.02 vs 0.08).
- Summaries: **showPerimeterLabel / showTotalAreaLabel / showSelectedAreaLabel / scaleBarUnitId / northArrowSizePercent / strokeColorHexOverride** (from PrintConfig.lineColorMode.hex).
- Layers: `List<ReportLayerState>` (id/nameBengali/visible/opacity/locked/zOrder) — renderer hides plots/roads/measure/labels/grid/base per visibility and multiplies alpha by opacity; `layerVisible/layerOpacity` default visible for unknown ids.
- `fromCanvas(canvas, layers, sqFeetPerShotok, showGridOverride?)` captures the live canvas in one call.

## 3. Mapper rules (`SurveyReportDataMapper`)
1. Selection: `includePlotIds` explicit only; ∅ ⇒ placeholder page (`"রিপোর্টের জন্য প্লট নির্বাচন করুন"` + hint `"ম্যাপে প্লটে ট্যাপ করে রিপোর্টে যুক্ত করুন"`). `mapContentMode=selected` narrows to `activePlotIndex`.
2. Dimension labels: ft'ins → stored `segments[i].formattedLength` verbatim (canvas parity, test-locked); other units → `LengthFormatter.format(edgeFeet, unit, precision)` live.
3. Area: ALWAYS from raw `areaSqFt` ÷ CURRENT factor (`formatAreaValue`) — wording `"X শতক"` (canvas parity; v1.5 said শতাংশ — fixed).
4. Per-plot: `perimeterLabel` (unit-aware), `dagNumber`, `labelStyle = ReportLabelStyle.from(plot.labelStyle)` (every field).
5. Totals: totalAreaSqFt, perimeterLabel (sum of land plots), `totalAreaLabel = "মোটঃ X"` unit-aware.

## 4. Renderer (`BengaliSurveyReportRenderer.drawReport`) — v1.6 consumption
- Params: canvas, pageW, pageH, data, template, typeface, qr/logo/signature/inset bitmaps, sections, **pageLayout: PageLayout?** (margin `m = pageLayout.marginPt ?: 14f`).
- Palette per template (STANDARD/DIMENSION/ROYAL/LAND_MEASUREMENT/CUSTOM) unchanged — geometry/labels invariant (test-locked).
- Map area: grid → plots (roads first) → crosshair → central label → inset.
- Per-plot paint: color = labelStyle.colorHex ?: measurementColorHex; textSize = 7.5 × fontScale × labelStyle.fontSizeScale; bold→isFakeBoldText; italic→Typeface.create(typeface, ITALIC); underline→isUnderlineText; letterSpacing = sp/size em; alpha = 255 × labelStyle.opacity × layer opacity.
- Placement: outward normal offset `13 × fontScale × outwardSign`; outwardSign: −1 if !showMeasurementsOutside; −1 if BELOW; −0.35 if INSIDE; +1 ABOVE. `manuallyPositioned` adds `offsetX×bboxW / offsetY×bboxH`. Collision avoidance: single-pass nudge along outward normal (shift += labelH+1.5 on intersect). Background pill when backgroundHex set (rounded rect, backgroundOpacity).
- Vertices: radius `3 × pointScale`; hidden when !showCornerPoints; Bengali index numbers when showVertexNumbers.
- Boundary: white CAD outline beneath colored stroke (outlineScale), strokeColor = strokeColorHexOverride ?: palette.
- Fill alpha = plotFillOpacity × layer opacity (scaledAlpha helper).
- Central label: gated by layer_labels.visible && showSelectedAreaLabel; size scales with fontScale × labelStyle.fontSizeScale (0.6–2.4 clamp); line1 `"name (দাগ X)"`; line2 = plot.areaLabel.
- Inset: `BitmapMapFilters.apply(inset, brightness, contrast, grayscale, mapOpacity × layer_base opacity) ?: inset`.
- Certifier row: surveyor line + `"পরিধিঃ <perimeterLabel>"` (gated) + scale text `"ক্ষেত্র ১ ইঞ্চি = X ফিট"` (from shared transform — real calibration math) + alternating scale bar 66pt, label = LengthFormatter.format(feetPerInch, barUnit, precision) where barUnit = scaleBarUnitId override or pad.unit.
- Area table: total row gated by `sections.showTotalRow && pad.showTotalAreaLabel`; rows = land plots (take 2) with mbb frontage/depth line when enabled.
- Title block compass: radius × northArrowSizePercent/100 (0.6–1.6).
- Preview bitmap: `renderPreviewBitmap(..., pageLayout)` — same geometry as PDF page.

## 5. Print customization state (ViewModel)
- `updatePrintConfigUndoable(transform)` — pushes to printUndoStack (max 50), clears redo. `undoPrintChange`/`redoPrintChange`; `canUndoPrint`/`canRedoPrint`.
- `resetPrintFidelityToCanvas()` — "ক্যানভাস অবস্থায় রিসেট": print-only knobs → defaults (mapContentMode full, vertexNumbers off, fillOpacity 1, collision on, all summary toggles on, scaleBar inherit, northArrow 100%, lineColorMode PLOT_OWN).
- `applyPrintStylesToCanvas()` — pushes print choices with canvas equivalents into `updateCanvasSettings`: grayscale, mapOpacity=baseMapOpacity, measurementColorHex=lineColorMode.hex (display-only).
- `PrintJobState` validation (combine uiState+printConfig): blocked when `feetPerCanvasUnit<=0 && !isCalibrated` → `"ম্যাপ ক্যালিব্রেট করা হয়নি — …"`; warning when selection empty → `"কোনো প্লট নির্বাচন করা হয়নি — প্রিভিউতে নির্দেশক পাতা দেখাবে"`. `observePrintJobState()` pumps into `_printJobState`.
- Presets (Room v7): `savePrintPreset(name)` (toast `"প্রিসেট সংরক্ষিত: X"`), `applyPrintPreset(id)` (Moshi decode; re-binds mouzaName/jlNumber to live project; undoable; toast `"প্রিসেট প্রয়োগ হয়েছে: X"`), `deletePrintPreset(id)`, `printPresets: StateFlow<List<PrintPresetEntity>>`.

## 6. PDF engine (`ReportPdfExporter`)
- `PageLayout.from(config)`: paper pt sizes (A4 595×842, A3 842×1191, A2 1191×1684, Legal 612×1008, custom W/H), landscape swaps if w<h, margin = clamp(4..30 mm) × 72/25.4.
- `PrintPageMath.buildFileName(mapName, title, date)` → `[MapName]_[Title]_[Date]` Bengali-safe (space/forbidden → `_`, blank → Mouza_Map/Survey/Report, 48-char cap per part). `pageNumberLabel(i,n)` = `"পৃষ্ঠা ২/৩"` Bengali digits; empty for single page.
- Page plan: `buildPagePlan(data, individualPlotPages)` = [FullSheet] + per-land-plot SinglePlot (tight bounds, roads skipped).
- Loop per page: cancel check → progress(page, total) → startPage → drawReport(pageLayout) → Bengali page-number footer (multi-page only) → finishPage. Cancel → delete partial + `CANCELLED_MESSAGE_BN = "রপ্তানি বাতিল করা হয়েছে"`.
- Failure: `mapExportError(t)` bilingual (OOM/IO/Security/IllegalState mappings) + raster fallback (SAME renderer 2× JPEG into minimal PDF container) — logs `"vector PDF path failed"`.
- `saveSurveyPadPdfToDownloads(...)` → generate + copy to MediaStore Downloads with `[MapName]_[Title]_[Date].pdf`; legacy `saveToDownloads/sharePdf/printReport` retained.
- ViewModel export: `exportSurveyPadPdf(context, ticket)` — builds model → IO typeface/QR/signature decode → exporter with progress→`printJobState.exportProgressPage/TotalPages`, cancel via AtomicBoolean → toasts `"সার্ভে প্যাড PDF ডাউনলোডে সেভ হয়েছে"` / cancel / error.

## 7. UI wiring (`ui/screens/print/PrintPreviewScreen.kt` + `PrintSurveyPadCustomize.kt`)
- Top bar actions: আনডু/রিডু (enabled by stacks), ক্যানভাস অবস্থায় রিসেট (RestartAlt), প্রিন্ট স্টাইল ক্যানভাসে প্রয়োগ (PushPin), টিকেট, রিজেনারেট.
- `TabRow`: **প্রিভিউ** | **কাস্টমাইজ**.
- Preview tab: zoomable page bitmap (pinch 1–5×, double-tap, pan, reset), plot-selection chips row (`togglePlotReportSelection`), template cards (selectTemplate — persisted), quick fields (শিরোনাম, সার্ভেয়ার), export center link, bottom bar: **PDF ডাউনলোড** (exports via ViewModel) / শেয়ার / প্রিন্ট.
- Customize tab (`PrintSurveyPadCustomizePanel`) — 7 expandable sections, every control live:
  1. কাগজ ও লেআউট — paper chips (A4/A3/A2/লিগ্যাল/কাস্টম), orientation chips, custom W/H fields (only when CUSTOM), margin field, পৃষ্ঠা নম্বর toggle, প্রতি প্লট আলাদা পাতা toggle.
  2. ম্যাপ কন্টেন্ট ও প্লট — content mode chips (পূর্ণ ম্যাপ/সক্রিয় প্লট/কনটেন্টে ক্রপ), ভারটেক্স নম্বর toggle, ফিল অস্বচ্ছতা slider 0–100%, লেবেল সংঘর্ষ এড়ানো toggle, কর্নার পয়েন্ট toggle (canvas).
  3. একক ও লেবেল — 7 unit chips (2 rows), দশমিক slider 0–3, ক্ষেত্রফল একক chips (শতক/বর্গফুট/বর্গমিটার/একর/হেক্টর), পরিধি/মোট সারি/নির্বাচিত লেবেল toggles, ডাইমেনশন লেবেল master, স্কেল বার একক chips (ক্যানভাস + 5 units).
  4. CAD স্টাইল (ক্যানভাস লাইভ) — font/line/outline/point/measurement-stroke sliders (each % readout), মাপের লেখার অবস্থান chips, বাইরে/ভিতরে toggles, গ্রিড toggle (layer_grid) + spacing slider, ক্রসহেয়ার, ব্রাইটনেস/কনট্রাস্ট sliders (−0.5..0.5), গ্রেস্কেল, ম্যাপ অস্বচ্ছতা.
  5. অফিসিয়াল উপাদান — নর্থ অ্যারো সাইজ 60–160%, ওয়াটারমার্ক, QR, বারকোড, লিজেন্ড, ক্ষেত্রফল সারসংক্ষেপ, হেডার, ফুটার toggles.
  6. রিপোর্ট সেকশন ও সীমানা রং — reuses PrintCustomizePanel (8 sections toggle+reorder, paper/orientation, margin, 8 color modes, গ্লোবাল/প্রজেক্ট সেভ).
  7. প্রিন্ট প্রিসেট — list (tap=apply, trash=delete) + new-preset name field + save.
- Export progress bar with `"PDF তৈরি হচ্ছে — পৃষ্ঠা ১/২"` + বাতিল; blocked/warning banners (InfoBanner).

## 8. Fidelity checklist (element → where honored)
raster map filters ✓(inset) · plot geometry ✓ · per-plot label styles (size/bold/italic/underline/color/bg/opacity/rotation/offset/letterSpacing/lock) ✓ · edge dimensions (unit+precision+canvas text) ✓ · area labels (live factor) ✓ · perimeter ✓ · selection ✓ · content modes ✓ · layer visibility/opacity ✓ · grid+color+spacing ✓ · crosshair ✓ · corner points+vertex numbers ✓ · CAD scales ✓ · text position/outside ✓ · collision avoidance ✓ · scale text+bar+unit ✓ · north arrow size ✓ · paper/orientation/margin/custom ✓ · multi-page+page numbers ✓ · title/ticket/footer/QR/logo/signature/watermark/legend/area table ✓ · stamp boxes (inspector/certifier) ✓ · file naming ✓ · progress/cancel ✓ · bilingual errors ✓.
