package ui.forge

import business.forge.*
import calculator.BaseCalculator
import calculator.GenericListView
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.control.SplitPane
import javafx.scene.layout.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import utils.*

class ForgeCalculator(priceFetcher: PriceFetcher) : BaseCalculator(priceFetcher) {
    private val controls = ForgeControls()
    private val recipes: List<ForgeRecipe> = ResourceLoader.load<List<ForgeRecipe>>(ForgeModuleConfig.recipesPath) ?: emptyList()
    
    // Using IO dispatcher for background work
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun createTopBar(onBack: () -> Unit) = super.createTopBar(onBack).apply {
        children.add(0, "Forge Calculator".label(color = Styles.ACCENT, size = 18, bold = true))
    }

    override fun createContent(): Region = SplitPane().apply {
        // Use the class property listView
        
        val sidebar = RecipeSidebar(priceFetcher)
        val sidebarScroll = ScrollPane(sidebar.node).apply {
            applyDarkStyle()
            enableAdvancedScrolling { GeneralConfig.forgeScrollMultiplier }
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
                
                val searchQueries = config.searchText.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                
                val calculatedResults = recipes
                    .filter { recipe ->
                        if (searchQueries.isEmpty()) return@filter true
                        
                        val cleanName = recipe.displayName.lowercase().replace("_", " ")
                        val rawName = recipe.displayName.lowercase()
                        
                        searchQueries.any { query ->
                            cleanName.contains(query) || rawName.contains(query)
                        }
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
                Log.debug(this, "Forge recalculation failed", e)
                Platform.runLater { loading.set(false) }
            }
        }
    }

    private fun createForgeCell(item: ForgeResult): Node = hbox(10.0) {
        padding = Insets(10.0, 15.0, 10.0, 15.0)
        alignment = Pos.CENTER_LEFT
        val baseStyle = " -fx-border-color: transparent transparent #333333 transparent; -fx-border-width: 0 0 1 0; -fx-font-size: 13px;"
        style = baseStyle
        
        val profit = item.profit
        val hasPrice = item.sellValue > 0
        val profitColor = if (profit >= 0) "green" else "red"
        val profitSign = if (profit >= 0) "+" else ""

        // Left side: Source and Name
        children.add(vbox(2.0) {
            children.addAll(
                hbox(5.0) {
                    children.addAll(
                        (if (item.isBazaar) "[BZ]" else "[AH]").label(color = if (item.isBazaar) "cyan" else "pink", size = 10, bold = true),
                        item.recipe.displayName.label(color = Styles.TEXT_COLOR, size = 14, bold = true)
                    )
                },
                hbox(5.0) {
                    children.addAll(
                        "Duration: ".label(color = "#888888", size = 11),
                        formatDuration(item.effectiveDuration).label(color = "cyan", size = 11)
                    )
                }
            )
        })

        children.add(spacer())

        // Right side: Price and Profit
        children.add(vbox(2.0) {
            alignment = Pos.TOP_RIGHT
            children.addAll(
                hbox(5.0) {
                    alignment = Pos.CENTER_RIGHT
                    children.addAll(
                        "Profit: ".label(color = "#888888", size = 11),
                        if (hasPrice && item.totalCost > 0) 
                            "$profitSign${String.format("%,.0f", profit)}".label(color = profitColor, size = 13, bold = true)
                        else 
                            "N/A".label(color = "#666666", size = 13, bold = true)
                    )
                },
                hbox(5.0) {
                    alignment = Pos.CENTER_RIGHT
                    children.addAll(
                        if (item.profitPerHour != 0.0)
                            "$profitSign${String.format("%,.0f/hr", item.profitPerHour)}".label(color = profitColor, size = 11, bold = true)
                        else
                            "N/A/hr".label(color = "#666666", size = 11, bold = true)
                    )
                }
            )
        })

        // Hover effect
        setOnMouseEntered { style = "$baseStyle -fx-background-color: rgba(255, 255, 255, 0.05);" }
        setOnMouseExited { style = baseStyle }
    }
}
