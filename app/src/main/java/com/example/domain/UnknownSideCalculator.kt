package com.example.domain

import kotlin.math.sqrt

data class TriangleAreaResult(
    val isValid: Boolean,
    val errorBn: String? = null,
    val areaSqFt: Double = 0.0,
    val areaShotok: Double = 0.0,
    val areaKatha: Double = 0.0,
    val perimeterFeet: Double = 0.0
)

data class QuadAreaResult(
    val isValid: Boolean,
    val errorBn: String? = null,
    val exactAreaSqFt: Double = 0.0,
    val exactAreaShotok: Double = 0.0,
    val averageAreaSqFt: Double = 0.0,
    val averageAreaShotok: Double = 0.0,
    val differenceShotok: Double = 0.0
)

data class MissingSideResult(
    val isValid: Boolean,
    val errorBn: String? = null,
    val requiredLengthFeet: Double = 0.0,
    val formattedLengthFtIn: String = "",
    val totalSqFt: Double = 0.0
)

data class PartitionShareRow(
    val label: String,
    val weightOrPercent: Double,
    val normalizedPercent: Double,
    val areaShotok: Double,
    val areaSqFt: Double,
    val areaKatha: Double
)

object UnknownSideCalculator {

    fun calculateTriangleHeron(
        aFeet: Double,
        bFeet: Double,
        cFeet: Double,
        sqFeetPerShotok: Double = 435.6
    ): TriangleAreaResult {
        if (aFeet <= 0 || bFeet <= 0 || cFeet <= 0) {
            return TriangleAreaResult(false, "সব বাহুর দৈর্ঘ্য ০-এর বেশি হতে হবে")
        }
        if (aFeet + bFeet <= cFeet || aFeet + cFeet <= bFeet || bFeet + cFeet <= aFeet) {
            return TriangleAreaResult(false, "ত্রিভুজ গঠন সম্ভব নয় (দুই বাহুর যোগফল তৃতীয় বাহুর চেয়ে বড় হতে হবে)")
        }
        val s = (aFeet + bFeet + cFeet) * 0.5
        val sq = (s * (s - aFeet) * (s - bFeet) * (s - cFeet)).coerceAtLeast(0.0)
        val areaSqFt = sqrt(sq)
        val factor = sqFeetPerShotok.coerceAtLeast(1.0)
        val shotok = areaSqFt / factor
        return TriangleAreaResult(
            isValid = true,
            areaSqFt = areaSqFt,
            areaShotok = shotok,
            areaKatha = shotok / 1.65,
            perimeterFeet = aFeet + bFeet + cFeet
        )
    }

    fun calculateQuadrilateral(
        northFeet: Double,
        southFeet: Double,
        eastFeet: Double,
        westFeet: Double,
        diagonalFeet: Double?,
        sqFeetPerShotok: Double = 435.6
    ): QuadAreaResult {
        if (northFeet <= 0 || southFeet <= 0 || eastFeet <= 0 || westFeet <= 0) {
            return QuadAreaResult(false, "চারটি বাহুর দৈর্ঘ্যই ০-এর বেশি হতে হবে")
        }
        val factor = sqFeetPerShotok.coerceAtLeast(1.0)
        val avgSqFt = ((northFeet + southFeet) * 0.5) * ((eastFeet + westFeet) * 0.5)
        val avgShotok = avgSqFt / factor

        if (diagonalFeet == null || diagonalFeet <= 0.0) {
            return QuadAreaResult(
                isValid = true,
                exactAreaSqFt = avgSqFt,
                exactAreaShotok = avgShotok,
                averageAreaSqFt = avgSqFt,
                averageAreaShotok = avgShotok,
                differenceShotok = 0.0
            )
        }

        val t1 = calculateTriangleHeron(northFeet, eastFeet, diagonalFeet, factor)
        val t2 = calculateTriangleHeron(southFeet, westFeet, diagonalFeet, factor)
        if (!t1.isValid || !t2.isValid) {
            return QuadAreaResult(false, "কর্ণের দৈর্ঘ্যটি চার বাহুর সাথে সামঞ্জস্যপূর্ণ নয়")
        }
        val exactSqFt = t1.areaSqFt + t2.areaSqFt
        val exactShotok = exactSqFt / factor
        return QuadAreaResult(
            isValid = true,
            exactAreaSqFt = exactSqFt,
            exactAreaShotok = exactShotok,
            averageAreaSqFt = avgSqFt,
            averageAreaShotok = avgShotok,
            differenceShotok = kotlin.math.abs(avgShotok - exactShotok)
        )
    }

    /**
     * Solves unknown side (e.g., length or opposite side) for a target area.
     * For rectangular plots: oppositeSideFeet == knownWidthFeet -> depth = targetSqFt / knownWidthFeet.
     * For trapezoidal plot where two parallel edges (a, b) differ and we want height, pass sideBFeet;
     * or if solving fourth side given targetArea, width, and one perpendicular side: area = ((known + unknown)/2) * width.
     */
    fun solveMissingSide(
        targetShotok: Double,
        knownSideFeet: Double,
        oppositeParallelSideFeet: Double? = null,
        sqFeetPerShotok: Double = 435.6
    ): MissingSideResult {
        if (targetShotok <= 0.0 || knownSideFeet <= 0.0) {
            return MissingSideResult(false, "নির্ধারিত জমি ও জানা বাহু ০-এর বেশি হতে হবে")
        }
        val factor = sqFeetPerShotok.coerceAtLeast(1.0)
        val targetSqFt = targetShotok * factor
        val reqFeet = if (oppositeParallelSideFeet != null && oppositeParallelSideFeet > 0.0) {
            val avgWidth = (knownSideFeet + oppositeParallelSideFeet) * 0.5
            targetSqFt / avgWidth
        } else {
            targetSqFt / knownSideFeet
        }
        return MissingSideResult(
            isValid = true,
            requiredLengthFeet = reqFeet,
            formattedLengthFtIn = BengaliFormatters.formatFtInches(reqFeet, useBengali = true),
            totalSqFt = targetSqFt
        )
    }

    fun calculateBontonnamaShares(
        totalShotok: Double,
        shareLabelsAndWeights: List<Pair<String, Double>>,
        sqFeetPerShotok: Double = 435.6
    ): List<PartitionShareRow> {
        val safeTotal = totalShotok.coerceAtLeast(0.0)
        val factor = sqFeetPerShotok.coerceAtLeast(1.0)
        val weightSum = shareLabelsAndWeights.sumOf { it.second.coerceAtLeast(0.0) }
        if (weightSum <= 0.0) return emptyList()
        return shareLabelsAndWeights.map { (label, w) ->
            val cleanW = w.coerceAtLeast(0.0)
            val ratio = cleanW / weightSum
            val shotok = safeTotal * ratio
            PartitionShareRow(
                label = label,
                weightOrPercent = cleanW,
                normalizedPercent = ratio * 100.0,
                areaShotok = shotok,
                areaSqFt = shotok * factor,
                areaKatha = shotok / 1.65
            )
        }
    }
}
