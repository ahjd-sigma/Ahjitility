package business.kat

import utils.PriceFetcher
import utils.RecipeFetcher
import utils.KatConfig
import utils.GeneralConfig
import utils.PriceResult
import kotlin.math.max

object KatCalculations {
    val recipeFetcher = RecipeFetcher()
    private val nextRarity get() = KatConfig.nextRarity
    private val rarityNumbers get() = KatConfig.rarityNumbers

    private val itemNameMappings get() = KatConfig.itemNameMappings
    private val itemIdMappings get() = KatConfig.itemIdMappings

    fun getMappedId(id: String): String {
        val parts = id.split(KatConfig.itemTagSeparator)
        val baseId = parts[0]
        val sanitizedBase = baseId.replace(KatConfig.space, KatConfig.underscore)
        val mappedBase = itemIdMappings[baseId] ?: itemIdMappings[sanitizedBase] ?: sanitizedBase
        return if (parts.size > 1) "$mappedBase${KatConfig.itemTagSeparator}${parts[1]}" else mappedBase
    }

    private fun getItemDisplayName(id: String): String {
        val baseId = id.split(KatConfig.itemTagSeparator)[0]
        return itemNameMappings[baseId] ?: baseId.replace(KatConfig.underscore, KatConfig.space).lowercase().split(KatConfig.space).joinToString(KatConfig.space) { it.replaceFirstChar { char -> char.uppercase() } }
    }

    private fun calculateTaxedPrice(id: String, priceFetcher: PriceFetcher, bazaarTax: Double, ahMultiplier: Double, isBuy: Boolean = false): Pair<Double, PriceResult> {
        val priceResult = if (isBuy) priceFetcher.getBuyPrice(id, true) else priceFetcher.getSellPrice(id, false)
        val rawPrice = priceResult.price
        val taxRate = if (priceResult.isBazaar) bazaarTax else calculateAhTax(rawPrice, ahMultiplier)
        return Pair(rawPrice * (1.0 - taxRate / 100.0), priceResult)
    }

    private fun getMaterialsBreakdown(materials: Map<String, Int>, priceFetcher: PriceFetcher, isBazaarInstant: Boolean): List<MaterialCost> {
        return materials.map { (matId, amount) ->
            val mappedMatId = getMappedId(matId)
            val priceResult = priceFetcher.getBuyPrice(mappedMatId, isBazaarInstant)
            val unitPrice = priceResult.price
            MaterialCost(
                itemId = matId,
                quantity = amount,
                unitPrice = unitPrice,
                totalPrice = unitPrice * amount,
                displayName = getItemDisplayName(matId),
                isBazaar = priceResult.isBazaar
            )
        }
    }

    private fun getReductionPrices(priceFetcher: PriceFetcher, isBazaarInstant: Boolean): Pair<Double, Double> {
        var flowerPrice = priceFetcher.getBuyPrice(KatConfig.katFlowerId, isBazaarInstant).price
        if (flowerPrice <= 0) flowerPrice = KatConfig.defaultFlowerPrice
        
        var bouquetPrice = priceFetcher.getBuyPrice(KatConfig.katBouquetId, isBazaarInstant).price
        if (bouquetPrice <= 0) bouquetPrice = KatConfig.defaultBouquetPrice
        
        return Pair(flowerPrice, bouquetPrice)
    }

    fun calculate(
        recipe: KatRecipe,
        priceFetcher: PriceFetcher,
        bazaarTax: Double = KatConfig.bazaarTax,
        ahMultiplier: Double = KatConfig.ahMultiplier
    ): KatResult {
        val startRarity = recipe.baseRarity.uppercase()
        val endRarity = nextRarity[startRarity] ?: KatConfig.unknownRarity

        val petName = recipe.itemTag.split(KatConfig.itemTagSeparator)[0]
        val startRarityNum = rarityNumbers[startRarity] ?: 0
        val endRarityNum = rarityNumbers[endRarity] ?: 0

        val startId = getMappedId("$petName${KatConfig.itemTagSeparator}$startRarityNum")
        val endId = getMappedId("$petName${KatConfig.itemTagSeparator}$endRarityNum")

        val startPrice = priceFetcher.getBuyPrice(startId, true).price
        val (endPrice, _) = calculateTaxedPrice(endId, priceFetcher, bazaarTax, ahMultiplier)

        val materialCost = recipe.materials.entries.sumOf { (matId, amount) ->
            val mappedMatId = getMappedId(matId)
            priceFetcher.getBuyPrice(mappedMatId, false).price * amount
        }

        val totalCost = recipe.cost + materialCost + startPrice
        val profit = endPrice - totalCost
        val profitPerHour = if (recipe.hours > 0) profit / recipe.hours else profit

        return KatResult(
            recipe = recipe,
            startPrice = startPrice,
            endPrice = endPrice,
            materialCost = materialCost,
            profit = profit,
            profitPerHour = profitPerHour
        )
    }

    fun calculateFamily(
        family: KatFamily,
        priceFetcher: PriceFetcher,
        bazaarTax: Double = KatConfig.bazaarTax,
        ahMultiplier: Double = KatConfig.ahMultiplier
    ): KatFamilyResult {
        val results = family.recipes.map { calculate(it, priceFetcher, bazaarTax, ahMultiplier) }
        return KatFamilyResult(family, results)
    }

    fun calculateUpgradeCard(
        recipe: KatRecipe,
        family: KatFamily,
        priceFetcher: PriceFetcher,
        timeReducer: KatTimeReducer = KatTimeReducer(),
        isBazaarInstant: Boolean = KatConfig.defaultBazaarInstant,
        bazaarTax: Double = KatConfig.bazaarTax,
        ahMultiplier: Double = KatConfig.ahMultiplier,
        onRecipeAvailable: (() -> Unit)? = null
    ): List<KatUpgradeCard> {
        val cards = mutableListOf<KatUpgradeCard>()

        val petName = recipe.itemTag.split(KatConfig.itemTagSeparator)[0]
        val startRarity = recipe.baseRarity.uppercase()
        val endRarity = nextRarity[startRarity] ?: KatConfig.unknownRarity
        val startRarityNum = rarityNumbers[startRarity] ?: 0
        val endRarityNum = rarityNumbers[endRarity] ?: 0

        // 1. Craft from Scratch (if it's the first recipe and is COMMON)
        val isFirstRecipe = family.recipes.indexOf(recipe) == 0 && startRarity == KatConfig.rarities.first()
        if (isFirstRecipe) {
            val materials = recipeFetcher.getCraftingMaterials(petName, 0)
            if (materials == null && onRecipeAvailable != null) {
                recipeFetcher.fetchRecipeInBackground(petName, 0, onRecipeAvailable)
            }

            if (materials != null) {
                val craftEndId = getMappedId("$petName${KatConfig.itemTagSeparator}0")
                val materialsBreakdown = getMaterialsBreakdown(materials, priceFetcher, isBazaarInstant)
                val totalMaterialCost = materialsBreakdown.sumOf { it.totalPrice }
                
                // End price after tax
                val (craftEndPrice, craftEndPriceResult) = calculateTaxedPrice(craftEndId, priceFetcher, bazaarTax, ahMultiplier)
                val craftRawEndPrice = craftEndPriceResult.price

                val hasUnknownPrices = materialsBreakdown.any { it.unitPrice <= 0 } || craftRawEndPrice <= 0
                val unknownPriceItems = materialsBreakdown.filter { it.unitPrice <= 0 }.map { it.itemId }.toMutableList()
                if (craftEndPrice <= 0) unknownPriceItems.add(craftEndId)

                cards.add(KatUpgradeCard(
                    recipe = recipe,
                    startRarity = KatConfig.craftRarity,
                    endRarity = KatConfig.rarities.first(),
                    startPrice = 0.0,
                    startPriceSource = PriceSource.CRAFT,
                    endPrice = craftEndPrice,
                    endPriceSource = PriceSource.AH,
                    baseDuration = 0.0,
                    reducedDuration = 0.0,
                    materialsBreakdown = materialsBreakdown,
                    flowerCount = 0,
                    bouquetCount = 0,
                    previousTierCost = 0.0,
                    previousTierSource = PriceSource.CRAFT,
                    totalMaterialCost = totalMaterialCost,
                    totalFlowerCost = 0.0,
                    totalBouquetCost = 0.0,
                    totalCost = totalMaterialCost,
                    expectedProfit = craftEndPrice - totalMaterialCost,
                    profitMargin = if (totalMaterialCost > 0) (craftEndPrice - totalMaterialCost) / totalMaterialCost else 0.0,
                    hasUnknownPrices = hasUnknownPrices,
                    unknownPriceItems = unknownPriceItems,
                    isCraftOnly = true
                ))
            }
        }

        // 2. Normal Upgrade
        val startId = getMappedId("$petName${KatConfig.itemTagSeparator}$startRarityNum")
        val endId = getMappedId("$petName${KatConfig.itemTagSeparator}$endRarityNum")

        // Check recursive price for previous tier
        val (startPrice, startPriceSource) = getPetPriceRecursive(petName, startRarity, family, priceFetcher, onRecipeAvailable, timeReducer, isBazaarInstant, bazaarTax, ahMultiplier)
        
        val (endPrice, endPriceResult) = calculateTaxedPrice(endId, priceFetcher, bazaarTax, ahMultiplier)
        val rawEndPrice = endPriceResult.price

        val materialsBreakdown = getMaterialsBreakdown(recipe.materials, priceFetcher, isBazaarInstant)
        val materialCost = materialsBreakdown.sumOf { it.totalPrice }

        // Time reduction
        val (flowerPrice, bouquetPrice) = getReductionPrices(priceFetcher, isBazaarInstant)
        val reduction = timeReducer.calculateOptimalReduction(recipe.hours, flowerPrice, bouquetPrice)

        val totalFlowerCost = reduction.flowerCount * flowerPrice
        val totalBouquetCost = reduction.bouquetCount * bouquetPrice
        val totalCost = recipe.cost + materialCost + startPrice + totalFlowerCost + totalBouquetCost
        
        val expectedProfit = endPrice - totalCost
        val hasUnknownPrices = materialsBreakdown.any { it.unitPrice <= 0 } || startPrice <= 0 || rawEndPrice <= 0 || (reduction.flowerCount > 0 && flowerPrice <= 0) || (reduction.bouquetCount > 0 && bouquetPrice <= 0)
        val unknownPriceItems = materialsBreakdown.filter { it.unitPrice <= 0 }.map { it.itemId }.toMutableList()
        if (startPrice <= 0) unknownPriceItems.add(startId)
        if (endPrice <= 0) unknownPriceItems.add(endId)
        if (reduction.flowerCount > 0 && flowerPrice <= 0) unknownPriceItems.add(KatConfig.katFlowerId)
        if (reduction.bouquetCount > 0 && bouquetPrice <= 0) unknownPriceItems.add(KatConfig.katBouquetId)

        cards.add(KatUpgradeCard(
            recipe = recipe,
            startRarity = startRarity,
            endRarity = endRarity,
            startPrice = startPrice,
            startPriceSource = startPriceSource,
            endPrice = endPrice,
            endPriceSource = PriceSource.AH,
            baseDuration = recipe.hours,
            reducedDuration = reduction.finalDuration,
            materialsBreakdown = materialsBreakdown,
            flowerCount = reduction.flowerCount,
            bouquetCount = reduction.bouquetCount,
            previousTierCost = startPrice,
            previousTierSource = startPriceSource,
            totalMaterialCost = materialCost,
            totalFlowerCost = totalFlowerCost,
            totalBouquetCost = totalBouquetCost,
            totalCost = totalCost,
            expectedProfit = expectedProfit,
            profitMargin = if (totalCost > 0) expectedProfit / totalCost else 0.0,
            hasUnknownPrices = hasUnknownPrices,
            unknownPriceItems = unknownPriceItems,
            isCraftOnly = false
        ))

        return cards
    }

    private fun calculateAhTax(price: Double, multiplier: Double): Double {
        val thresholds = KatConfig.ahTaxThresholds
        val rates = KatConfig.ahTaxRates
        
        var baseTax = rates.last()
        for (i in thresholds.indices) {
            if (price < thresholds[i]) {
                baseTax = rates[i]
                break
            }
        }
        return baseTax * multiplier
    }

    private fun getPetPriceRecursive(
        petName: String,
        rarity: String,
        family: KatFamily,
        priceFetcher: PriceFetcher,
        onRecipeAvailable: (() -> Unit)? = null,
        timeReducer: KatTimeReducer = KatTimeReducer(),
        isBazaarInstant: Boolean = KatConfig.defaultBazaarInstant,
        bazaarTax: Double = KatConfig.bazaarTax,
        ahMultiplier: Double = KatConfig.ahMultiplier
    ): Pair<Double, PriceSource> {
        val rarityNum = rarityNumbers[rarity] ?: 0
        val petId = getMappedId("$petName${KatConfig.itemTagSeparator}$rarityNum")
        val ahPrice = priceFetcher.getBuyPrice(petId, true).price
        
        // If it's the first rarity, we can also craft it
        if (rarity == KatConfig.rarities.first()) {
            val materials = recipeFetcher.getCraftingMaterials(petName, 0)
            if (materials == null && onRecipeAvailable != null) {
                recipeFetcher.fetchRecipeInBackground(petName, 0, onRecipeAvailable)
            }

            if (materials != null) {
                val craftPrice = getMaterialsBreakdown(materials, priceFetcher, isBazaarInstant).sumOf { it.totalPrice }
                
                return if (ahPrice > 0 && ahPrice < craftPrice) {
                    Pair(ahPrice, PriceSource.AH)
                } else if (craftPrice > 0) {
                    Pair(craftPrice, PriceSource.CRAFT)
                } else {
                    Pair(ahPrice, PriceSource.AH)
                }
            }
            return Pair(ahPrice, PriceSource.AH)
        }

        // If not common, check if we should buy from AH or upgrade from previous tier
        val rarities = KatConfig.rarities
        val currentIdx = rarities.indexOf(rarity)
        if (currentIdx <= 0) return Pair(ahPrice, PriceSource.AH)

        val prevRarity = rarities[currentIdx - 1]
        val prevRecipe = family.recipes.find { it.baseRarity.uppercase() == prevRarity } ?: return Pair(ahPrice, PriceSource.AH)
        
        val prevPrice = getPetPriceRecursive(petName, prevRarity, family, priceFetcher, onRecipeAvailable, timeReducer, isBazaarInstant, bazaarTax, ahMultiplier).first
        if (prevPrice <= 0) return Pair(ahPrice, PriceSource.AH)

        val materialCost = getMaterialsBreakdown(prevRecipe.materials, priceFetcher, isBazaarInstant).sumOf { it.totalPrice }

        // Include flower/bouquet prices in the recursive calculation
        val (flowerPrice, bouquetPrice) = getReductionPrices(priceFetcher, isBazaarInstant)
        
        val reduction = timeReducer.calculateOptimalReduction(prevRecipe.hours, flowerPrice, bouquetPrice)
        val reductionCost = (reduction.flowerCount * flowerPrice) + (reduction.bouquetCount * bouquetPrice)

        val upgradePrice = prevPrice + prevRecipe.cost + materialCost + reductionCost
        
        return if (ahPrice > 0 && ahPrice < upgradePrice) {
            Pair(ahPrice, PriceSource.AH)
        } else {
            Pair(upgradePrice, PriceSource.CRAFT)
        }
    }

}
