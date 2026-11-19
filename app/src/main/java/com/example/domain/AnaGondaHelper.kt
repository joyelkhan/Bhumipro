package com.example.domain

import com.example.data.model.UnitSanity
import kotlin.math.floor

data class AnaGondaBreakdown(
    val ana: Int = 0,
    val gonda: Int = 0,
    val kora: Int = 0,
    val kranti: Int = 0,
    val til: Int = 0,
    val decimalFraction: Double = 0.0,
    val shareShotok: Double = 0.0,
    val shareSqFt: Double = 0.0
)

object AnaGondaHelper {
    const val SQFT_PER_SHOTOK = UnitSanity.DEFAULT_SQ_FEET_PER_SHOTOK

    // Standard Bangladeshi Khotiyan share:
    // 16 Ana = 1.0 (total estate)
    // 1 Ana = 20 Gonda -> 320 Gonda = 1.0
    // 1 Gonda = 4 Kora -> 1280 Kora = 1.0
    // 1 Kora = 3 Kranti -> 3840 Kranti = 1.0
    // 1 Kranti = 20 Til -> 76800 Til = 1.0
    const val TOTAL_TIL = 76800.0

    fun toDecimalFraction(ana: Int, gonda: Int, kora: Int, kranti: Int, til: Int): Double {
        val t = (ana.coerceIn(0, 16) * 4800.0) +
            (gonda.coerceIn(0, 19) * 240.0) +
            (kora.coerceIn(0, 3) * 60.0) +
            (kranti.coerceIn(0, 2) * 20.0) +
            til.coerceIn(0, 19)
        return (t / TOTAL_TIL).coerceIn(0.0, 1.0)
    }

    fun calculateShare(
        totalShotok: Double,
        ana: Int,
        gonda: Int,
        kora: Int,
        kranti: Int,
        til: Int,
        sqFeetPerShotok: Double = SQFT_PER_SHOTOK
    ): AnaGondaBreakdown {
        val factor = sqFeetPerShotok.coerceAtLeast(1.0)
        val fraction = toDecimalFraction(ana, gonda, kora, kranti, til)
        val shareShotok = totalShotok.coerceAtLeast(0.0) * fraction
        val shareSqFt = shareShotok * factor
        return AnaGondaBreakdown(
            ana = ana,
            gonda = gonda,
            kora = kora,
            kranti = kranti,
            til = til,
            decimalFraction = BengaliFormatters.roundToDecimals(fraction, 6),
            shareShotok = BengaliFormatters.roundToDecimals(shareShotok, 4),
            shareSqFt = BengaliFormatters.roundToDecimals(shareSqFt, 2)
        )
    }

    fun fromFraction(
        fraction: Double,
        totalShotok: Double = 100.0,
        sqFeetPerShotok: Double = SQFT_PER_SHOTOK
    ): AnaGondaBreakdown {
        val clamped = fraction.coerceIn(0.0, 1.0)
        var remTil = kotlin.math.round(clamped * TOTAL_TIL).toInt()
        val ana = remTil / 4800
        remTil %= 4800
        val gonda = remTil / 240
        remTil %= 240
        val kora = remTil / 60
        remTil %= 60
        val kranti = remTil / 20
        val til = remTil % 20
        val factor = sqFeetPerShotok.coerceAtLeast(1.0)
        val shareShotok = totalShotok * clamped
        return AnaGondaBreakdown(
            ana = ana,
            gonda = gonda,
            kora = kora,
            kranti = kranti,
            til = til,
            decimalFraction = clamped,
            shareShotok = BengaliFormatters.roundToDecimals(shareShotok, 4),
            shareSqFt = BengaliFormatters.roundToDecimals(shareShotok * factor, 2)
        )
    }

    fun shotokToSqFt(shotok: Double, sqFeetPerShotok: Double = SQFT_PER_SHOTOK): Double =
        shotok * sqFeetPerShotok.coerceAtLeast(1.0)

    fun sqFtToShotok(sqFt: Double, sqFeetPerShotok: Double = SQFT_PER_SHOTOK): Double =
        sqFt / sqFeetPerShotok.coerceAtLeast(1.0)
}
