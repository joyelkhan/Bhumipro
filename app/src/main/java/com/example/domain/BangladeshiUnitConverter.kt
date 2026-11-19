package com.example.domain

import com.example.data.model.UnitSanity

enum class LandAreaUnit(val id: String, val labelBn: String, val sqFtFactor: Double) {
    SHOTOK("shotok", "শতক / ডেসিমেল", 435.6),
    KATHA("katha", "কাঠা (৭২০ বর্গফুট)", 720.0),
    BIGHA("bigha", "বিঘা (২০ কাঠা)", 14400.0),
    ACRE("acre", "একর (১০০ শতক)", 43560.0),
    HECTARE("hectare", "হেক্টর", UnitSanity.SQ_FEET_PER_HECTARE),
    SQ_FT("sq_ft", "বর্গফুট", 1.0),
    SQ_METER("sq_m", "বর্গমিটার", UnitSanity.SQ_FEET_PER_SQ_METER),
    SQ_HAT("sq_hat", "বর্গহাত", 2.25),
    CHOTAK("chotak", "ছটাক (৪৫ বর্গফুট)", 45.0),
    KANI_CTG("kani_ctg", "কানি (চট্টগ্রাম - ১৭২৮০ বর্গফুট)", 17280.0)
}

object BangladeshiUnitConverter {
    fun convert(
        value: Double,
        from: LandAreaUnit,
        to: LandAreaUnit,
        shotokSqFtOverride: Double = UnitSanity.DEFAULT_SQ_FEET_PER_SHOTOK
    ): Double {
        val factorFrom = if (from == LandAreaUnit.SHOTOK) shotokSqFtOverride.coerceAtLeast(1.0) else from.sqFtFactor
        val factorTo = if (to == LandAreaUnit.SHOTOK) shotokSqFtOverride.coerceAtLeast(1.0) else to.sqFtFactor
        val sqFt = value * factorFrom
        return sqFt / factorTo
    }

    fun convertAll(
        value: Double,
        from: LandAreaUnit,
        shotokSqFtOverride: Double = UnitSanity.DEFAULT_SQ_FEET_PER_SHOTOK
    ): Map<LandAreaUnit, Double> {
        return LandAreaUnit.entries.associateWith { target ->
            convert(value, from, target, shotokSqFtOverride)
        }
    }
}
