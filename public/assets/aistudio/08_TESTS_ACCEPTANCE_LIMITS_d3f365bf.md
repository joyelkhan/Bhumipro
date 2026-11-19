# 08 — Tests, Acceptance, Edge Cases & Honest Limitations

## 1. Test suite (v1.6.0 baseline: **159 tests / 0 failures**, Robolectric)

### Test files (must all exist & pass)
| File | Coverage |
|---|---|
| `ReportEngineTest` | ft'ins reference format `"১০১' ১১\""`; transform fit; all 5 templates render 595×842; template-invariant stats; **dimensionLabels_useSameConversionAsMapCanvas** (mapper text == canvas formattedLength); PDF header `%PDF-`; bengaliToday regex `[০-৯]{2}/[০-৯]{2}/[০-৯]{4}`; visual sheets writer |
| `SurveyPadPrintFidelityTest` (v1.6, 13 tests) | see §2 |
| `WorkspaceV14ViewModelTest` | label styles undoable; selection ops; canvas settings; vertex delete; paste; Room v5 round-trip |
| `WorkspaceV14EngineTest` | engine-level (selection geometry etc.) |
| `V150MasterPromptTest` | v1.5 CAD scales, presets, zoom modes |
| `SettingsPersistenceTest` / `SettingsResetRoomSafetyTest` / `SettingsRegistryTest` / `SettingsCodecTest` / `SettingsViewModelTest` | settings never touch draft_projects |
| `LandProGisUnitTest` | GeometryUtils/MeasurementEngine/AnaGonda/JtsDivision |
| `RefactorSprintTest` | decontamination contracts |
| `ExampleUnitTest` / `ExampleRobolectricTest` | sanity |

### Test-environment contract
`Dispatchers.setMain(UnconfinedTestDispatcher())`; in-memory Room `allowMainThreadQueries()`; VM built with `databaseOverride`; await `workspaceReady.first { it }` (≤15s); Robolectric has NO native PdfDocument → exporter auto-falls back to raster (same renderer) — tests must accept `[(1,2)]` (JVM) or `[(1,2),(2,2)]` (device) for multi-page progress.

## 2. v1.6 fidelity tests (exact assertions)
1. `mapper_capturesEveryCanvasSettingIntoPad` — unit/precision/color/scales/text-position/filters/opacity/factor/layer snapshot + ALL print-only knobs (content mode, vertex numbers, fill opacity, collision off, stroke override #2563EB from BLUE mode).
2. `dimensionLabels_ftIns_keepsCanvasTextVerbatim` — stored `"৫৫' ০\""` identity.
3. `dimensionLabels_meters_liveRecomputeWithPrecision` — 100 units × 0.55 = 55 ft = 16.764 m → `"১৬.৭৬ মি"`.
4. `areaLabels_shotokFactor_recomputedLiveOnPaper` — 4356 sqFt: 435.6→`"১০.০০ শতক"` · 432→`"১০.০৮ শতক"` · 430→`"১০.১৩ শতক"`.
5. `totalArea_perimeter_labelsFollowTogglesAndUnits` — pad gates + `"১৯৮…"` perimeter feet prefix.
6. `perPlotLabelStyle_transfersToPaper` — every PlotLabelStyle field → ReportLabelStyle.
7. `pageLayout_resolvesPaperOrientationMargin` — A4 portrait 595×842, margin 12mm→34pt±0.5; A3 landscape 1191×842; custom 800×1000.
8. `buildFileName_mapNameTitleDate_bengaliSafe` — Bengali kept, `/`→`_`, blanks→`Mouza_Map_Survey_Report`.
9. `pageNumberLabel_bengaliDigits` — `"পৃষ্ঠা ২/৩"`; single page → `""`.
10. `renderPreviewBitmap_matchesPageLayoutGeometry` — A3 landscape preview = 1191×842.
11. `drawReport_collisionAvoidance_andVertexNumbers_runClean` — ≥4 labels drawn.
12. `bitmapFilters_…` — passthrough same-instance; extremes no-crash.
13. `printConfig_undoRedo_roundTrip` / `resetPrintFidelityToCanvas_restoresDefaults` / `printPresets_saveApplyDelete_roundTrip` / `printPresetDao_survivesSchemaV7_table` / `surveyPadPdf_multiPage_progressAndCancel` (incl. cancel → `রপ্তানি বাতিল করা হয়েছে`, file deleted) / `exportSurveyPadPdf_viewModel_endToEnd_writesFile`.

## 3. Acceptance checklist (manual smoke, real device)
1. Digitize 4-corner plot on a 300-DPI mouza scan (scale_16) → edge labels ft'ins identical to canvas pills.
2. Switch unit মিটার → all labels update instantly; শতক area unchanged.
3. শতক dialog 430 → areas recompute; print preview area label matches canvas.
4. Per-plot style (italic, red, rotation 45°, dragged) → canvas AND paper show identical label.
5. Hide ম্যাপ ও পরিমাপ layers → print reflects; restore.
6. Grid+crosshair on → visible on paper at same spacing.
7. Brightness −0.3 + grayscale → inset tone matches canvas.
8. A3 landscape + margin 20mm → preview and PDF page both reflow; page number `পৃষ্ঠা ১/২` with individual-plot pages on.
9. Undo/redo print changes; reset-to-canvas; apply-to-canvas (grayscale pushes to canvas).
10. Save preset → new project → apply → all print knobs restored.
11. Cancel mid-export → partial file deleted, toast রপ্তানি বাতিল করা হয়েছে.
12. Block path: fresh uncalibrated project → print opens with blocked banner.

## 4. Edge cases (must handle, all implemented)
- Very large scans → TiledBitmapManager tiles + downsample; OOM → bilingual error + fallback page.
- 2000+ plots → STRtree hit-testing, viewport culling, text cache.
- Overlapping dimension labels → single-pass nudge (deterministic).
- Extreme label sizes (0.4×–4×) clamped; rotation ±180 clamped.
- Missing calibration → blocked print with message (never wrong numbers).
- Empty selection → placeholder page, not blank.
- Mixed units → ft'ins uses canvas text; others recompute (never mixes stored+live).
- Low storage / write failure → IOException bilingual; partial file deleted.
- Long Bengali titles → filename parts capped 48 chars, renderer wrapLines for disclaimer.
- Process death → Room + DataStore restore everything incl. print config, presets, canvas settings.
- Custom paper → any W×H ≥ 100pt content enforced by PageLayout.contentWidth/Height floor.

## 5. Known limitations (honest, do NOT fake)
1. **DWG print**: DWG/DXF import exists (CadMapHelper, sample DXF layer); **native DWG parsing/printing is NOT implemented** — listed in ExportFormat.DXF for export only; the v1.6 print pipeline renders DWG overlays as their imported/converted layers.
2. **Multi-page continuation**: individual-plot pages exist; automatic content-splitting of one huge plot across pages is NOT implemented (single plot always fits one page via fit-transform).
3. **Label collision avoidance** is deterministic single-pass nudge — not a full solver; dense label clusters may still slightly overlap (documented).
4. **JVM tests**: native PdfDocument unavailable under Robolectric → vector path falls back to raster in tests; device builds use the vector path.
5. **Signing**: v1.6.0 APKs signed with upload key **v2** (random password, delivered in `KEYSTORE_CREDENTIALS_v2.txt`) because the v1 key password was lost with the previous sandbox session; installing v1.6.0 over v1.5.0 requires uninstall-first (signature change). If the original AI Studio-managed key is still available to the owner, re-sign with it for update continuity.
6. **Georeferencing metadata in PDF**: not embedded (no GeoPDF XMP); scale text + scale bar carry the calibration instead.
7. **Screenshot policy**: preview screen disables screenshots by design (FLAG_SECURE), share via PNG export instead.

## 6. Definition of done (for a rebuild AI)
- All 9 entities + 9 DAOs + migrations 2→7; 159+ tests green; two signed APKs (debug+release); SHA256SUMS; `versionCode 7 / versionName 1.6.0`; fidelity checklist (06 §8) fully satisfied; no decorative controls; no duplicate print persistence; single renderer.
