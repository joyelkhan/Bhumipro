package com.example.ui.settings

data class BpStringEntry(
    val bn: String,
    val en: String
)

object BpStrings {
    private val map = mapOf(
        "settings_title" to BpStringEntry("সেটিংস ও পছন্দসমূহ", "Settings & Preferences"),
        "section_appearance" to BpStringEntry("থিম ও ভাষা", "Appearance & Language"),
        "section_measurement" to BpStringEntry("পরিমাপ ডিফল্ট", "Measurement Defaults"),
        "section_behavior" to BpStringEntry("অ্যাপ আচরণ", "App Behavior"),
        "section_print_feed" to BpStringEntry("সার্ভে প্যাড ও ফিড", "Survey Pad & Feed"),
        "section_backup" to BpStringEntry("সেটিংস ব্যাকআপ (JSON)", "Settings Backup (JSON)"),
        "section_about" to BpStringEntry("ডেভেলপার ও সংস্করণ", "Developer & Version"),
        "theme_mode" to BpStringEntry("অ্যাপ থিম মোড", "App Theme Mode"),
        "language" to BpStringEntry("অ্যাপের ভাষা", "App Language"),
        "shotok_factor" to BpStringEntry("ডিফল্ট ১ শতক = বর্গফুট", "Default 1 Shotok = Sq.Ft"),
        "default_unit" to BpStringEntry("ডিফল্ট দৈর্ঘ্য একক", "Default Length Unit"),
        "default_precision" to BpStringEntry("দশমিক নির্ভুলতা", "Decimal Precision"),
        "quick_tips" to BpStringEntry("কুইক টিপস দেখান", "Show Quick Tips"),
        "sound_effects" to BpStringEntry("সাউন্ড ইফেক্ট", "Sound Effects"),
        "haptic_feedback" to BpStringEntry("হ্যাপটিক ভাইব্রেশন", "Haptic Feedback"),
        "notifications_enabled" to BpStringEntry("নোটিফিকেশন ও জরিপ সতর্কতা", "Notifications & Survey Alerts"),
        "auto_snapshot" to BpStringEntry("স্বয়ংক্রিয় স্ন্যাপশট", "Auto-Save Snapshots"),
        "print_defaults" to BpStringEntry("প্রিন্ট ডিফল্ট (টিকেট ও স্বাক্ষর)", "Print Defaults (Ticket & Signature)"),
        "open_feed" to BpStringEntry("নোটিফিকেশন ও জরিপ ফিড", "Notifications & Survey Feed"),
        "export_json" to BpStringEntry("JSON রপ্তানি / কপি", "Export / Copy JSON"),
        "import_json" to BpStringEntry("JSON ইমপোর্ট", "Import JSON"),
        "reset_settings" to BpStringEntry("সেটিংস রিসেট (প্রজেক্ট অক্ষত থাকবে)", "Reset Settings (Keeps Projects Safe)"),
        "developer_credit" to BpStringEntry("Abu Naser Khan • NaserX Labs", "Abu Naser Khan • NaserX Labs")
    )

    fun get(key: String, language: String = "বাংলা"): String {
        val entry = map[key] ?: return key
        return if (language == "English") entry.en else entry.bn
    }
}
