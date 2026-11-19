# 04 — Digitize & Measure: formulas, units, শতক, undo/redo (exact math)

## 1. Digitization flow
`startDrawingNewPlot()` → draw mode; taps add points (`addDrawingPoint`); live feedback: current edge length toast/inline in active unit; `removeLastDrawingPoint()`; `cancelDrawingPolygon()`; `closeDrawingPolygon()` → closes ring when ≥3 points → creates plot:
- name `"প্লট <bengali(n)>"`, color from palette cycle `["#2563EB", "#DC2626", "#16A34A", "#9333EA", "#EA580C"]`
- metrics via `MeasurementEngine.computePlotMetrics(plot, feetPerCanvasUnit, sqFeetPerShotok)` (below)
- toast `"<name> তৈরি হয়েছে (X শতক)"`; activePlotIndex = new plot.
`addNewPlot()` — quick default-rectangle plot (used in tests).
`toggleDrawMode()` — toggles draw/select tools.

## 2. Measurement formulas (ground truth)
```
signedArea(units²) = Σ (xᵢ·yᵢ₊₁ − xᵢ₊₁·yᵢ)/2          (shoelace)
sqFt   = |signedArea| × feetPerCanvasUnit²
sqM    = sqFt ÷ 3.280839895²
শতক    = sqFt ÷ sqFeetPerShotok (floor 1.0)     ← CURRENT factor, live
perimeter = Σ edgeLength × feetPerCanvasUnit
edgeLen(units) = hypot(b.x−a.x, b.y−a.y)
edgeFeet  = edgeLen × feetPerCanvasUnit
```
`computePlotMetrics` rounds: areaShotok & areaSqFt & sqM to 2dp; frontage = min(bboxW,bboxH) in feet; depth = max; avgSide = perimeter ÷ n; validation from `JtsDivisionEngine.validatePolygon` (isValidGeometry, isClockwise).
ft'ins format: `totalInches = round(feet×12)`; `"ftPart' inPart\""` in Bengali digits — e.g. 55 ft → `"৫৫' ০\""`. Reference: 101.9166… ft → `"১০১' ১১\""` (test-locked).

## 3. Segment records
For each edge i: `BoundarySegment(startLabel = vertex i label, endLabel = i+1, lengthFeet, formattedLength, bearingDegrees = atan2(dx,dy) in degrees normalized)`. `plot.segments.size` MUST equal `plot.points.size` (print label reuse depends on it).

## 4. Unit display (`LengthFormatter.format(feet, unit, precision, useBengali=true)`)
| Unit | Formula | Suffix |
|---|---|---|
| FEET_INCHES | round(inches) split | `৫৫' ০"` |
| FEET | ft | `′` |
| METERS | ft÷3.280839895 | ` মি` |
| CENTIMETERS | ft÷3.280839895×100 | ` সেমি` |
| LINKS | ft÷0.66 | ` লিঙ্ক` |
| CHAINS | ft÷66 | ` চেইন` |
| HAT | ft÷1.5 | ` হাত` |
Non-ft'ins: `formatDecimal(value, precision)` + suffix; precision 0..3. `toUnitValue(feet,unit)` for numeric readouts. **Changing unit is display-only** — stored `lengthFeet` never changes; canvas re-renders instantly; print follows (v1.6 mapper).

## 5. Area display (`AreaDisplayUnit`)
shotok (শতক, uses project factor) · sq_ft (বর্গফুট) · acre (একর, ÷43560) · hectare (হেক্টর, ÷107639.104) · sq_m (বর্গমিটার, ÷10.763910416709722). v1.6 paper wording (canvas parity): `"X শতক"`, `"X বর্গফুট"`, `"X একর"`, `"X হেক্টর"`, `"X বর্গমিটার"`; শতক 2dp, বর্গফুট 1dp, একর/হেক্টর 3dp, বর্গমিটার 2dp.

## 6. আনা-গন্ডা (`AnaGondaHelper`)
`SQFT_PER_SHOTOK` default 435.6; conversions shotok↔sqFt with override factor (110, 111-part math for আনা/গন্ডা/কড়ি/টিক representation — used by the separate tool screen, factor-aware).

## 7. Vertex operations (each undoable via `pushUndoSnapshot`)
`addVertexToActivePlot()` (append) · `insertVertexAtEdgeMidpoint()` · `deleteSelectedVertex()`/`deleteActivePlotOrVertex()` · `moveSelectedVertex`/`nudgeSelectedVertex` · `reverseActivePlotVertices()` · `copyActivePlotGeometry()` + `pasteCopiedPlotGeometry()` (offset +24,+24; new plot name `"প্লট X (পেস্ট)"`).
After any vertex change: recompute metrics (areaSqFt, শতক, perimeter, segments) — display recomputed, raw units² intact.

## 8. Undo / redo
`UndoSnapshot(plots, activePlotIndex, layers)` pushed before EVERY mutation (vertex ops, label style, selection transform, division commit, paste, recalc). `undo()`/`redo()` restore; `canUndo`/`canRedo` drive toolbar enabled state. Print customizations have a SEPARATE stack (06 §5) — the two never mix.

## 9. Plot annotations
`openAnnotationDialog`/`dismissAnnotationDialog`/`savePlotAnnotation(plotId, text)` → `annotationText`; rendered in `layer_notes`.

## 10. Complete public function inventory of LandSurveyViewModel (canonical names)
addCalibrationTapPoint, addControlPoint, addDrawingPoint, addManualSplitPoint, addNewPlot, addTapeMeasurePoint, addVertexToActivePlot, adjustDivisionPartsDelta, adjustRoadWidthDelta, adjustRotation, applyCadFontScaleToSelection, applyCalibrationDistance, applyLabelStyleToSelection, applyPrintLayoutPreset, applyPrintPreset, applyPrintStylesToCanvas, beginLabelDrag, beginVertexDrag, buildSurveyReportData, cancelCalibrationMode, cancelDivisionPreview, cancelDrawingPolygon, cancelPrintExport, clearPlotSelection, clearPrintJobMessage, clearProjectPrintOverride, clearStatusMessage, clearTapeMeasure, closeChooseSourceModal, closeDivisionModal, closeDrawingPolygon, closeLayersSheet, closeProjectMetadataDialog, closeRenameDialog, closeScaleModal, closeShotokStandardModal, closeSnapshotsSheet, closeTextLineSizeModal, commitDivision, copyActivePlotGeometry, copyLabelStyle, createNamedSnapshot, deleteActivePlotOrVertex, deletePrintPreset, deleteSelectedVertex, deselectAllPlots, dismissAnnotationDialog, dragLabel, duplicateActivePlot, exportAndSharePdf, exportProjectInFormat, exportSurveyPadPdf, hideAllLayers, importCadUri, importDocumentOrImageUri, initWorkspace, insertRoadBend, insertVertexAtEdgeMidpoint, invertPlotSelection, loadSampleDxfCadLayer, markPreserveSourceOnNextInit, mergeActiveWithNextPlot, moveDrawingPoint, moveLayer, moveRoadNode, moveSelectedVertex, nudgeSelectedVertex, observePrintJobState, observeProjectSnapshots, openChooseSourceModal, openDivisionModal, openLayersSheet, openProjectMetadataDialog, openRenameDialog, openScaleModal, openShotokStandardModal, openSnapshotsSheet, openTextLineSizeModal, pasteCopiedPlotGeometry, pasteLabelStyle, persistPrintConfig, previewDivision, printSurveyA4, redo, redoPrintChange, removeLastDrawingPoint, renameLayer, renameProject, requestFitPlot, requestFitScreen, requestZoomPercent, resetCanvasSettings, resetLabelStyle, resetPrintConfig, resetPrintFidelityToCanvas, resetRoadToPreset, resetRotation, restoreSnapshot, reverseActivePlotVertices, savePlotAnnotation, savePrintPreset, saveProjectToRoom, saveScaleSettings, selectAdjacentPlots, selectAllPlots, selectPdfPage, selectPlotById, selectPlotByIndex, selectPlotsByAreaRange, selectPlotsByDagNumber, selectPlotsByOwner, selectPlotsInRectangle, selectVertex, setActiveTool, setDivisionPartsCount, setDivisionSweepAngle, setDivisionType, setGeorefOpacity, setLabelStyle, setLayerOpacity, setPathographSwipeRatio, setRoadWidthFeet, setSelectedRoadOption, showAllLayers, startCalibrationMode, startDrawingNewPlot, toggleDrawMode, toggleGridSnap, toggleLabelLocked, toggleLayerLocked, toggleLayerVisibility, toggleMinimap, toggleOrthoSnap, togglePlotAsRoad, togglePlotHidden, togglePlotHighlight, togglePlotLocked, togglePlotReportSelection, togglePlotSelectionAndActivate, toggleShowGrid, toggleSideBySideComparison, toggleSnap, triggerZoomToFit, undo, undoPrintChange, updateActivePlotMetadata, updateCanvasSettings, updateComparativeSummary, updateControlPointOffset, updateCustomTargetShotok, updateDivisionPercentage, updatePrintConfig, updatePrintConfigUndoable, updateProjectMetadata, updateShotokStandard, updateTextAndLineSize.
