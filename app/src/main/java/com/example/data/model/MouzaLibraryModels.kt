package com.example.data.model

data class MouzaMapRecord(
    val mapKey: String,
    val divisionId: String,
    val divisionNameBn: String,
    val districtId: String,
    val districtNameBn: String,
    val upazilaNameBn: String,
    val mouzaNameBn: String,
    val jlNumber: String,
    val sheetNumber: String = "১",
    val surveyType: String = "R.S",
    val scaleId: String = "scale_16",
    val scanDpi: Int = 300,
    val isFavorite: Boolean = false,
    val isOfflineSaved: Boolean = true,
    val lastOpenedAt: Long = 0L
)
