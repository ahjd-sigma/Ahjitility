package business.forge

import utils.PriceResult
import utils.Log

object ForgeCalculations {
    fun calculateProfit(
        recipe: ForgeRecipe,
        sellPrice: PriceResult,
        ingredientPrices: Map<String, PriceResult>,
        config: ForgeConfig,
        hasBazaar: Boolean
    ): ForgeResult {
        Log.debug(this, "Calculating profit for ${recipe.displayName}")
        val sellValue = calculateSellValue(sellPrice, config)
        val totalCost = calculateTotalCost(recipe, ingredientPrices, config)
        
        val reduction = quickForgeReduction(config.quickForgeLevel)
        val effectiveDuration = (recipe.durationSeconds * (1.0 - reduction)).toInt()
        
        val profit = sellValue - totalCost
        val profitPerHour = if (effectiveDuration > 0) {
            (profit / effectiveDuration) * 3600
        } else {
            profit
        }

        Log.debug(this, "Result: Profit=$profit, Profit/hr=$profitPerHour, Duration=${effectiveDuration}s")

        return ForgeResult(
            recipe,
            sellValue,
            totalCost,
            profitPerHour,
            sellPrice.isBazaar,
            hasBazaar,
            if (sellPrice.isBazaar) config.bazaarTax else calculateAhTax(sellPrice.price, config),
            effectiveDuration
        )
    }

    private fun calculateSellValue(price: PriceResult, config: ForgeConfig): Double {
        val taxRate = if (price.isBazaar) {
            config.bazaarTax
        } else {
            calculateAhTax(price.price, config)
        }
        return price.price * (1.0 - taxRate / 100.0) * config.slots
    }

    private fun calculateAhTax(price: Double, config: ForgeConfig): Double {
        val baseTax = when {
            price < 1_000_000 -> 1.0
            price < 10_000_000 -> 2.0
            price < 100_000_000 -> 3.0
            else -> 3.5
        }
        return baseTax * config.ahMultiplier
    }

    private fun calculateTotalCost(
        recipe: ForgeRecipe,
        ingredientPrices: Map<String, PriceResult>,
        config: ForgeConfig
    ): Double {
        var cost = (recipe.coinCost ?: 0).toDouble()
        recipe.ingredients.forEach { ingredient ->
            val price = ingredientPrices[ingredient.itemId]?.price ?: 0.0
            cost += price * ingredient.quantity
        }
        return cost * config.slots
    }

    private fun quickForgeReduction(level: Int) = when {
        level <= 0 -> 0.0
        level >= 20 -> 0.30
        else -> 0.10 + (level * 0.005)
    }
}
