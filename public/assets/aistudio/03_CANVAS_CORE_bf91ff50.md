# 03 — Canvas Core (InteractiveMapCanvas): render pipeline, gestures, view controls

## 1. Render order (per frame, bottom → top)
1. Base map raster (`pdfBitmap`) with `mapOpacity` + ColorMatrix filters — brightness/contrast/grayscale from `CanvasDisplaySettings` (same math as `BitmapMapFilters`).
2. Survey grid (`layer_grid` visible): world-anchored lines every `effectiveGridSpacing` canvas units (8..160), `gridColorHex`, alpha from layer opacity.
3. CAD overlay (`layer_cad`).
4. Plots (`layer_plots`): fill = plot color @ fill alpha × layer opacity; then white outline path (width = stroke × (1 + outlineScale×1.6)); then boundary stroke (width = strokeWidthPx × lineScale, strokeJoin ROUND).
5. Corner markers (`showCornerPoints`): circle radius `3 × pointScale`, stroke color = boundary; ring white 1px. Optional vertex numbers (v1.6 print; canvas optional).
6. Roads (`layer_roads`): isRoad plots.
7. Measurements (`layer_measure`): edge dimension text along each edge midpoint; color `measurementColorHex`; base size `dimensionFontSizeSp × fontScale × plot.labelStyle.fontSizeScale`; per-edge split totals if `showSplitTotalInside`.
8. Plot labels (`layer_labels`): plot name + area label at centroid (or manual offset): two-line `"name[dagPart]\nX শতক • pct%"` when in another plot's shadow, else `"name[dagPart]\nX শতক[notePart]"`; dagPart = `" (দাগ X)"` when dagNumber present.
9. Annotations (`layer_notes`).
10. Crosshair (`showCrosshair`): center cross.
11. Drawing overlay: in-progress polygon + first-edge length readout; snap indicator.
12. Tape measure line (`addTapeMeasurePoint`, `clearTapeMeasure`) + calibration tap markers when `isCalibrating`.
13. Minimap (optional, `toggleMinimap`): viewport rectangle over downscaled full map.

## 2. Gestures (all wired, none decorative)
| Gesture | Action |
|---|---|
| Single tap (select tool) | hit-test via `SpatialIndex` (STRtree) → `togglePlotSelectionAndActivate(plotId)`; on empty space: nothing |
| Tap in draw mode (`toggleDrawMode`/`startDrawingNewPlot`) | `addDrawingPoint(PointF)` |
| Long-press plot | confirm-add/select + context (rename/lock/delete pills row) |
| Drag plot (when active & unlocked) | move whole polygon (undoable) |
| Drag vertex (`beginVertexDrag`) | `moveSelectedVertex` / `moveDrawingPoint`; `nudgeSelectedVertex(dx,dy)` for keyboard-like nudges |
| Pinch | zoom 15%..1600% (`MapTransformMath` clamp), focal zoom |
| Double-tap | zoom-in step |
| Two-finger pan | translate; `requestFitScreen()` / `requestFitPlot()` reset view; `requestZoomPercent(p)`; `triggerZoomToFit()` |
| Long-press empty (draw mode) | `insertRoadBend`/insert vertex at edge midpoint (`insertVertexAtEdgeMidpoint`) |
| Label drag | `beginLabelDrag` → `dragLabel(plotId, offset)` sets `manuallyPositioned=true` |
| Snap toggles | `toggleSnap`, `toggleGridSnap`, `toggleOrthoSnap` |

## 3. Zoom & view
- Range: **15%–1600%**; preset chips (zoomPresetPercent): 15/25/50/75/100/150/200/400/800/1600.
- `activeTool` values: "select", "draw", "measure"(tape), "calibrate", "division", "vertex".
- Rotation: `adjustRotation(deg)` / `resetRotation()` (view-only rotation, geometry untouched).

## 4. Floating toolbar & persistent bottom bar
Floating tools: undo, redo, zoom in/out, fit screen, minimap, layers (`openLayersSheet`), grid toggle (`toggleShowGrid`).
Bottom bar (persistent): plot pills horizontal row (color dot + name + area), add plot, active tool button, print button, export button, more (⋮) → modals.
Plot pill long-press actions: rename (`openRenameDialog`), lock (`togglePlotLocked`), delete (`deleteActivePlotOrVertex`), highlight (`togglePlotHighlight`), duplicate (`duplicateActivePlot`).

## 5. Layers sheet (`openLayersSheet/closeLayersSheet`)
Per layer row: visibility eye (`toggleLayerVisibility`), lock (`toggleLayerLocked`), opacity slider (`setLayerOpacity`), rename (`renameLayer`), reorder (`moveLayer(up/down)`), batch: `showAllLayers()`, `hideAllLayers()`. Layer order in sheet = render zOrder.

## 6. Canvas settings binding (from CAD/text modals + v1.5 panel)
`updateCanvasSettings(transform)` — single entry; every knob in 01 §2.7 is reachable: unit chips, precision, measurement color, stroke scale, grid color/spacing, crosshair, corner points, brightness/contrast/grayscale sliders, map opacity, font/line/outline/point scale, text position, zoom text mode, outside/split toggles. `resetCanvasSettings()` restores defaults (Room v5 JSON replaced; plots untouched).

## 7. Performance requirements
- STRtree spatial index rebuilt on plot list change; hit-test O(log n) — must stay fluid at **2000+ plots**.
- Base raster: tile-decoded via `TiledBitmapManager` (no full decode of huge scans; viewport window + downsample).
- Text cache: formatted measurement strings cached per (edge length, unit, precision) to avoid per-frame format.
- Viewport culling: skip drawing plots outside visible rect.
