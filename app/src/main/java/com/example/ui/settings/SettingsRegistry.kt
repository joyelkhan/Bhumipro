package com.example.ui.settings

import com.example.data.model.AppPreferences

enum class SettingControlType {
    SEGMENTED,
    TOGGLE,
    SLIDER,
    ACTION
}

data class SettingDescriptor(
    val key: String,
    val sectionKey: String,
    val titleKey: String,
    val controlType: SettingControlType,
    val options: List<String> = emptyList(),
    val summaryProvider: (AppPreferences) -> String
)

object SettingsRegistry {
    val entries: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            key = "theme_mode",
            sectionKey = "section_appearance",
            titleKey = "theme_mode",
            controlType = SettingControlType.SEGMENTED,
            options = listOf("লাইট", "ডার্ক", "সিস্টেম"),
            summaryProvider = { it.themeMode }
        ),
        SettingDescriptor(
            key = "language",
            sectionKey = "section_appearance",
            titleKey = "language",
            controlType = SettingControlType.SEGMENTED,
            options = listOf("বাংলা", "English"),
            summaryProvider = { it.language }
        ),
        SettingDescriptor(
            key = "default_shotok",
            sectionKey = "section_measurement",
            titleKey = "shotok_factor",
            controlType = SettingControlType.SEGMENTED,
            options = listOf("435.6", "432.0", "430.0"),
            summaryProvider = { "${it.appDefaultShotokSqFt} বর্গফুট" }
        ),
        SettingDescriptor(
            key = "default_unit",
            sectionKey = "section_measurement",
            titleKey = "default_unit",
            controlType = SettingControlType.SEGMENTED,
            options = listOf("feet_inches", "feet", "meters", "links", "gaj", "hat"),
            summaryProvider = { it.defaultUnitId }
        ),
        SettingDescriptor(
            key = "default_precision",
            sectionKey = "section_measurement",
            titleKey = "default_precision",
            controlType = SettingControlType.SEGMENTED,
            options = listOf("0", "1", "2", "3"),
            summaryProvider = { "${it.defaultPrecision} ঘর" }
        ),
        SettingDescriptor(
            key = "quick_tips",
            sectionKey = "section_behavior",
            titleKey = "quick_tips",
            controlType = SettingControlType.TOGGLE,
            summaryProvider = { if (it.showQuickTips) "চালু" else "বন্ধ" }
        ),
        SettingDescriptor(
            key = "haptic_feedback",
            sectionKey = "section_behavior",
            titleKey = "haptic_feedback",
            controlType = SettingControlType.TOGGLE,
            summaryProvider = { if (it.hapticFeedback) "চালু" else "বন্ধ" }
        ),
        SettingDescriptor(
            key = "auto_snapshots",
            sectionKey = "section_behavior",
            titleKey = "auto_snapshot",
            controlType = SettingControlType.TOGGLE,
            summaryProvider = { if (it.autoSaveSnapshots) "চালু" else "বন্ধ" }
        )
    )

    fun findByKey(key: String): SettingDescriptor? = entries.firstOrNull { it.key == key }

    fun sectionKeys(): List<String> = entries.map { it.sectionKey }.distinct()
}
