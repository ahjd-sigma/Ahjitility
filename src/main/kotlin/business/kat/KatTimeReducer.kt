package business.kat

import utils.KatConfig

class KatTimeReducer(private val config: KatUpgradeConfig = KatUpgradeConfig()) {

    fun calculateOptimalReduction(baseHours: Double, flowerPrice: Double = KatConfig.defaultFlowerPrice, bouquetPrice: Double = KatConfig.defaultBouquetPrice): TimeReduction {
        if (!config.enableTimeReduction || baseHours <= config.targetMaxDuration) {
            return TimeReduction(0, 0, baseHours)
        }

        // Find optimal flower/bouquet combination
        var bestReduction = TimeReduction(0, 0, baseHours)
        var minCost = Double.MAX_VALUE

        // Increase range to handle longer upgrades (e.g., 30 days = 720 hours)
        // 30 flowers = 720 hours, 6 bouquets = 720 hours
        for (flowers in 0..KatConfig.maxFlowers) {
            for (bouquets in 0..KatConfig.maxBouquets) {
                val totalSkip = (flowers * FLOWER_SKIP_HOURS) + (bouquets * BOUQUET_SKIP_HOURS)
                val finalDuration = kotlin.math.max(0.0, baseHours - totalSkip)

                // We want to reach the target duration with minimum cost
                if (finalDuration <= config.targetMaxDuration) {
                    val estimatedCost = (flowers * flowerPrice) + (bouquets * bouquetPrice)
                    if (estimatedCost < minCost) {
                        minCost = estimatedCost
                        bestReduction = TimeReduction(flowers, bouquets, finalDuration)
                    }
                }
            }
        }

        // If no combination reached target, find the one that reduces time the most within reason
        if (bestReduction.flowerCount == 0 && bestReduction.bouquetCount == 0 && baseHours > config.targetMaxDuration) {
            // Find max reduction possible within our loop limits
            val maxFlowers = KatConfig.maxFlowers
            val maxBouquets = KatConfig.maxBouquets
            val totalSkip = (maxFlowers * FLOWER_SKIP_HOURS) + (maxBouquets * BOUQUET_SKIP_HOURS)
            bestReduction = TimeReduction(maxFlowers, maxBouquets, kotlin.math.max(0.0, baseHours - totalSkip))
        }

        return bestReduction
    }

    companion object {
        val FLOWER_SKIP_HOURS get() = KatConfig.FLOWER_SKIP_HOURS
        val BOUQUET_SKIP_HOURS get() = KatConfig.BOUQUET_SKIP_HOURS
    }
}

data class TimeReduction(
    val flowerCount: Int,
    val bouquetCount: Int,
    val finalDuration: Double
)

data class KatUpgradeConfig(
    val targetMaxDuration: Double = KatConfig.targetMaxDurationHours,
    val enableTimeReduction: Boolean = KatConfig.enableTimeReduction
)