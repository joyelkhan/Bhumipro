# 02 — Project Lifecycle: import → calibrate → save (every dialog & validation)

## 1. Workspace bootstrap
`initWorkspace(projectId, WorkspaceMode.MOUZA_SURVEY)`:
1. Sets `_workspaceMode`; if `preserveSourceOnNextInit` flag set, skips reset (used when navigating from Map Library).
2. Loads project: Room `draft_projects` → `LandProRepository` → SurveyProject (plots via Moshi decode of `plotsJson`; unknown JSON names skipped for backward compatibility).
3. Loads print config: `printConfigStore.loadEffective(projectId)` → `_printConfig` (project override → global default → `PrintConfig()`), then `.copy(mouzaName = state.mouzaName, jlNumber = state.jlNumber)`.
4. Loads base map raster through `TiledBitmapManager` → `_pdfBitmap` (this bitmap is the print inset; filters are applied at RENDER time, never baked).
5. `workspaceReady` StateFlow flips true (tests await this).
6. `markPreserveSourceOnNextInit()` — called by Map Library before navigation.

Default seeded project: `draft_raujan_4` (title 35-PASCHIM RAUJAN-4_result_rev, mouza ৩৫-পশ্চিম রাউজান-৪, scaleId scale_16, dpi 300, feetPerCanvasUnit 0.55, shotok 435.6). **The workspace starts with ZERO plots and ZERO selected plots.**

## 2. Source modal (choose map source) — `openChooseSourceModal()/close…`
Options (function → behavior):
- `importDocumentOrImageUri(uri)` — gallery/PDF: copy into project assets, decode raster (PDF page selectable via `selectPdfPage(pageIndex)` when multi-page), record `ImportedAssetEntity(assetType = RASTER_MAP)`.
- `importCadUri(uri)` + `loadSampleDxfCadLayer()` — CAD/DXF overlay into `layer_cad` (`CadMapHelper`); overlay opacity `setGeorefOpacity(f)`.
- Import metadata: `ImportMetadataReader` reads embedded DPI (JFIF/pHYs) → auto scanDpi when `scanDpi == SCAN_DPI_AUTO`.

## 3. Scale calibration — মৌজার স্কেল modal (`openScaleModal/closeScaleModal`)
Three methods (mutually exclusive, all persisted):

### 3.1 Preset scale + DPI (default method `PRESET_SCALE_DPI`)
- Pick one of the 4 `mouzaScalePresets` (scale_16 default) → `feetPerCanvasUnit = feetPerInch / scanDpi` (e.g. 330/300 = **0.55 ft/px at 300 DPI**).
- DPI from `scanDpiPresets` incl. AUTO (metadata) and CUSTOM (`SCAN_DPI_CUSTOM`, valid 50..2400).
- Apply: `saveScaleSettings(scaleId, scanDpi, customDpi?)` → recalculates every plot: `GeometryUtils.recalculatePlot(plot, newFeetPerUnit, sqFeetPerShotok)`; status: `"ক্যালিব্রেশন সফল: …"`.

### 3.2 Two-point tape calibration (`startCalibrationMode()`)
- `isCalibrating = true`; user taps 2 points → `addCalibrationTapPoint(PointF)` collects into `_calibrationPoints` (max 2).
- `showCalibrationDistanceDialog` opens: user enters known ground distance in feet.
- `applyCalibrationDistance(knownFeet)` → `calibrationPixelDistance = px distance`, `feetPerCanvasUnit = knownFeet / pxDistance`, `calibrationMethod = "TWO_POINT"`, `isCalibrated = true`, `calibrationUpdatedAt = now`, RMSE = 0 for 2 points. `cancelCalibrationMode()` aborts without change.
- Georeference overlay panel (`GeorefOverlayPanel`) adds control points: `addControlPoint`, `updateControlPointOffset`, quality report via `GeoreferencingHelper` (`GeorefQualityReport`).

### 3.3 Custom scale text — `838`: `"কাস্টম X ফুট/ইঞ্চি"` when user enters custom feet-per-inch.

Validation gates: print/export blocked (`PrintJobState.blockedReasonBn = "ম্যাপ ক্যালিব্রেট করা হয়নি — প্রিন্টের আগে স্কেল/DPI ক্যালিব্রেশন সম্পন্ন করুন"`) only when `feetPerCanvasUnit <= 0` AND `!isCalibrated`.

## 4. শতক standard dialog (`openShotokStandardModal/close…/updateShotokStandard`)
- Input: sq.ft per শতক. `updateShotokStandard(sqFeetPerShotok, alsoSaveAsAppDefault)`:
  - clamp 100..2000
  - recompute EVERY plot: area = areaSqFt ÷ factor (`recalculatePlot`)
  - persist to project (and optionally to app default via SettingsDataStore)
  - toast: with default → `"১ শতক = X বর্গফুট প্রয়োগ ও ডিফল্ট হিসেবে সংরক্ষিত"`; else `"১ শতক = X বর্গফুট প্রয়োগ হয়েছে"`
- Preset buttons: **435.6 (জাতীয়)** · 432 · 430 · কাস্টম স্টেপার।
- Print-side display: `SurveyReportDataMapper.formatAreaValue` recomputes from raw areaSqFt with the CURRENT factor — stored শতক can never drift from the factor (fidelity rule).

## 5. Project metadata dialog (`openProjectMetadataDialog/close…/updateProjectMetadata`)
Editable fields: title, mouzaName, district, upazila, jlNumber, khatianNo, dagNo, ownerName, surveyDate, notes, referenceCode. `renameProject(title)` separate quick action. Changing metadata NEVER touches plots.

## 6. Save / load / snapshots
- `saveProjectToRoom(statusOverride?)` → toast `"সংরক্ষিত হয়েছে"`; writes plotsJson + canvasSettingsJson + selectedPlotIdsJson + all scalar columns.
- Snapshots: `openSnapshotsSheet`, `createNamedSnapshot(label)` → `ProjectSnapshotEntity` (plotsJson+calibration copy), `observeProjectSnapshots`, `restoreSnapshot(id)` (undoable — pushes undo snapshot first).
- Process death: state reloads from Room; canvasSettings/selection survive (v5 columns). Print config survives via PrintConfigStore.

## 7. Text/Line size modal (`openTextLineSizeModal/close…/updateTextAndLineSize`)
Edits project-level `labelFontSizeSp` (default 11) and `strokeWidthPx` (default 4.5) — these are BASE sizes; canvas multipliers (fontScale/lineScale) apply on top. Per-plot override via labelStyle.fontSizeScale.

## 8. Lifecycle messages (verbatim Bengali toasts)
`"প্লট মার্জ সম্পন্ন (X শতক)"` · `"ক্যালিব্রেশন সফল: X পিক্সেল = Y ফুট • প্রতি পিক্সেল Z ফুট"` · `"১ শতক = … বর্গফুট প্রয়োগ…"` · `"<plot.name> তৈরি হয়েছে (X শতক)"` · `"সংরক্ষিত হয়েছে"`.
