package com.example.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

object BengaliFormatters {
    private val enToBnDigits = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )
    private val bnToEnDigits = enToBnDigits.entries.associate { (k, v) -> v to k }

    fun toBengaliDigits(input: String): String {
        if (input.isEmpty()) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(enToBnDigits[ch] ?: ch)
        }
        return sb.toString()
    }

    fun toBengaliDigits(number: Int): String = toBengaliDigits(number.toString())

    fun toBengaliDigits(number: Long): String = toBengaliDigits(number.toString())

    fun toEnglishDigits(input: String): String {
        if (input.isEmpty()) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(bnToEnDigits[ch] ?: ch)
        }
        return sb.toString()
    }

    fun parseDoubleFlexible(input: String): Double? {
        val cleaned = toEnglishDigits(input).trim().replace(",", "")
        return cleaned.toDoubleOrNull()
    }

    fun parseIntFlexible(input: String): Int? {
        val cleaned = toEnglishDigits(input).trim()
        return cleaned.toIntOrNull()
    }

    fun roundToDecimals(value: Double, decimals: Int): Double {
        if (!value.isFinite()) return 0.0
        val factor = 10.0.pow(decimals.coerceIn(0, 6))
        return kotlin.math.round(value * factor) / factor
    }

    fun formatDecimal(value: Double, decimals: Int = 2, useBengali: Boolean = true): String {
        val d = decimals.coerceIn(0, 4)
        val safe = if (value.isFinite()) value else 0.0
        val formatted = String.format(Locale.US, "%.${d}f", safe)
        return if (useBengali) toBengaliDigits(formatted) else formatted
    }

    fun formatFtInches(feet: Double, useBengali: Boolean = true): String {
        val safeFeet = if (feet.isFinite() && feet > 0.0) feet else 0.0
        val totalInches = (safeFeet * 12.0).roundToLong().coerceAtLeast(0L)
        val ftPart = totalInches / 12L
        val inPart = totalInches % 12L
        val raw = "$ftPart' $inPart\""
        return if (useBengali) toBengaliDigits(raw) else raw
    }

    fun bengaliToday(date: Date = Date()): String {
        val raw = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date)
        return toBengaliDigits(raw)
    }

    fun bengaliDateTimeSlug(date: Date = Date()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
    }
}
