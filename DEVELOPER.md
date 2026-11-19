# Developer Guide — BhumiPro (ভূমিপ্রো)

- **Developer / Owner**: Abu Naser Khan
- **Organization**: NaserX Labs
- **Application Name**: BhumiPro (`মৌজা ম্যাপে জমির পরিমাণ`)
- **Application ID**: `com.aistudio.landpro.xyzqr`
- **Gradle Namespace**: `com.example`
- **Version**: `1.6.0` (`versionCode = 7`)

## Technology Stack
- **Language**: Kotlin 2.x with Jetpack Compose & Material 3
- **Minimum / Target / Compile SDK**: `minSdk = 24` / `targetSdk = 36` / `compileSdk = 36`
- **Persistence**:
  - Room `v7` (`landpro_surveying.db`, 9 entities, 9 DAOs, additive migrations `2 -> 7`)
  - Jetpack DataStore Preferences (`bhumipro_settings_prefs`, `print_config_prefs`, `print_ticket_prefs`)
- **Geometry & GIS**:
  - LocationTech JTS Core `1.20.0` (`JtsDivisionEngine`, `SpatialIndex` STRtree)
  - Custom CAD / DXF parser (`CadMapHelper`) and 2D affine georeferencing (`GeoreferencingHelper`)
- **PDF & Barcode**:
  - Native `android.graphics.pdf.PdfDocument` vector pipeline with 2x JPEG raster fallback (`ReportPdfExporter`)
  - Single-source Bengali Survey Pad renderer (`BengaliSurveyReportRenderer`)
  - ZXing Core `3.5.3` QR & Code128 generation (`BarcodeAndQrHelper`)
- **Serialization**: Moshi `1.15.2` with KSP codegen (`@JsonClass(generateAdapter = true)`)

## Architecture
- **MVVM + Repository**:
  - `data/model`: Domain models, print config, canvas settings, Moshi codecs
  - `data/local`: Room database, DAOs, DataStore stores
  - `data/repository`: `LandProRepository`
  - `domain` & `domain/report`: Pure GIS geometry, unit converters, Bengali formatters, JTS division, Survey Pad fidelity & PDF exporter
  - `ui/theme`, `ui/components`, `ui/screens`, `ui/settings`, `ui/viewmodel`: Material 3 UI and lifecycle-aware `StateFlow` ViewModels

## Development & Release Standards
1. **Offline-First & Zero Tracking**: No analytics, ads, or unrequested network calls.
2. **Play Policy Compliance**: Zero-permission Android Photo Picker (`PickVisualMedia`) and SAF (`OpenDocument`) for media/file access; no broad external storage permissions.
3. **Explicit Plot Selection**: Reports render only user-selected plots (`selectedPlotIds`); empty selection renders a clear placeholder page instead of auto-selecting.
4. **Release Signing**: Configure `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` via secure environment variables before running `gradle bundleRelease` or `gradle assembleRelease`.
