# 05 — Multi-Plot, Selection (8 modes) & Land Division (19 road layouts)

## 1. Plot management
- Pills row (bottom bar): tap = activate+select; long-press = context (rename/lock/delete/highlight/duplicate).
- `selectPlotById/ByIndex`, `togglePlotSelectionAndActivate(id)` — selection is ALWAYS explicit; **v1.2 rule: nothing is auto-selected, default selection = ∅**.
- Metadata per plot: `updateActivePlotMetadata` (ownerName/dagNumber/khatianRef/notes/status), `savePlotAnnotation`.
- Flags: `togglePlotAsRoad` (roads excluded from area table + printed separately), `togglePlotHidden`, `togglePlotHighlight`, `togglePlotLocked`; rename dialog (`openRenameDialog` + commit), delete (`deleteActivePlotOrVertex`), duplicate, geometry copy/paste (04 §7).
- Merge: `mergeActiveWithNextPlot()` — union polygon via JTS; toast `"প্লট মার্জ সম্পন্ন (X শতক)"`.
- Label ops: `setLabelStyle(plotId, transform)` (single), `applyLabelStyleToSelection(transform)` (multi), `copyLabelStyle`/`pasteLabelStyle`, `resetLabelStyle(plotId)`, `toggleLabelLocked(plotId)`, `applyCadFontScaleToSelection(scale)`; locked style (non-default) refuses edits.

## 2. Selection modes — ALL 8 (v1.4/v1.5)
| Mode | Function | Semantics |
|---|---|---|
| Tap toggle | `togglePlotSelectionAndActivate` | single in/out + activate |
| Rectangle | `selectPlotsInRectangle(a, b)` | bbox intersects ring → select |
| All | `selectAllPlots()` | every visible plot |
| None | `deselectAllPlots()` | clear set |
| Invert | `invertPlotSelection()` | complement within visible plots |
| Adjacent | `selectAdjacentPlots()` | plots sharing any boundary segment with active plot (JTS touches) |
| By দাগ | `selectPlotsByDagNumber(dag)` | exact/normalized match |
| By owner | `selectPlotsByOwner(name)` | |
| By area | `selectPlotsByAreaRange(minShotok, maxShotok)` | uses stored শতক |
Selection state persists in Room (`selectedPlotIdsJson`) and drives print inclusion (`includePlotIds`); `clearPlotSelection()`.

## 3. Division modal (`openDivisionModal/closeDivisionModal`)
Types (`setDivisionType`): EQUAL · PERCENTAGE · CUSTOM_AREA.
- EQUAL: `setDivisionPartsCount(n)` + `adjustDivisionPartsDelta(±)`; optional `setDivisionSweepAngle(angle)` for angular sweep splits; `useCustomRoadCenterline` + `addManualSplitPoint` for manual centerline.
- PERCENTAGE: `updateDivisionPercentage(index, value)` — live validation Σ=100±0.01.
- CUSTOM_AREA: `updateCustomTargetShotok(index, shotok)` per part; residual part auto-computed.
- Road: `setSelectedRoadOption(id)` + `setRoadWidthFeet(w)` + `adjustRoadWidthDelta(±)`; `insertRoadBend`/`moveRoadNode`/`resetRoadToPreset` for custom centerline.
- **19 road layout options** (`roadOptionsList`, preserve ALL): none_vertical / none_horizontal / cross / middle_vertical / middle_horizontal(ডিফল্ট) / side_top / side_bottom / side_left / side_right / t_bottom / t_top / t_right / t_left / l_bottom_left / l_bottom_right / l_top_left / l_top_right / both_sides_longitudinal / both_sides_transverse — grouped as 4 L-variants + longitudinal/transverse pairs.

## 4. Division engine (`JtsDivisionEngine` — JTS Core)
- `previewDivision()` → `_previewDividedPlots` (overlay, dashed preview) + `divisionReport: DivisionReport` (per-part area in শতক with CURRENT `sqFeetPerShotok`, road area, net area; `GeometryUtils.sqFtToShotangsho(sqFt, factor)`).
- `cancelDivisionPreview()`; `commitDivision()` — undoable (snapshot first); replaces plots with parts (names `"প্লট X.১"…`), roads as `isRoad = true` plots; toast summary.
- Validation: `JtsDivisionEngine.validatePolygon(points)` (closed ring, non-self-intersecting, min area) → blocks commit with bilingual message.
- Numeric guarantee: Σ parts + road = original area ± ε (test-locked).

## 5. Snapshots & comparison
`observeProjectSnapshots` (Room), `createNamedSnapshot(label)`, `restoreSnapshot(id)` — restores plots+calibration (undoable). Comparative pantograph (`setPathographSwipeRatio`, `toggleSideBySideComparison`, `updateComparativeSummary`) is a separate tool but shares the same project model.

## 6. Acceptance behaviors (test-asserted)
1. Selection ∅ by default; report with ∅ selection renders placeholder `"রিপোর্টের জন্য প্লট নির্বাচন করুন"`.
2. `templateChange_neverChangesGeometryOrLabels`: polygonsDrawn/dimensionLabels/areaTableRows invariant across all 5 templates.
3. Division percentages must sum to 100; custom target shotoks must be > 0.
4. Merge/division/restore all push undo snapshots; undo returns exact prior plots (including labelStyle).
