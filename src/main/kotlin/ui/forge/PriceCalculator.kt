package ui.forge

import forge.Item
import forge.PriceFetcher

class PriceCalculator(private val priceFetcher: PriceFetcher) {

    fun assignPrices(
        items: List<Item>,
        isInstantSell: Boolean,
        isInstantBuy: Boolean,
        slotsMultiplier: Int,
        quickForgeLevel: Int,
        bzTax: Double,
        ahMulti: Double
    ) {
        val reduction = when {
            quickForgeLevel == 0 -> 0.0
            quickForgeLevel == 20 -> 0.30
            else -> 0.10 + (quickForgeLevel.toDouble() * 0.005)
        }

        items.forEach { item ->
            if (item.itemId != null) {
                val result = priceFetcher.getSellPrice(item.itemId, isInstantSell)
                val rawPrice = result.price
                item.isBazaar = result.isBazaar

                if (item.isBazaar) {
                    item.appliedTaxRate = bzTax
                    item.value = rawPrice * (1.0 - bzTax / 100.0) * slotsMultiplier
                } else {
                    val baseRate = when {
                        rawPrice < 10_000_000 -> 2.0
                        rawPrice < 100_000_000 -> 3.0
                        else -> 3.5
                    }
                    val finalAhRate = baseRate * ahMulti
                    item.appliedTaxRate = finalAhRate
                    item.value = rawPrice * (1.0 - finalAhRate / 100.0) * slotsMultiplier
                }
            }

            var recipeCost = (item.coinCost ?: 0).toDouble()
            item.ingredients?.forEach { ingredient ->
                val result = priceFetcher.getBuyPrice(ingredient.itemId, isInstantBuy)
                recipeCost += result.price * (ingredient.quantity ?: 0)
            }
            item.totalRecipeCost = recipeCost * slotsMultiplier

            val totalProfit = item.value - item.totalRecipeCost
            val baseDurationSeconds = item.durationSeconds ?: 0
            val reducedDurationSeconds = (baseDurationSeconds.toDouble() * (1.0 - reduction)).toInt()

            if (reducedDurationSeconds > 0) {
                val hours = reducedDurationSeconds.toDouble() / 3600.0
                item.profitPerHour = totalProfit / hours
            } else {
                item.profitPerHour = totalProfit
            }
        }
    }

    fun getQuickForgeReduction(level: Int): Double {
        return when {
            level == 0 -> 0.0
            level == 20 -> 0.30
            else -> 0.10 + (level.toDouble() * 0.005)
        }
    }
}