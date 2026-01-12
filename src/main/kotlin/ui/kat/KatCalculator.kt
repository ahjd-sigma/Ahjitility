package ui.kat

import calculator.BaseCalculator
import business.kat.*
import utils.*
import javafx.scene.layout.VBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.Region
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.application.Platform
import kotlinx.coroutines.*

class KatCalculator(priceFetcher: PriceFetcher) : BaseCalculator(priceFetcher) {
    private val listView = KatListView { familyName ->
        KatBlacklistManager.toggleBlacklist(familyName)
        applySortingAndFiltering()
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var refreshJob: Job? = null
    private var uiUpdateJob: Job? = null
    private var salesJob: Job? = null

    private val timeReducer = KatTimeReducer()
    private var rawResults = listOf<KatFamilyResult>()
    private var salesProgress = 0.0
    private var isRecipeFetching = false

    // Overlay Progress Bars
    private val craftProgressBar = stylizedProgressBar(GeneralConfig.colorAccentBlue).apply {
        isVisible = false
    }
    private val salesProgressBar = stylizedProgressBar(GeneralConfig.colorAccentOrange).apply {
        isVisible = false
    }

    // Sorting and filtering controls
    private val sortByComboBox = comboBox(
        items = listOf("Profit Margin (High to Low)", "Profit Margin (Low to High)", "Profit Amount", "Market Profit/hr", "Sales/hr", "All Cards"),
        defaultValue = "Profit Margin (High to Low)",
        width = KatUIConfig.sortByWidth,
        onChange = { applySortingAndFiltering() }
    )

    private val rarityFilterComboBox = comboBox(
        items = listOf("All Rarities", "Mythic", "Legendary", "Epic", "Rare", "Uncommon", "Common"),
        defaultValue = "All Rarities",
        width = KatUIConfig.rarityFilterWidth,
        onChange = { applySortingAndFiltering() }
    )

    private val buyModeComboBox = comboBox(
        items = listOf("Instant Buy", "Buy Order"),
        defaultValue = if (KatConfig.defaultBazaarInstant) "Instant Buy" else "Buy Order",
        width = KatUIConfig.buyModeWidth,
        onChange = { recalculateResults() }
    )

    private val bazaarTaxField = textField(
        text = KatConfig.bazaarTax.toString(),
        width = KatUIConfig.taxFieldWidth,
        onChange = { recalculateResults() }
    )

    private val ahTaxMultiplierField = textField(
        text = KatConfig.ahMultiplier.toString(),
        width = KatUIConfig.taxFieldWidth,
        onChange = { recalculateResults() }
    )

    private val isBazaarInstant get() = buyModeComboBox.value == "Instant Buy"
    private val bazaarTax get() = bazaarTaxField.text.toDoubleOrNull() ?: KatConfig.bazaarTax
    private val ahMultiplier get() = ahTaxMultiplierField.text.toDoubleOrNull() ?: KatConfig.ahMultiplier

    private val searchField = textField(
        prompt = "Search by family name...",
        width = KatUIConfig.searchFieldWidth,
        onChange = { 
            // Debounce search input
            refreshJob?.cancel()
            refreshJob = scope.launch {
                delay(300) // Default search debounce
                applySortingAndFiltering()
            }
        }
    )

    private val refreshButton = button(
        text = "Refresh Prices",
        onClick = { refreshData(force = true) }
    )

    private val refreshCraftsButton = button(
        text = "Refresh Crafts",
        onClick = {
            KatCalculations.recipeFetcher.clearCache()
            refreshData(force = true)
        }
    )

    override fun createContent() = listView.node.apply {
        // Remove border
        styleClass.add("edge-to-edge") 
    }

    override fun createTopBar(onBack: () -> Unit) = super.createTopBar(onBack).apply {
        children.addAll(
            separator(),
            label(
                text = "Kat Calculator",
                color = KatUIConfig.labelColorPrimary,
                size = KatUIConfig.fontSizeTitle,
                bold = true
            )
        )
    }

    override fun createControls() = javafx.scene.layout.HBox(10.0).apply {
        padding = Insets(KatUIConfig.mainPadding)
        alignment = Pos.CENTER_LEFT
        style = "-fx-background-color: ${GeneralConfig.colorDarkBg}; -fx-border-color: ${GeneralConfig.colorDarkerBg}; -fx-border-width: 0 0 1 0;"

        searchField.prefWidth = 350.0 // Expanded search field

        children.addAll(
            "Search:".label(Styles.label),
            searchField,
            separator(),
            "Sort:".label(Styles.label), sortByComboBox,
            "Rarity:".label(Styles.label), rarityFilterComboBox,
            "Buy:".label(Styles.label), buyModeComboBox,
            "BZ %:".label(Styles.label), bazaarTaxField,
            "AH x:".label(Styles.label), ahTaxMultiplierField,
            
            // Center: Spacer to push buttons to the right
            spacer(),

            // Right Side: Action Buttons
            refreshButton,
             refreshCraftsButton
         )

         // Start data refresh after UI is created
         Platform.runLater {
             refreshData(force = true)
             startProgressPolling()
         }
     }

    override fun createOverlay() = StackPane().apply {
        isMouseTransparent = true
        maxWidth = 300.0
        maxHeight = Region.USE_COMPUTED_SIZE
        isPickOnBounds = false
        
        translateY = -60.0
        
        children.addAll(
            craftProgressBar,
            salesProgressBar
        )
    }

    private fun startProgressPolling() {
        println("[DEBUG] KatCalculator: Starting progress polling coroutine")
        scope.launch {
            while (isActive) {
                val progress = KatCalculations.recipeFetcher.getProgress()
                val hasPending = KatCalculations.recipeFetcher.hasPendingRequests()
                
                if (hasPending || progress < 1.0) {
                    println("[DEBUG] KatCalculator: Craft Progress: ${String.format("%.2f", progress)} (Pending: $hasPending)")
                }
                
                Platform.runLater {
                    // Exclusive visibility: Craft hides Sales
                    if (hasPending && progress < 1.0) {
                        craftProgressBar.updateProgress(progress, "Fetching Recipes... ${String.format("%.0f%%", progress * 100)}")
                        if (!craftProgressBar.isVisible) {
                            salesProgressBar.isVisible = false
                            craftProgressBar.isVisible = true
                            println("[DEBUG] KatCalculator: Showing craftProgressBar (hiding salesProgressBar)")
                        }
                    } else if (progress >= 1.0 && !hasPending) {
                        if (craftProgressBar.isVisible) {
                            craftProgressBar.updateProgress(1.0, "Recipes Loaded!")
                        }
                    }
                }
                
                if (progress >= 1.0 && !hasPending && craftProgressBar.isVisible) {
                    delay(1500)
                    Platform.runLater { 
                        println("[DEBUG] KatCalculator: Hiding craftProgressBar after completion")
                        craftProgressBar.isVisible = false
                        // Sales fetch usually starts after this, and it will handle its own visibility
                    }
                }
                delay(KatUIConfig.progressPollingMs)
            }
        }
    }

    private fun fetchSalesData(card: KatUpgradeCard): KatUpgradeCard {
        val baseName = card.recipe.itemTag.split(";")[0]
        
        // End Sales
        val endId = getPetRarityId(baseName, if (card.isCraftOnly) null else card.endRarity)
        val endSales = priceFetcher.fetchHourlySales(endId)
        val marketProfit = if (endSales != null) endSales * card.expectedProfit else null
        
        // Start Sales (N/A for craft only)
        val startSales = if (card.isCraftOnly) null else {
            priceFetcher.fetchHourlySales(getPetRarityId(baseName, card.startRarity))
        }

        return card.copy(startHourlySales = startSales, endHourlySales = endSales, expectedHourlyMarketProfit = marketProfit)
    }

    private fun getPetRarityId(baseName: String, rarity: String?): String {
        val rarityNum = if (rarity == null) 0 else (KatConfig.rarityNumbers[rarity.uppercase()] ?: 0)
        return KatCalculations.getMappedId("$baseName;$rarityNum")
    }

    private fun recalculateResults() {
        if (rawResults.isEmpty()) return
        
        scope.launch {
            rawResults = rawResults.map { familyResult ->
                val updatedCards = familyResult.family.recipes.flatMap { recipe ->
                    KatCalculations.calculateUpgradeCard(
                        recipe, familyResult.family, priceFetcher, timeReducer, isBazaarInstant,
                        bazaarTax, ahMultiplier
                    )
                }.map { card -> 
                    // Preserve sales data if already fetched
                    fetchSalesData(card)
                }
                familyResult.copy(upgradeCards = updatedCards)
            }
            triggerUIUpdate()
        }
    }

    private fun refreshData(force: Boolean = false) {
        if (force) {
            Platform.runLater { loading.set(true) }
            KatCalculations.recipeFetcher.resetProgress()
            isRecipeFetching = true
        }
        scope.launch {
            try {
                if (force) priceFetcher.fetchAllPrices(force = true)
                
                // Load data (this might do network call if file missing)
                val families = KatDataLoader.load()

                val results = families.map { family ->
                    val upgradeCards = family.recipes.flatMap { recipe ->
                        KatCalculations.calculateUpgradeCard(
                            recipe, family, priceFetcher, timeReducer, isBazaarInstant,
                            bazaarTax, ahMultiplier, onRecipeAvailable = {
                                // Real-time update: Re-calculate just this family when its recipe arrives
                                updateFamilyResult(family)
                            }
                        )
                    }
                    KatFamilyResult(family, emptyList(), upgradeCards, KatBlacklistManager.isBlacklisted(family.name))
                }

                Platform.runLater {
                    rawResults = results
                    applySortingAndFiltering()
                    if (force) loading.set(false)
                }

                // Sequential Fetching: 1. Recipes (handled by calculateUpgradeCard calls)
                // 2. Wait for Recipes to finish, then start Sales
                if (force) {
                    while (KatCalculations.recipeFetcher.hasPendingRequests()) {
                        delay(500)
                    }
                    isRecipeFetching = false
                    startSalesFetch()
                }
            } catch (e: Exception) {
                if (force) {
                    isRecipeFetching = false
                    Platform.runLater { loading.set(false) }
                }
            }
        }
    }

    private fun updateFamilyResult(family: KatFamily) {
        scope.launch {
            val updatedCards = family.recipes.flatMap { recipe ->
                KatCalculations.calculateUpgradeCard(
                    recipe, family, priceFetcher, timeReducer, isBazaarInstant,
                    bazaarTax, ahMultiplier
                )
            }.map { card -> 
                // ONLY fetch sales data if we are NOT in the initial recipe fetching phase
                if (!isRecipeFetching) fetchSalesData(card) else card 
            }
            
            // Update rawResults
            rawResults = rawResults.map { 
                if (it.family.name == family.name) {
                    it.copy(upgradeCards = updatedCards)
                } else it
            }
            
            triggerUIUpdate()
        }
    }

    private fun triggerUIUpdate() {
        uiUpdateJob?.cancel()
        uiUpdateJob = scope.launch {
            delay(KatUIConfig.uiUpdateDebounceMs) // Debounce UI updates slightly to batch rapid changes
            applySortingAndFiltering()
        }
    }

    private fun startSalesFetch() {
        salesJob?.cancel()
        salesJob = scope.launch {
            // Get unique base IDs for pets
            val baseIdsToFetch = rawResults.flatMap { familyResult ->
                familyResult.upgradeCards.map { card ->
                    card.recipe.itemTag.split(";")[0]
                }
            }.distinct()

            if (baseIdsToFetch.isEmpty()) return@launch

            // Wait for craft progress bar to hide if it's currently showing
            while (craftProgressBar.isVisible) {
                delay(100)
            }

            Platform.runLater {
                salesProgressBar.updateProgress(0.0, "Fetching Sales Data...")
                salesProgressBar.isVisible = true
            }

            for ((index, baseId) in baseIdsToFetch.withIndex()) {
                // Fetching for one rarity will populate all rarities in the cache
                priceFetcher.fetchHourlySales("$baseId;0")
                
                // Update only relevant cards from the newly populated cache
                updateSalesForBaseId(baseId)
                
                // Update progress
                val progress = (index + 1).toDouble() / baseIdsToFetch.size
                
                Platform.runLater { 
                    salesProgressBar.updateProgress(progress, "Updating Sales... ${String.format("%.0f%%", progress * 100)}")
                }
                
                // Add delay between fetches to prevent API rate limiting
                delay(200) // Default sales fetch idle delay
            }

            Platform.runLater { salesProgressBar.updateProgress(1.0, "Sales Updated!") }
            delay(1500) // Keep visible for a bit
            Platform.runLater { 
                salesProgressBar.isVisible = false 
            }
        }
    }

    private fun updateSalesForBaseId(baseId: String) {
        rawResults = rawResults.map { familyResult ->
            val hasCardWithBaseId = familyResult.upgradeCards.any { 
                it.recipe.itemTag.startsWith(baseId) 
            }
            if (hasCardWithBaseId) {
                val updatedCards = familyResult.upgradeCards.map { card ->
                    if (card.recipe.itemTag.startsWith(baseId)) fetchSalesData(card) else card
                }
                familyResult.copy(upgradeCards = updatedCards)
            } else familyResult
        }
        triggerUIUpdate()
    }

    private fun updateAllSalesFromCache() {
        rawResults = rawResults.map { familyResult ->
            val updatedCards = familyResult.upgradeCards.map { card -> fetchSalesData(card) }
            familyResult.copy(upgradeCards = updatedCards)
        }
        triggerUIUpdate()
    }

    private fun applySortingAndFiltering() {
        if (rawResults.isEmpty()) return

        val sortBy = sortByComboBox.value
        val rarityFilter = rarityFilterComboBox.value
        val searchText = searchField.text?.lowercase() ?: ""

        scope.launch {
            // Update exclusion status first
            val currentResults = rawResults.map { it.copy(isExcluded = KatBlacklistManager.isBlacklisted(it.family.name)) }

            // Apply family name filtering
            val familyFilteredResults = if (searchText.isBlank()) {
                currentResults
            } else {
                currentResults.filter { it.family.name.lowercase().contains(searchText) }
            }

            // Get all upgrade cards from filtered families
            var allCards = familyFilteredResults.flatMap { it.upgradeCards }

            // Apply rarity filtering
            if (rarityFilter != "All Rarities") {
                allCards = allCards.filter { it.endRarity.equals(rarityFilter, ignoreCase = true) }
            }

            // Apply sorting
            val sortedCards = when (sortBy) {
                "Profit Margin (High to Low)" -> allCards.sortedByDescending { it.profitMargin }
                "Profit Margin (Low to High)" -> allCards.sortedBy { it.profitMargin }
                "Profit Amount" -> allCards.sortedByDescending { it.expectedProfit }
                "Market Profit/hr" -> allCards.sortedByDescending { it.expectedHourlyMarketProfit ?: -Double.MAX_VALUE }
                "Sales/hr" -> allCards.sortedByDescending { it.endHourlySales ?: -Double.MAX_VALUE }
                "All Cards" -> allCards
                else -> allCards
            }

            // Group cards back into families - show entire family for each card
            val familiesToShow = mutableListOf<String>()
            sortedCards.forEach { card ->
                val familyName = familyFilteredResults.find { it.upgradeCards.contains(card) }?.family?.name
                if (familyName != null && !familiesToShow.contains(familyName)) {
                    familiesToShow.add(familyName)
                }
            }

            // Final results: ordered by the card sorting, but with excluded families moved to the bottom
            val finalResults = familiesToShow.mapNotNull { name ->
                val familyResult = currentResults.find { it.family.name == name }
                if (familyResult != null && rarityFilter != "All Rarities") {
                    // Only show the specific rarity cards within the family
                    val filteredCards = familyResult.upgradeCards.filter { it.endRarity.equals(rarityFilter, ignoreCase = true) }
                    if (filteredCards.isEmpty()) null else familyResult.copy(upgradeCards = filteredCards)
                } else familyResult
            }.sortedBy { it.isExcluded }

            Platform.runLater {
                listView.updateItems(finalResults)
            }
        }
    }
}
