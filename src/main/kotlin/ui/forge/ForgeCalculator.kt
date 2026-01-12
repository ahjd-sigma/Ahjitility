package ui.forge

import calculator.*
import business.forge.*
import utils.*
import javafx.scene.layout.*
import javafx.scene.control.*
import javafx.collections.FXCollections
import javafx.application.Platform
import kotlinx.coroutines.*
import javafx.scene.Node
import javafx.scene.text.TextFlow
import javafx.geometry.Insets

class ForgeCalculator(priceFetcher: PriceFetcher) : BaseCalculator(priceFetcher) {
    private val controls = ForgeControls()
    private val recipes: List<ForgeRecipe> = ResourceLoader.load<List<ForgeRecipe>>(ForgeModuleConfig.recipesPath) ?: emptyList()
    
    // Using IO dispatcher for background work
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun createContent(): Region = SplitPane().apply {
        // Use the class property listView
        
        val sidebar = RecipeSidebar(priceFetcher)
        val sidebarScroll = ScrollPane(sidebar.node).apply {
            applyDarkStyle()
            enableAdvancedScrolling({ GeneralConfig.forgeScrollMultiplier })
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        }
        
        listView.onSelectionChanged { sidebar.showRecipe(it, controls.toConfig().sourcePriority) }
        
        items.addAll(listView.listView, sidebarScroll)
        setDividerPositions(ForgeUIConfig.dividerPosition)
        
        // Initial Calculation
        recalculate()
    }

    override fun createControls(): Region = controls.node.apply {
        controls.onChange { recalculate() }
        controls.onRefresh { 
             scope.launch {
                 Platform.runLater { loading.set(true) }
                 priceFetcher.fetchAllPrices(force = true) // Force refresh
                 recalculate()
             }
        }
    }
    
    // Helper to access list view from controls callback. 
    // Since createContent creates listView locally, we need to store a reference or change structure.
    // Better to make listView a property.
    private val listView = GenericListView<ForgeResult>({ GeneralConfig.forgeScrollMultiplier }) { createForgeCell(it) }

    private fun recalculate(targetList: GenericListView<ForgeResult> = listView) {
        val config = controls.toConfig()
        Platform.runLater { loading.set(true) }
        
        scope.launch {
            try {
                // Ensure prices are fetched (cached or new)
                priceFetcher.fetchAllPrices()
                
                val calculatedResults = recipes
                    .filter { 
                        config.searchText.isBlank() || 
                        it.displayName.contains(config.searchText, ignoreCase = true) 
                    }
                    .map { recipe ->
                        val sellPrice = priceFetcher.getSellPrice(recipe.itemId, config.isInstantSell, config.sourcePriority)
                        val hasBazaar = priceFetcher.hasBazaarPrice(recipe.itemId)
                        val ingredientPrices = recipe.ingredients.associate { 
                            it.itemId to priceFetcher.getBuyPrice(it.itemId, config.isInstantBuy, config.sourcePriority) 
                        }
                        
                        ForgeCalculations.calculateProfit(recipe, sellPrice, ingredientPrices, config, hasBazaar)
                    }
                    .filter { result ->
                         config.durationFilter.matches(result.effectiveDuration)
                    }
                    .sortedWith { a, b ->
                         // 1. First, group by Source Availability IF a specific priority is chosen
                         if (config.sourcePriority != SourcePriority.ALL && a.hasBazaar != b.hasBazaar) {
                             when (config.sourcePriority) {
                                 SourcePriority.BAZAAR_FIRST -> if (a.hasBazaar) -1 else 1
                                 SourcePriority.AH_FIRST -> if (!a.hasBazaar) -1 else 1
                                 else -> 0 // Should not happen with the check above
                             }
                         } else {
                             // 2. Sort by the selected mode (Profit or Profit/Hr)
                             // This is used for "All Sources" OR within groups
                             when (config.sortMode) {
                                 SortMode.PROFIT -> b.profit.compareTo(a.profit)
                                 SortMode.PROFIT_PER_HOUR -> b.profitPerHour.compareTo(a.profitPerHour)
                             }
                         }
                    }

                Platform.runLater {
                    targetList.items = calculatedResults
                    loading.set(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Platform.runLater { loading.set(false) }
            }
        }
    }

    private fun createForgeCell(item: ForgeResult): Node = VBox().apply {
        val profit = item.profit
        val hasPrice = item.sellValue > 0
        
        val textFlow = TextFlow(
            text(if (item.isBazaar) "[BZ] " else "[AH] ", if (item.isBazaar) "cyan" else "pink"),
            text("${item.recipe.displayName} ", Styles.TEXT_COLOR, bold = true),
            text("- ", Styles.TEXT_COLOR),
            if (hasPrice) text("${String.format("%,.0f", item.sellValue)} ", "orange")
            else text("Price N/A ", Styles.TEXT_COLOR),
            
            if (hasPrice && item.totalCost > 0) {
                val color = if (profit >= 0) "green" else "red"
                val sign = if (profit >= 0) "+" else ""
                text("($sign${String.format("%,.0f", profit)}) ", color)
            } else text("(N/A) ", "#666666"),

            text("- ", "#666666"),

            if (item.profitPerHour != 0.0) {
                val color = if (item.profitPerHour >= 0) "green" else "red"
                val sign = if (item.profitPerHour >= 0) "+" else ""
                text("[$sign${String.format("%,.0f", item.profitPerHour)}/hr] ", color, bold = true)
            } else text("[N/A/hr] ", "#666666"),

            text("(${formatDuration(item.effectiveDuration)})", "cyan", 11.0)
        ).apply { padding = Insets(8.0, 10.0, 8.0, 10.0) }

        children.addAll(textFlow, Separator().apply { style = "-fx-background-color: #333333; -fx-opacity: 0.3;" })
    }
}
