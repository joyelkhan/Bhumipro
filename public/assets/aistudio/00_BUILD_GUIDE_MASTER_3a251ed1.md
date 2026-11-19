# BhumiPro — মৌজা ম্যাপে জমির পরিমাণ (Mouza Map Land Measurement)
## Complete A-to-Z Rebuild Guide for AI — MASTER INDEX

**Version**: v1.6.0 · **applicationId**: `com.aistudio.landpro.xyzqr` · **minSdk 24 / targetSdk 36 / compileSdk 36**
**Scope**: এই ডকুমেন্ট সেট কেবল **মৌজা ম্যাপে জমির পরিমাণ** ফিচারটি শুরু থেকে শেষ পর্যন্ত, একটি ছোট জিনিসও বাদ না দিয়ে, যেকোনো AI দিয়ে স্ক্র্যাচ থেকে নতুন করে বানানোর জন্য লেখা। প্রতিটি ফিল্ড, ফাংশন, ডায়ালগ, অপশন, স্টেট, ভ্যালিডেশন ও বাংলা স্ট্রিং এখানে আছে।

---

## কিভাবে পড়বেন (Reading Order for AI)

| # | File | What it defines |
|---|------|-----------------|
| 00 | **BUILD_GUIDE_MASTER.md** (this file) | Index, build order, tech stack, non-negotiable rules |
| 01 | [01_ARCHITECTURE_AND_DATA.md](01_ARCHITECTURE_AND_DATA.md) | Package layout, every data model with EVERY field, Room v7 DDL, DataStore keys |
| 02 | [02_PROJECT_LIFECYCLE.md](02_PROJECT_LIFECYCLE.md) | Project create/import, Mouza scale calibration (presets/DPI/two-point/custom), metadata, save/load |
| 03 | [03_CANVAS_CORE.md](03_CANVAS_CORE.md) | InteractiveMapCanvas: render pipeline, gestures, zoom 15–1600%, grid/crosshair/filters, label rendering rules |
| 04 | [04_DIGITIZE_AND_MEASURE.md](04_DIGITIZE_AND_MEASURE.md) | Point-by-point digitization, vertex ops, measurement formulas, 7 unit systems, শতক factor dialog, undo/redo |
| 05 | [05_MULTI_PLOT_SELECTION_DIVISION.md](05_MULTI_PLOT_SELECTION_DIVISION.md) | Plot pills, all 8 selection modes, land division (equal/percent/custom + 19 road layouts), snapshots |
| 06 | [06_PRINT_PIPELINE_SURVEY_PAD.md](06_PRINT_PIPELINE_SURVEY_PAD.md) | v1.6 Survey Pad: data-fidelity model, renderer, PDF engine, customize tab, presets, undo/redo |
| 07 | [07_UI_STRINGS_INVENTORY.md](07_UI_STRINGS_INVENTORY.md) | Every Bengali string (labels/buttons/toasts/errors) for this feature, colors, navigation routes |
| 08 | [08_TESTS_ACCEPTANCE_LIMITS.md](08_TESTS_ACCEPTANCE_LIMITS.md) | Test list with exact assertions, acceptance checklist, edge cases, honest limitations |

## Tech Stack (must match exactly)

- **Language/UI**: Kotlin 2.2.x, Jetpack Compose (Material 3), single-Activity
- **Architecture**: MVVM (`LandSurveyViewModel` = single source of workspace state), Repository (`LandProRepository`), offline-first
- **Persistence**: Room 2.7 (schema **v7**, DB name `landpro_surveying.db`) + DataStore Preferences (settings + print config)
- **Geometry**: JTS Core 1.20 (`JtsDivisionEngine`, validation) + custom `MeasurementEngine`/`GeometryUtils` (calibration math)
- **Serialization**: Moshi 1.15.2 **codegen only** (`@JsonClass(generateAdapter = true)`)
- **PDF**: `android.graphics.pdf.PdfDocument` — ONE renderer (`BengaliSurveyReportRenderer`) draws preview bitmap, PDF page and thumbnails
- **Bengali font**: bundled Noto Sans Bengali (Typeface resolved in `PdfAndPrintHelper.resolveBengaliTypeface`)
- **Test**: JUnit4 + Robolectric 4.16 (`main` dispatcher replaced by `UnconfinedTestDispatcher`, in-memory Room via `databaseOverride`)

## Build Order (what to implement in what sequence)

1. **Data layer first** (01): entities + DAO + migrations 2→7, Moshi models, `SettingsDataStore`, `PrintConfigStore`
2. **Domain math** (04): `GeometryUtils`, `MeasurementEngine`, `LengthFormatter`, `BangladeshiUnitConverter`, `AnaGondaHelper`, `JtsDivisionEngine`, `SpatialIndex`
3. **ViewModel state machine** (02/04/05): every `MutableStateFlow` listed in 01 §5, then the function inventory (04 §7, 05 §6)
4. **Canvas** (03): `InteractiveMapCanvas` render pipeline + gestures; then bars/sheets/modals
5. **Screens & navigation** (07): Dashboard → Workspace → print screens; routes
6. **Print pipeline** (06): mapper → renderer → exporter → customize UI
7. **Tests** (08) as you go; suite must stay green (v1.6.0 baseline: **159 tests, 0 failures**)

## Non-Negotiable Rules (violating any = wrong build)

1. **অফলাইন-ফার্স্ট**: কোনো ফিচার নেটওয়ার্কের উপর নির্ভরশীল হবে না (মৌজা ম্যাপ লাইব্রেরি ডাউনলোড ব্যতীত, যা এই স্কোপের বাইরে)।
2. **Geometry is sacred**: label style / units / filters / selection কখনো stored measurement বা point পরিবর্তন করবে না। শুধু `GeometryUtils.recalculatePlot` (calibration বদলালে) ও division engine জ্যামিতি লিখতে পারে।
3. **একটাই প্রিন্ট কনফিগ পার্সিস্টেন্স**: `PrintConfigStore` (DataStore `print_config_prefs`) — লাইভ কনফিগের একমাত্র স্টোর। Room v7 `print_presets` শুধু নাম-দেওয়া স্ন্যাপশট, ডুপ্লিকেট সেটিংস সিস্টেম নয়।
4. **একটাই রেন্ডারার**: preview bitmap, PDF page, thumbnail — তিনটাই `BengaliSurveyReportRenderer.drawReport` দিয়ে আঁকা হয়; অন্য কোনো PDF ইঞ্জিন তৈরি করা যাবে না।
5. **প্রতিটি কন্ট্রোল আসল কাজ করবে**: decorative/placeholder UI নিষিদ্ধ।
6. **বাংলা-প্রথম**: সব ইউজার-ফেসিং টেক্সট বাংলা; ডিজিট বাংলা (`BengaliFormatters.toBengaliDigits`)।
7. **Room শুধু additive migration**: নতুন কলাম = `ALTER TABLE ... ADD COLUMN ... DEFAULT`; কখনো destructive/fallback নয়; প্রজেক্ট ডেটা কখনো হারাবে না।
8. **ft'ins canvas parity**: প্রিন্টে ft'ins লেবেল সবসময় ক্যানভাসের stored `formattedLength` হুবহু কপি; অন্য একক হলে লাইভ রি-কম্পিউট (`LengthFormatter.format`)।

## Version History Anchors

- v1.1.0 — first real build pipeline (31 tests)
- v1.2.0 — decontamination, explicit plot selection (empty ≠ all)
- v1.4.0 — per-plot `PlotLabelStyle`, canvas settings (Room v5), selection ops
- v1.5.0 — CAD scale controls, Map Library (Room v6), zoom presets
- **v1.6.0 — Survey Pad print system: full data fidelity, page layout, multi-page, presets (Room v7), print undo/redo** (this guide's target)
