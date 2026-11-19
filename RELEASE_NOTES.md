# Release Notes — BhumiPro v1.6.0 (Code 7)

**Developer**: Abu Naser Khan • **Publisher**: NaserX Labs

## Highlights
- 5-tab adaptive shell (`হোম`, `ফাইল`, `বন্টননামা`, `ফিড`, `সেটিংস`), `"ম্যাপের উৎস বাছুন"` Photo Picker / PDF / DXF sheet, and 3-pane tablet plot inspector (`>= 720.dp`).
- Precision 2.6× zoom loupe during vertex editing, live CAD vector style preview modal, Method 1 scale card, and interactive map-draggable polyline road subdivision.
- Complete Bengali Survey Pad PDF & PNG print preview fidelity with full Print Defaults profile form and QR verification.
- Unknown Side & Multi-Method Area calculator, Bontonnama shareholder split calculator, Ana-Gonda calculator, unit converter, Digital Compass, and 8-division offline Mouza Map Library.
- Zero-permission Android Photo Picker & SAF import/export workflow compliant with Google Play Target SDK 36 policies.

## Known Limitations
1. **DXF/DWG Handling**: DXF overlay import and DXF export are supported; native binary `.dwg` parsing is not bundled.
2. **Per-Plot Continuation**: Multi-page export creates one full sheet plus one page per selected land plot; a single huge plot always scales to fit one page rather than splitting across multiple tiles.
3. **Label Collision Avoidance**: Uses a deterministic single-pass nudge algorithm; extremely dense clusters may still touch slightly.
4. **Georef PDF Metadata**: PDFs carry calibrated scale bars and text rather than embedded GeoPDF XMP tags.
5. **Preview Screenshot Protection**: `LivePrintPreviewScreen` enables `FLAG_SECURE`; share reports via PDF or PNG export.
