package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppPreferences(
    val language: String = "বাংলা",
    val themeMode: String = "লাইট",
    val appDefaultShotokSqFt: Double = 435.6,
    val dashboardTemplate: String = "standard",
    val defaultUnitId: String = MeasurementUnit.FEET_INCHES.id,
    val defaultPrecision: Int = 2,
    val showQuickTips: Boolean = true,
    val soundEffects: Boolean = true,
    val hapticFeedback: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val autoSaveSnapshots: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SettingsBundle(
    val version: Int = 1,
    val preferences: AppPreferences = AppPreferences(),
    val exportedAt: Long = 0L
)
