package ui.kat

import business.kat.*
import calculator.BaseCalculator
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import kotlinx.coroutines.*
import utils.*

class KatCalculator(priceFetcher: PriceFetcher) : BaseCalculator(priceFetcher) {
    private val listView = KatListView { familyName ->
        KatBlacklistManager.toggleBlacklist(familyName)
        applySortingAndFiltering()
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val taskQueue = TaskQueue(scope)

    private val timeReducer = KatTimeReducer()
    private var rawResults = listOf<KatFamilyResult>()
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
            taskQueue.submitUnique("SEARCH") {
                delay(300) // Default search debounce
                applySortingAndFiltering()
            }
        }
    )

    private val refreshButton = "Refresh Prices".button(
        onClick = { 
            Log.debug(this, "Manual price refresh triggered")
            refreshData(includeSales = false) 
        }
    )

    private val refreshCraftsButton = "Refresh Crafts".button(
        onClick = {
            Log.debug(this, "Manual craft cache clear and refresh triggered")
            KatCalculations.recipeFetcher.clearCache()
            refreshData(includeSales = false)
        }
    )

    private val refreshSalesButton = "Refresh Sales".button(
        onClick = {
            Log.debug(this, "Manual sales refresh triggered")
            priceFetcher.clearSalesCache()
            startSalesFetch()
        }
    )

    override fun createContent() = listView.node.apply {
        // Remove border
        styleClass.add("edge-to-edge") 
    }

    override fun createTopBar(onBack: () -> Unit) = super.createTopBar(onBack).apply {
        children.add(0, "Kat Calculator".label(color = Styles.ACCENT, size = 18, bold = true))
    }

    override fun createControls() = hbox(20.0) {
        padding = Insets(12.0)
        style = "-fx-background-color: ${Styles.DARK_BG}; -fx-border-color: #333333; -fx-border-width: 0 0 1 0;"

        children.addAll(
            vbox(2.0) {
                children.addAll("Search".label(color = "#888888", size = 10, bold = true), searchField)
            },
            vbox(2.0) {
                children.addAll("Sort".label(color = "#888888", size = 10, bold = true), sortByComboBox)
            },
            vbox(2.0) {
                children.addAll("Rarity".label(color = "#888888", size = 10, bold = true), rarityFilterComboBox)
            },
            vbox(2.0) {
                children.addAll("Market".label(color = "#888888", size = 10, bold = true), buyModeComboBox)
            },
            vbox(2.0) {
                children.addAll("BZ % / AH x".label(color = "#888888", size = 10, bold = true), hbox(5.0) { children.addAll(bazaarTaxField, ahTaxMultiplierField) })
            },
            
            // Center: Spacer to push buttons to the right
            spacer(),

            // Right Side: Action Buttons
            vbox(2.0) {
                alignment = Pos.BOTTOM_LEFT
                children.add(hbox(8.0) {
                    children.addAll(refreshButton, refreshCraftsButton, refreshSalesButton)
                })
            }
        )

        // Start data refresh after UI is created
        Platform.runLater {
            refreshData()
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
        Log.debug(this, "Starting progress polling coroutine")
        scope.launch {
            while (isActive) {
                val progress = KatCalculations.recipeFetcher.getProgress()
                val hasPending = KatCalculations.recipeFetcher.hasPendingRequests()
                
                if (hasPending || progress < 1.0) {
                    Log.debug(this@KatCalculator, "Craft Progress: ${String.format("%.2f", progress)} (Pending: $hasPending)")
                }
                
                Platform.runLater {
                    // Exclusive visibility: Craft hides Sales
                    if (hasPending && progress < 1.0) {
                        craftProgressBar.updateProgress(progress, "Fetching Recipes... ${String.format("%.0f%%", progress * 100)}")
                        if (!craftProgressBar.isVisible) {
                            salesProgressBar.isVisible = false
                            craftProgressBar.isVisible = true
                            Log.debug(this@KatCalculator, "Showing craftProgressBar (hiding salesProgressBar)")
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
                        Log.debug(this@KatCalculator, "Hiding craftProgressBar after completion")
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
        val marketProfit = endSales * card.expectedProfit
        
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
        
        taskQueue.submit("RECALCULATE") {
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

    private fun refreshData(includeSales: Boolean = true) {
        taskQueue.submit("REFRESH") {
            Log.debug(this@KatCalculator, "Starting data refresh (includeSales=$includeSales)")
            Platform.runLater { loading.set(true) }
            KatCalculations.recipeFetcher.resetProgress()
            isRecipeFetching = true

            try {
                Log.debug(this@KatCalculator, "Fetching all prices...")
                priceFetcher.fetchAllPrices(force = true)
                
                // Load data (this might do network call if file missing)
                val families = KatDataLoader.load()
                Log.debug(this@KatCalculator, "Processing ${families.size} families")

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
                    loading.set(false)
                }

                // Sequential Fetching: 1. Recipes (handled by calculateUpgradeCard calls)
                // 2. Wait for Recipes to finish, then start Sales (if requested)
                Log.debug(this@KatCalculator, "Waiting for pending recipe requests...")
                while (KatCalculations.recipeFetcher.hasPendingRequests()) {
                    delay(500)
                }
                
                isRecipeFetching = false
                
                if (includeSales) {
                    Log.debug(this@KatCalculator, "All recipes fetched, starting sales fetch")
                    priceFetcher.clearSalesCache()
                    startSalesFetch()
                } else {
                    Log.debug(this@KatCalculator, "All recipes fetched, skipping sales fetch as requested")
                }
            } catch (e: Exception) {
                Log.debug(this@KatCalculator, "Critical failure during refreshData", e)
                isRecipeFetching = false
                Platform.runLater { loading.set(false) }
            }
        }
    }

    private fun updateFamilyResult(family: KatFamily) {
        taskQueue.submit("DATA_UPDATE_${family.name}") {
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
        taskQueue.submitUnique("UI_UPDATE") {
            delay(KatUIConfig.uiUpdateDebounceMs) // Debounce UI updates slightly to batch rapid changes
            applySortingAndFiltering()
        }
    }

    private fun startSalesFetch() {
        taskQueue.submit("SALES") {
            // Get unique base IDs for pets
            val baseIdsToFetch = rawResults.flatMap { familyResult ->
                familyResult.upgradeCards.map { card ->
                    card.recipe.itemTag.split(";")[0]
                }
            }.distinct()

            if (baseIdsToFetch.isEmpty()) return@submit

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

    private fun applySortingAndFiltering() {
        if (rawResults.isEmpty()) return

        val sortBy = sortByComboBox.value
        val rarityFilter = rarityFilterComboBox.value
        val searchText = searchField.text?.lowercase() ?: ""

        taskQueue.submitUnique("SORT_FILTER") {
            // Update exclusion status first
            val currentResults = rawResults.map { it.copy(isExcluded = KatBlacklistManager.isBlacklisted(it.family.name)) }

            // Apply family name filtering
            val familyFilteredResults = if (searchText.isBlank()) {
                currentResults
            } else {
                val searchQueries = searchText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                currentResults.filter { result ->
                    val cleanName = result.family.name.lowercase().replace("_", " ")
                    val rawName = result.family.name.lowercase()
                    searchQueries.any { query -> cleanName.contains(query) || rawName.contains(query) }
                }
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
