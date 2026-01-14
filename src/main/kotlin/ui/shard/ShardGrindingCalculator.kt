package ui.shard

import business.shard.ShardCalculations
import business.shard.ShardDataLoader
import business.shard.ShardInfo
import calculator.BaseCalculator
import calculator.GenericListView
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import kotlinx.coroutines.*
import utils.*

class ShardGrindingCalculator(priceFetcher: PriceFetcher) : BaseCalculator(priceFetcher) {
    private val controls = ShardControls()
    private val listView = GenericListView<ShardInfo>({ GeneralConfig.shardScrollMultiplier }) { createShardCell(it) }
    private val sidebar = ShardSidebar()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun createTopBar(onBack: () -> Unit) = super.createTopBar(onBack).apply {
        children.add(0, "Shard Grinding Calculator".label(color = Styles.ACCENT, size = 18, bold = true))
    }

    private var allShards = emptyList<ShardInfo>()
    private var currentRates = mutableMapOf<String, Double>()
    private var currentChestPrices = mutableMapOf<String, Double>()
    private var currentBaitCounts = mutableMapOf<String, Double>()

    override fun createContent() = SplitPane(
        listView.listView,
        ScrollPane(sidebar.node).apply {
            applyDarkStyle()
            enableAdvancedScrolling { GeneralConfig.shardScrollMultiplier }
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        }
    ).apply {
        setDividerPositions(ShardUIConfig.dividerPosition)
    }

    override fun createControls() = controls.node.apply {
        controls.onChange { updateUI() }
        controls.onSellModeChange { refreshData() }
        controls.onRefresh { refreshData() }
    }

    init {
        listView.onSelectionChanged { shard ->
            sidebar.show(shard, { newRate -> 
                shard?.let { saveRate(it.shardId, newRate) }
            }, { newChestPrice ->
                shard?.let { saveChestPrice(it.displayName, newChestPrice) }
            }, { newBaitCount ->
                shard?.let { saveBaitCount(it.displayName, newBaitCount) }
            })
        }
        refreshData()
    }

    private fun refreshData() {
        Platform.runLater { loading.set(true) }
        scope.launch {
            try {
                Log.debug(this@ShardGrindingCalculator, "Starting refreshData")
                val rates = ShardDataLoader.loadRates()
                Log.debug(this@ShardGrindingCalculator, "Loaded ${rates.size} rates. Keys: ${rates.keys}")
                currentRates = rates.toMutableMap()
                
                val chestPrices = ShardDataLoader.loadChestPrices()
                Log.debug(this@ShardGrindingCalculator, "Loaded ${chestPrices.size} chest prices")
                currentChestPrices = chestPrices.toMutableMap()

                val baitCounts = ShardDataLoader.loadBaitCounts()
                Log.debug(this@ShardGrindingCalculator, "Loaded ${baitCounts.size} bait counts")
                currentBaitCounts = baitCounts.toMutableMap()

                Log.debug(this@ShardGrindingCalculator, "Fetching prices...")
                try {
                    withTimeout(15000) { // 15s timeout
                        priceFetcher.fetchAllPrices()
                    }
                } catch (e: Exception) {
                    Log.debug(this@ShardGrindingCalculator, "Price fetch failed or timed out", e)
                }

                val properties = ShardDataLoader.loadProperties()
                Log.debug(this@ShardGrindingCalculator, "Loaded ${properties.size} properties")
                
                val priceIds = ShardCalculations.getPriceIds(rates, properties)
                Log.debug(this@ShardGrindingCalculator, "Resolved ${priceIds.size} price IDs")
                
                val isInstant = controls.isInstantSell
                Log.debug(this@ShardGrindingCalculator, "Fetching individual prices (isInstant=$isInstant)...")
                val prices = priceIds.associate { (shardId, itemId) ->
                    try {
                        val price = priceFetcher.getSellPrice(itemId, isInstant).price
                        shardId to price
                    } catch (e: Exception) {
                        Log.debug(this@ShardGrindingCalculator, "Failed to get price for $shardId ($itemId)", e)
                        shardId to 0.0
                    }
                }
                Log.debug(this@ShardGrindingCalculator, "Fetched ${prices.size} prices")

                val baitPrice = try {
                    priceFetcher.getBuyPrice("WOODEN_BAIT", isInstant).price
                } catch (e: Exception) {
                    Log.debug(this@ShardGrindingCalculator, "Failed to get bait price", e)
                    0.0
                }

                Log.debug(this@ShardGrindingCalculator, "Combining data...")
                allShards = ShardCalculations.combineData(
                    rates, properties, prices, controls.sellMode, controls.hunterFortune, 
                    currentChestPrices, currentBaitCounts, baitPrice
                )
                
                Log.debug(this@ShardGrindingCalculator, "Updating UI with ${allShards.size} shards")
                Platform.runLater {
                    try {
                        updateUI()
                        loading.set(false)
                        Log.debug(this@ShardGrindingCalculator, "UI Update complete")
                    } catch (e: Exception) {
                        Log.debug(this@ShardGrindingCalculator, "Shard UI update failed", e)
                        loading.set(false)
                    }
                }
            } catch (e: Exception) {
                Log.debug(this@ShardGrindingCalculator, "Shard refresh failed", e)
                Platform.runLater { loading.set(false) }
            }
        }
    }

    private fun updateUI() {
        val search = controls.searchText
        val fortune = controls.hunterFortune
        
        val searchQueries = search.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        
        val filtered = allShards
            .map { it.copy(hunterFortune = fortune) }
            .filter { shard ->
                if (searchQueries.isEmpty()) return@filter true
                
                val cleanName = shard.displayName.lowercase().replace("_", " ")
                val rawName = shard.displayName.lowercase()
                val shardId = shard.shardId.lowercase()
                
                searchQueries.any { query ->
                    cleanName.contains(query) || rawName.contains(query) || shardId.contains(query)
                }
            }
            .let { list ->
                when (controls.sortMode) {
                    "Profit/Hour" -> list.sortedByDescending { it.profitPerHour }
                    "Alphabetical" -> list.sortedBy { it.displayName }
                    else -> list.sortedByDescending { it.profitPerHour }
                }
            }
        
        listView.items = filtered
    }

    private fun saveRate(shardId: String, newRate: Double) {
        currentRates[shardId] = newRate
        // Save in background
        scope.launch {
            ShardDataLoader.saveRates(currentRates)
        }
        
        // Update local state immediately
        allShards = allShards.map {
            if (it.shardId == shardId) it.copy(ratePerHour = newRate) else it
        }
        updateUI()
        sidebar.hide()
    }

    private fun saveChestPrice(displayName: String, newChestPrice: Double) {
        currentChestPrices[displayName] = newChestPrice
        // Save in background
        scope.launch {
            ShardDataLoader.saveChestPrices(currentChestPrices)
        }
        
        // Update local state immediately
        allShards = allShards.map {
            if (it.displayName == displayName) it.copy(chestPrice = newChestPrice) else it
        }
        updateUI()
        sidebar.hide()
    }

    private fun saveBaitCount(displayName: String, newBaitCount: Double) {
        currentBaitCounts[displayName] = newBaitCount
        // Save in background
        scope.launch {
            ShardDataLoader.saveBaitCounts(currentBaitCounts)
        }
        
        // Update local state immediately
        allShards = allShards.map {
            if (it.displayName == displayName) it.copy(baitCount = newBaitCount) else it
        }
        updateUI()
        sidebar.hide()
    }

    private fun createShardCell(shard: ShardInfo): Node = try {
        hbox {
            padding = Insets(8.0, 10.0, 8.0, 10.0)

            val rarityColor = getRarityColor(shard.rarity)
            val sellSuffix = if (shard.sellMode == "Instant Sell") "(SI)" else "(SO)"

            val iconStream = javaClass.getResourceAsStream("/icons/shardIcons/${shard.shardId}.png")
            val imageView = if (iconStream != null) {
                ImageView(Image(iconStream)).apply {
                    fitWidth = ShardUIConfig.iconSize
                    fitHeight = ShardUIConfig.iconSize
                }
            } else {
                // Fallback for missing icon
                Region().apply {
                    minWidth = ShardUIConfig.iconSize
                    minHeight = ShardUIConfig.iconSize
                    style = "-fx-background-color: #333;"
                }
            }

            children.addAll(
                imageView,
                vbox(4.0) {
                    children.addAll(
                        shard.displayName.label(color = rarityColor, bold = true, size = "14px"),
                        "${shard.rarity} Shard".label(color = "#888888", size = "11px")
                    )
                },
                spacer(),
                vbox(4.0, alignment = Pos.CENTER_RIGHT) {
                    children.addAll(
                        String.format("%,.0f/hr", shard.profitPerHour).label(color = "#ffaa00", bold = true, size = "14px"),
                        hbox(4.0, alignment = Pos.CENTER_RIGHT) {
                            children.addAll(
                                String.format("%,.2f shards/hr", shard.effectiveRatePerHour).label(color = "#aaaaaa", size = "11px"),
                                "${String.format("%,.0f", shard.price)} coins $sellSuffix".label(color = "#888888", size = "11px")
                            )
                        }
                    )
                }
            )
        }
    } catch (e: Exception) {
        Log.debug(this, "Failed to create shard cell for ${shard.shardId}", e)
        "Error loading ${shard.displayName}".label()
    }

    private fun getRarityColor(rarity: String) = when (rarity.uppercase()) {
        "COMMON" -> "#ffffff"
        "UNCOMMON" -> "#55ff55"
        "RARE" -> "#5555ff"
        "EPIC" -> "#aa00aa"
        "LEGENDARY" -> "#ffaa00"
        "MYTHIC" -> "#ff55ff"
        else -> "#cccccc"
    }
}
