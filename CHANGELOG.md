# Changelog

## [1.6.0] — NaserX Labs Release
### New Features & UI/UX Upgrades
- **5-Tab Adaptive Navigation Shell (`BhumiProMainScaffold`)**: Replaced single-scroll dashboard navigation with a 5-tab bottom `NavigationBar` (`হোম`, `ফাইল`, `বন্টননামা`, `ফিড`, `সেটিংস`) on compact displays and an adaptive `NavigationRail` on screens `>= 600.dp`.
- **Home Dashboard & Source Picker (`ToolDashboardScreen`)**: Added primary `"ম্যাপের উৎস বাছুন"` orange CTA + modal bottom sheet (`HomeMapSourceBottomSheet`) with Photo Picker (`PickVisualMedia`), PDF/DXF (`OpenDocument`), offline Mouza library browse, and blank plot launch; plus 2×2 Pinned Tools (`ডিজিটাল সার্ভে ও জরিপ`, `আনা গন্ডা ক্যালকুলেটর`, `জমির পরিমাপ রূপান্তর`, `অজানা বাহু ও ক্ষেত্রফল`), 2×2 Measurement Tools (`সার্ভেয়ার প্রো`, `কম্পাস ও দিকনির্দেশনা`, `ম্যাপ সার্ভার` / offline library, `হেল্প ও সাপোর্ট`), and Saved Recent Projects.
- **Unknown Side & Multi-Method Area Calculator (`UnknownSideCalculator`, `UnknownSideAreaScreen`)**: Added 2-tab calculator supporting Heron 3-side triangles, 4-side + diagonal quadrilaterals (with diagonal vs average comparison), trapezoids, and reverse target-shotok missing side computation.
- **Standalone Partition / Bontonnama Calculator (`BontonnamaScreen`)**: Added weighted shareholder distribution (`অংশীদার ১..N`) conserving total shotok/sq.ft/katha across heirs or buyers.
- **Interactive Polyline Road Subdivision (`JtsDivisionEngine`, `InteractiveRoadControlPanel`, `InteractiveMapCanvas`)**: Users can tap `"রাস্তা ডিজাইন (ম্যাপে টানুন)"` inside Division Modal to drag circular white bend vertices and orange midpoint handles live on the map canvas with live road dimensions (`রাস্তা W × L`), draggable share badges, `"সব ভাগ সমান"`, street-width steppers, and deterministic JTS `CAP_FLAT` corridor subtraction + area conservation.
- **Precision Zoom Loupe & Vertex Editing (`InteractiveMapCanvas`)**: Added a live 2.6× magnified cursor loupe (`PointerEventPass.Final` finger-lift cleanup) during vertex dragging plus a contextual red `(✕)` vertex deletion badge.
- **Workspace Bar & 3-Pane Tablet Inspector (`MapWorkspaceScreen`)**: Added 3-card status strip (Points / Scale / Active Area & Total), orange Snap toggle pill, and an expandable right-side `TabletPlotInspectorDrawer` on wide screens (`>= 720.dp`).
- **Upgraded Workspace Modals (`MapModals.kt`, `SaveSurveyDialog.kt`)**:
  - Method 1 blue calibration card + Scale & Scan DPI dropdowns (with PDF DPI guard).
  - Shotok factor modal with selectable radio cards (`৪৩৫.৬`, `৪৩২.০`, `৪৩০.০`) and `±` fine stepper.
  - Text & Line Size modal with live miniature CAD/map vector preview card, 3 quick size presets (`ছোট` / `মাঝারি` / `বড়`), sliders, steppers, corner point & segment dimension toggles, and vertex/segment unit dropdowns.
  - Save Survey dialog with total plots/selected plot summary banner and project title + location metadata inputs.
- **Expanded Mouza Catalog (`LandProDatabase`, `MouzaMapLibraryScreen`)**: Seeded offline Mouza map hierarchy across all **8 Bangladesh divisions** (`খুলনা`, `চট্টগ্রাম`, `ঢাকা`, `বরিশাল`, `ময়মনসিংহ`, `রংপুর`, `রাজশাহী`, `সিলেট`) with search, favorites filter, and `BackHandler` drill-down navigation.
- **Complete Print Defaults Form & PNG Share (`PrintTicketScreen`, `PrintPreviewScreen`, `PrintTicketViewModel`)**: Extended `ReportTicket` & DataStore persistence (`address`, `registrationNo`, `districtField`, `qualifications`, `slogan`, `reportNote`, `signatureUri`), added template preview carousel + `PickVisualMedia` logo/signature pickers, guarded initial DataStore load against dirty edits, and added `"ছবি (PNG)"` share action in Print Preview.
- **Survey Pad Print Fidelity Pipeline**: Retained `SurveyPadFidelity`, `PageLayout`, `PrintPageMath`, `BitmapMapFilters`, 5 report templates, 7-section customize UI, Room `print_presets` (`v7` migration `6 -> 7`), 25-step print undo/redo, multi-page Survey Pad PDF, and `FilesTabScreen` / `FeedTabScreen`.
- **Feedback & Notification Settings (`SettingsDataStore`, `FeedbackController`, `AdvancedSettingsScreen`)**: Added `soundEffects` (`SoundEffectConstants.CLICK`) and `notificationsEnabled` preferences.

### Bug Fixes & Verification
- Fixed `LengthFormatter.format` call signature in `MapWorkspaceScreen.kt`.
- Replaced unavailable `Icons.Default.MagnetOn` reference with `Icons.Default.GpsFixed`.
- Added `hasLocalEdits` guard in `PrintTicketViewModel` to avoid DataStore initial-load race conditions overwriting early state updates.
- Verified with `BhumiProV20UpgradeTest` (6 tests including Workflow 6 draw-mode deletion, zoom step, and PDF source tracking) alongside all 14 existing test suites (**15 suites, 47/47 tests passing**).
- Finalized **Workflows 1–6**:
  - Top 2×2 **পিন করা টুল** & **পরিমাপ** dashboard grids (`মৌজা ম্যাপে জমি পরিমাপ`, `ম্যাপ জয়েন্ট`, `তুলনামূলক প্যান্টোগ্রাফ`, `সকল মৌজা ম্যাপ`, `আনা-গন্ডা হিসাব`, `ক্ষেত্রফল ও অজানা বাহু`, `জমি পরিমাপ`, `জমির সকল সূত্র`).
  - **ম্যাপের উৎস বাছুন** & **সংরক্ষিত PDF বাছুন** sheets (`HomeMapSourceBottomSheet`, `SavedPdfLibraryBottomSheet`) and 4-level Bangladesh Division → District → Upazila → Mouza drill-down (`MouzaMapLibraryScreen`).
  - **PDF-aware Scale Calibration (`ScaleCalibrationModal`)** disabling Scan DPI dropdown with `"PDF-এর জন্য বন্ধ"` badge when a PDF is loaded.
  - **Workspace Toolbar & Digitization (`MapWorkspaceScreen`, `WorkspacePersistentBottomBar`, `InteractiveMapCanvas`)**: `"ম্যাপ হিস্টরি"` bottom tab, 8-item workspace overflow menu, live draw area status strip + `"সীমানা বন্ধ করুন"` emerald pill CTA, first-vertex tap-to-close, vivid red drawing lines with white text halos, pan/zoom step commands, and Screenshot 11 plot context menu.
