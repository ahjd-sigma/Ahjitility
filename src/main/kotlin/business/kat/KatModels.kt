package business.kat

data class KatRecipe(
    val name: String,
    val baseRarity: String,
    val hours: Double,
    val cost: Long,
    val materials: Map<String, Int>,
    var itemTag: String
)

data class KatFamily(
    val name: String,
    val recipes: List<KatRecipe>,
    val isFullFamily: Boolean
)

data class KatResult(
    val recipe: KatRecipe,
    val startPrice: Double,
    val endPrice: Double,
    val materialCost: Double,
    val profit: Double,
    val profitPerHour: Double
) {
    val totalCost = recipe.cost + materialCost + startPrice
}

data class KatFamilyResult(
    val family: KatFamily,
    val results: List<KatResult>,
    val upgradeCards: List<KatUpgradeCard> = emptyList(),
    val isExcluded: Boolean = false
) {
    val totalProfit = results.filter { it.profit > 0 }.sumOf { it.profit }
    val totalUpgradeProfit = upgradeCards.sumOf { it.expectedProfit } // New calculation
}

enum class PriceSource { AH, CRAFT, UNKNOWN }

data class KatUpgradeCard(
    val recipe: KatRecipe,
    val startRarity: String,
    val endRarity: String,
    val startPrice: Double,
    val startPriceSource: PriceSource = PriceSource.AH,
    val endPrice: Double,
    val endPriceSource: PriceSource = PriceSource.AH,
    val baseDuration: Double,
    val reducedDuration: Double,
    val materialsBreakdown: List<MaterialCost>,
    val flowerCount: Int,
    val bouquetCount: Int,
    val previousTierCost: Double,
    val previousTierSource: PriceSource = PriceSource.AH,
    val totalMaterialCost: Double,
    val totalFlowerCost: Double,
    val totalBouquetCost: Double,
    val totalCost: Double,
    val expectedProfit: Double,
    val profitMargin: Double,
    val hasUnknownPrices: Boolean = false,
    val unknownPriceItems: List<String> = emptyList(),
    val isCraftOnly: Boolean = false, // New field to indicate this is a "Craft from Scratch" card
    val startHourlySales: Double? = null,
    val endHourlySales: Double? = null,
    val expectedHourlyMarketProfit: Double? = null
)

data class MaterialCost(
    val itemId: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val displayName: String = itemId,
    val isBazaar: Boolean = false
)