# 07 — UI: Navigation, Screens, Bengali Strings (verbatim), Colors

## 1. Navigation routes (MainActivity NavHost)
`dashboard` → `workspace/{projectId}?mode=` → `print_preview` (Survey Pad) → `print_ticket` (টিকেট সেটিংস) → `print_export_center` (legacy Export Center) · measurement tools routes · compass · ana_gonda. Workspace route: `BhumiProRoutes.workspaceRoute(projectId, mode)`.
Print preview wiring: `LivePrintPreviewScreen(viewModel = surveyViewModel, ticketViewModel, onBack, onOpenTicket, onOpenExportCenter)`.

## 2. Screen inventory for this feature
1. **Dashboard** (templates) — entry card `মৌজা ম্যাপে জমি পরিমাপ` (iconKey MOUZA_SURVEY, accent #2563EB, container #EFF6FF, pinned sortOrder 1).
2. **MapWorkspaceScreen** — top bar + canvas + pills row + floating toolbar + persistent bottom bar + all modals/sheets.
3. **Modals**: ChooseSource, Scale (ক্যালিব্রেশন), CalibrationDistance, ShotokStandard, TextLineSize, ProjectMetadata, Rename, Annotation, Division, Layers sheet, Snapshots sheet, SaveSurveyDialog, PostDivisionBottomSheet.
4. **Survey Pad Print Preview** (v1.6) + **PrintTicketScreen** + **Export Center**.

## 3. Bengali strings — verbatim inventory (Bengali primary, English secondary)
### Feature entry & workspace
`মৌজা ম্যাপে জমির পরিমাণ` (title) · `জমি মাপুন, ভাগবণ্টন করুন সুন্দর ভাবে` (description) · workspace mode title `মৌজা ম্যাপে জমি পরিমাপ`.
### Calibration
`১৬ ইঞ্চি = ১ মাইল (৩৩০ ফিট/ইঞ্চি)` / `৩২ … (১৬৫ …)` / `৬৪ … (৮২.৫ …)` / `৮০ … (৬৬ …)` · `স্বয়ংক্রিয় (ফাইল মেটাডেটা থেকে)` · `৩০০ DPI (স্ট্যান্ডার্ড স্ক্যান)` … · `কাস্টম DPI…` · `কাস্টম X ফুট/ইঞ্চি` · toasts: `ক্যালিব্রেশন সফল: X পিক্সেল = Y ফুট • প্রতি পিক্সেল Z ফুট`.
### শতক
`১ শতক = X বর্গফুট প্রয়োগ হয়েছে` · `১ শতক = X বর্গফুট প্রয়োগ ও ডিফল্ট হিসেবে সংরক্ষিত`.
### Units
`ফুট-ইঞ্চি` `ফুট` `মিটার` `সেন্টিমিটার` `লিঙ্ক` `চেইন` `হাত` · suffixes `′` ` মি` ` সেমি` ` লিঙ্ক` ` চেইন` ` হাত` · area: `শতক` `বর্গফুট` `একর` `হেক্টর` `বর্গমিটার` · `মোটঃ X শতক`.
### Plot lifecycle
`প্লট X তৈরি হয়েছে (Y শতক)` · `প্লট X (পেস্ট)` · `প্লট মার্জ সম্পন্ন (X শতক)` · `প্লট জ্যামিতি কপি হয়েছে` · `জ্যামিতি নতুন প্লট হিসেবে পেস্ট হয়েছে` · `শীর্ষবিন্দু মুছে ফেলা হয়েছে` · status `সক্রিয়`.
### Selection
`X টি প্লট নির্বাচিত` · `নির্বাচন মুছুন` · `কোনো প্লট নির্বাচিত নয় — ম্যাপে বা এখানে ট্যাপ করে রিপোর্টে প্লট যোগ করুন।`
### Print preview (Survey Pad)
Title `প্রিন্ট প্রিভিউ` · tabs `প্রিভিউ` `কাস্টমাইজ` · plot question `রিপোর্টে কোন প্লট যাবে?` · `প্রিন্টের লেখা` · fields `শিরোনাম` (default `ডিজিটাল ডিমার্কেশন`) `সার্ভেয়ার` · `উন্নত প্রিন্ট সেটিংস ও রপ্তানি কেন্দ্র` · buttons `PDF ডাউনলোড` `শেয়ার` `প্রিন্ট` · banner `এখানে স্ক্রিনশট বন্ধ। ছবি পাঠাতে শেয়ার → ছবি (PNG) ব্যবহার করুন।` · content descriptions `আনডু` `রিডু` `ক্যানভাস অবস্থায় রিসেট` `প্রিন্ট স্টাইল ক্যানভাসে প্রয়োগ` `টিকেট সেটিংস` `রিজেনারেট`.
Toasts: `প্রিন্ট কনফিগ গ্লোবাল ডিফল্ট হিসেবে সেভ হয়েছে` · `প্রিন্ট কনফিগ এই প্রজেক্টের জন্য সেভ হয়েছে` · `প্রজেক্ট ওভাররাইড মুছে ফেলা হয়েছে — গ্লোবাল ডিফল্ট প্রযোজ্য` · `প্রিন্ট কাস্টমাইজ ক্যানভাস অবস্থায় রিসেট হয়েছে` · `প্রিন্ট স্টাইল ক্যানভাসে প্রয়োগ হয়েছে` · `প্রিসেট সংরক্ষিত: X` · `প্রিসেট প্রয়োগ হয়েছে: X` · `প্রিসেট মুছে ফেলা হয়েছে` · `সার্ভে প্যাড PDF ডাউনলোডে সেভ হয়েছে` · `রপ্তানি বাতিল করা হয়েছে` · `PDF তৈরি হচ্ছে — পৃষ্ঠা ১/২` · `বাতিল`.
Ticket: `টিকেট সেভ হয়েছে` · `সব টিকেট মুছে ফেলা হয়েছে` · default title `ডিজিটাল ডিমার্কেশন`.
### Customize tab (labels as implemented)
`কাগজ ও লেআউট` · `ম্যাপ কন্টেন্ট ও প্লট` · `একক ও লেবেল` · `CAD স্টাইল (ক্যানভাস লাইভ)` · `অফিসিয়াল উপাদান` · `রিপোর্ট সেকশন ও সীমানা রং` · `প্রিন্ট প্রিসেট` · rows: `ম্যাপ কন্টেন্ট মোড` `পূর্ণ ম্যাপ` `সক্রিয় প্লট` `কনটেন্টে ক্রপ` `ভারটেক্স নম্বর দেখান (১,২,৩…)` `প্লট ফিল অস্বচ্ছতা` `লেবেল সংঘর্ষ এড়ানো (স্বয়ংক্রিয় সরানো)` `কর্নার পয়েন্ট দেখান (ক্যানভাস মান)` `দৈর্ঘ্য একক (সব লেবেল সাথে সাথে বদলাবে)` `দশমিক ঘর (০–৩)` `দশমিক নির্ভুলতা` `ক্ষেত্রফল একক` `শতক রূপান্তর ফ্যাক্টর ক্যানভাসের সেটিং অনুযায়ী লাইভ প্রয়োগ হয়` `পরিধি দেখান (পরিধিঃ …)` `মোট ক্ষেত্রফল সারি দেখান` `নির্বাচিত প্লটের লেবেল দেখান` `ডাইমেনশন লেবেল (মাস্টার)` `স্কেল বারের একক` `ক্যানভাস` `ফন্ট সাইজ গুণক` `লাইন প্রস্থ গুণক` `আউটলাইন গুণক` `পয়েন্ট সাইজ গুণক` `মাপের লেখা প্রস্থ গুণক` `মাপের লেখার অবস্থান` `ম্যাপের লাইন বাইরে দেখান` `বিভক্ত বাহুর মোট মাপ ভিতরে` `সার্ভে গ্রিড দেখান (layer_grid)` `ক্রসহেয়ার দেখান` `গ্রিড স্পেসিং` `ম্যাপ ফিল্টার (ক্যানভাসের মতোই কাগজে)` `ব্রাইটনেস` `কনট্রাস্ট` `গ্রেস্কেল` `ম্যাপ অস্বচ্ছতা` `নর্থ অ্যারো সাইজ` `ওয়াটারমার্ক` `QR কোড` `বারকোড` `লিজেন্ড` `ক্ষেত্রফল সারসংক্ষেপ` `হেডার ও পরিচিতি` `ফুটার ও পাতা` `নতুন প্রিসেটের নাম` `প্রয়োগ` `মুছুন` · presets empty: `কোনো প্রিসেট সংরক্ষিত নেই — নিচে নাম দিয়ে সেভ করুন` · chips `গ্লোবাল ডিফল্ট সেভ` `এই প্রজেক্টে সেভ` · margin field `মার্জিন (মিমি)` · paper labels `A4` `A3` `A2` `লিগ্যাল` `কাস্টম` `কাস্টম W×Hpt` · orientation `পোর্ট্রেট` `ল্যান্ডস্কেপ` · sections (ReportSectionId): `হেডার ও পরিচিতি` `শিরোনাম ও তারিখ` `ম্যাপ ও স্কেল` `সনাক্তকারী ও স্বাক্ষর` `দাবিত্যাগ` `পরিমাপ টেবিল` `লিজেন্ড` `ফুটার`.
### Official paper text (renderer)
`বসতমূলকারীঃ ডিজিটাল সার্ভেয়ার মোঃ <surveyorName>` · `ক্ষেত্র ১ ইঞ্চি = X ফিট` · `পরিধিঃ X` · `তারিখঃ X` · `"উ"`(উত্তর)/`"দ"`(দক্ষিণ)/`"পূ"`(পূর্ব)/`"প"`(পশ্চিম) compass letters · table header `প্লট / প্লটের পরিমান` · legend `জমির লাইন` `ডাইমেনশন লাইন` · footer default `ভূমি প্রো (BhumiPro) দিয়ে প্রস্তুত` · disclaimer (mapper) `এই ডিজিটাল ডিমার্কেশন রিপোর্টটি কেবল মৌল বি.আর.এস নকশা অনুযায়ী প্রস্তুত — সরকারিভাবে সত্যায়িত নথি নয়।` · placeholder `রিপোর্টের জন্য প্লট নির্বাচন করুন` + `ম্যাপে প্লটে ট্যাপ করে রিপোর্টে যোগ করুন` · watermark `BHUMIPRO SURVEY` · scale suffix ` = ১ মাইল (৩৩০ ফিট)`.
### Errors (bilingual)
Print blocked: `ম্যাপ ক্যালিব্রেট করা হয়নি — প্রিন্টের আগে স্কেল/DPI ক্যালিব্রেশন সম্পন্ন করুন` · warning `কোনো প্লট নির্বাচন করা হয়নি — প্রিভিউতে নির্দেশক পাতা দেখাবে` · export: `মেমরি কম — বড় ম্যাপ/বেশি প্লটে রপ্তানি ব্যর্থ। কম প্লট নির্বাচন করে চেষ্টা করুন। (low memory)` · `স্টোরেজ লেখা যায়নি — স্টোরেজ পূর্ণ বা অনুমতি নেই। (storage write failed)` · `স্টোরেজ অনুমতি নেই। (permission denied)` · `পিডিএফ ইঞ্জিন হঠাৎ বন্ধ হয়েছে — আবার চেষ্টা করুন। (pdf engine closed)` · `রপ্তানি ব্যর্থ: <msg>` · `PDF তৈরি করা যায়নি — আবার চেষ্টা করুন` · `সংরক্ষণ ব্যর্থ হয়েছে` · `শেয়ার শিট খোলা হয়েছে` / `শেয়ার করা যায়নি` · `প্রিন্ট সার্ভিস চালু হয়েছে`.

## 4. TestTags (UI automation anchors)
preview_open_ticket · preview_regenerate · preview_canvas · plot_selection_row · preview_open_export_center · customize_toggle · action_download_pdf · action_share · action_print · print_tabs · print_undo · print_redo · print_reset_canvas · print_apply_canvas.

## 5. Colors (`BhumiProPrintColors` + core palette)
Screen bg light; cards white; print screen palette: ScreenBg, CardWhite, CardBorder, FieldBg, FieldBorder, InkDark/InkSecondary/InkMuted, OrangePrimary/OrangeDeep/OrangeCream, CharcoalDark, CheckGreen. Renderer constants: INK #1C1C1E, INK_SECONDARY #56575C, INK_MUTED #8F9096, PAGE_WHITE, REPORT_BLUE #0F32B4, TABLE_HEADER_BLUE #0B2EB2, BOUNDARY_GREEN #1E6B3C, PLOT_FILL_GREEN #F0FAF2, DIMENSION_RED #C23A32, DISCLAIMER_PINK #E78CAB, INSPECTOR_RED #B23030, RULER_GREY #C9CDCC, HATCH_GREY #E4E4E6, BOX_GREY #B6B9B8, ROW_ALT_GREY #EFF1F0. Measurement default #133C2D. Plot palette cycle #2563EB/#DC2626/#16A34A/#9333EA/#EA580C.

## 6. Responsive & polish
Phone-first single column; preview card aspect follows pageLayout; customize sections collapse to save space; tablet: same layout with wider preview. RTL: Bengali renders via `textDirection = ContentOrLegacyDirection` defaults; long titles wrap (renderer wrapLines for disclaimer; title single-line ellipsis on canvas side).
