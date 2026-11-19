# BhumiPro — মৌজা ম্যাপে জমির পরিমাণ (Mouza Map Land Measurement)

Developed by **Abu Naser Khan** at **NaserX Labs**.

BhumiPro is an offline-first Android GIS land measurement, plot division, and Survey Pad PDF reporting application designed for Bangladeshi mouza maps and field surveying.

## Main Features
- **5-Tab Adaptive Shell**: Bottom `NavigationBar` on phones (`হোম`, `ফাইল`, `বন্টননামা`, `ফিড`, `সেটিংস`) and `NavigationRail` on tablets (`>= 600.dp`), with a unified `"ম্যাপের উৎস বাছুন"` source picker sheet on Home.
- **Interactive Mouza Map Workspace**: Import mouza scans (Photo Picker / PDF / DXF), calibrate via Method 1 scale presets (`১৬/৩২/৬৪/৮০ ইঞ্চি` + DPI) or 2-point ruler, and digitize plots with a 2.6× precision zoom loupe, quick vertex delete `(✕)`, magnetic snap, ortho snap, grid snap, and a docked 3-pane tablet inspector (`>= 720.dp`).
- **Bangladeshi Land Units & Calculators**:
  - Live switching across `ft'ins"`, `ft`, `meters`, `links`, `gaj`, and `hat`.
  - Configurable `১ শতক` conversion standard (`435.6`, `432.0`, `430.0`, or custom sq.ft).
  - **আনা-গন্ডা ক্যালকুলেটর**, **জমি একক রূপান্তর**, **অজানা বাহু ও ক্ষেত্রফল** (Heron, 4-side + diagonal vs average, inverse target shotok), **বন্টননামা ক্যালকুলেটর**, **ডিজিটাল কম্পাস**, and **অফলাইন মৌজা ম্যাপ লাইব্রেরি** covering all 8 Bangladesh divisions.
- **JTS Plot Division & Interactive Polyline Roads**: Split plots by equal parts, percentage, or custom shotok area with 16+ preset road layouts or interactive map-draggable polyline road corridors while conserving total land + road area.
- **Survey Pad Print Fidelity (v1.6.0)**:
  - Live-rendered Survey Pad preview matching canvas colors, units, per-plot label styles, grid, crosshair, corner points, brightness/contrast/grayscale, and layer opacity.
  - Print Defaults profile form, 5 report templates (`STANDARD`, `DIMENSION`, `ROYAL`, `LAND_MEASUREMENT`, `CUSTOM`), A4/A3/Legal/Letter/Custom page layouts, undo/redo print edits, reusable Room presets, multi-page individual plot sheets, QR/Code128 barcodes, and PDF/PNG/DXF/GeoJSON/CSV export.

## Build & Test Instructions
- Compile debug APK: `gradle :app:assembleDebug`
- Run unit & Robolectric tests: `gradle :app:testDebugUnitTest`
- Build signed release bundle (requires keystore env vars): `gradle :app:bundleRelease`

