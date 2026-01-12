package ui.shard

import business.shard.*
import calculator.*
import javafx.scene.layout.*
import javafx.scene.control.*
import javafx.geometry.*
import javafx.application.Platform
import utils.PriceFetcher
import utils.Styles
import utils.GeneralConfig
import utils.ShardUIConfig
import utils.text
import utils.applyDarkStyle
import utils.enableAdvancedScrolling
import javafx.scene.Node
import javafx.scene.image.ImageView
import javafx.scene.image.Image
import kotlinx.coroutines.*

class ShardGrindingCalculator(priceFetcher: PriceFetcher) : BaseCalculator(priceFetcher) {
    private val controls = ShardControls()
    private val listView = GenericListView<ShardInfo>({ GeneralConfig.shardScrollMultiplier }) { createShardCell(it) }
    private val sidebar = ShardSidebar()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var allShards = emptyList<ShardInfo>()
    private var currentRates = mutableMapOf<String, Double>()
    private var currentChestPrices = mutableMapOf<String, Double>()
    private var currentBaitCounts = mutableMapOf<String, Double>()

    override fun createContent() = SplitPane(
        listView.listView,
        ScrollPane(sidebar.node).apply {
            applyDarkStyle()
            enableAdvancedScrolling({ GeneralConfig.shardScrollMultiplier })
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
                println("[DEBUG] Shard: Starting refreshData")
                val rates = ShardDataLoader.loadRates()
                println("[DEBUG] Shard: Loaded ${rates.size} rates. Keys: ${rates.keys}")
                currentRates = rates.toMutableMap()
                
                val chestPrices = ShardDataLoader.loadChestPrices()
                println("[DEBUG] Shard: Loaded ${chestPrices.size} chest prices")
                currentChestPrices = chestPrices.toMutableMap()

                val baitCounts = ShardDataLoader.loadBaitCounts()
                println("[DEBUG] Shard: Loaded ${baitCounts.size} bait counts")
                currentBaitCounts = baitCounts.toMutableMap()

                println("[DEBUG] Shard: Fetching prices...")
                try {
                    withTimeout(15000) { // 15s timeout
                        priceFetcher.fetchAllPrices()
                    }
                } catch (e: Exception) {
                    println("[ERROR] Shard: Price fetch failed or timed out: ${e.message}")
                }

                val properties = ShardDataLoader.loadProperties()
                println("[DEBUG] Shard: Loaded ${properties.size} properties")
                
                val priceIds = ShardCalculations.getPriceIds(rates, properties)
                println("[DEBUG] Shard: Resolved ${priceIds.size} price IDs")
                
                val isInstant = controls.isInstantSell
                println("[DEBUG] Shard: Fetching individual prices (isInstant=$isInstant)...")
                val prices = priceIds.associate { (shardId, itemId) ->
                    try {
                        val price = priceFetcher.getSellPrice(itemId, isInstant).price
                        if (price == 0.0) {
                            // println("[INFO] Shard: Price is 0.0 for $shardId ($itemId)")
                        }
                        shardId to price
                    } catch (e: Exception) {
                        println("[ERROR] Shard: Failed to get price for $shardId ($itemId): ${e.message}")
                        shardId to 0.0
                    }
                }
                println("[DEBUG] Shard: Fetched ${prices.size} prices")

                val baitPrice = try {
                    priceFetcher.getBuyPrice("WOODEN_BAIT", isInstant).price
                } catch (e: Exception) {
                    println("[ERROR] Shard: Failed to get bait price: ${e.message}")
                    0.0
                }

                println("[DEBUG] Shard: Combining data...")
                allShards = ShardCalculations.combineData(
                    rates, properties, prices, controls.sellMode, controls.hunterFortune, 
                    currentChestPrices, currentBaitCounts, baitPrice
                )
                
                println("[DEBUG] Shard: Updating UI with ${allShards.size} shards")
                Platform.runLater {
                    try {
                        updateUI()
                        loading.set(false)
                        println("[DEBUG] Shard: UI Update complete")
                    } catch (e: Exception) {
                        println("[ERROR] Shard UI update failed: ${e.message}")
                        e.printStackTrace()
                        loading.set(false)
                    }
                }
            } catch (e: Exception) {
                println("[ERROR] Shard refresh failed: ${e.message}")
                e.printStackTrace()
                Platform.runLater { loading.set(false) }
            }
        }
    }

    private fun updateUI() {
        val search = controls.searchText
        val fortune = controls.hunterFortune
        
        val filtered = allShards
            .map { it.copy(hunterFortune = fortune) }
            .filter {
                it.displayName.contains(search, ignoreCase = true) ||
                        it.shardId.contains(search, ignoreCase = true)
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
        HBox(10.0).apply {
            padding = Insets(8.0, 10.0, 8.0, 10.0)
            alignment = Pos.CENTER_LEFT

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
                VBox(4.0).apply {
                    children.addAll(
                        Label(shard.displayName).apply { style = "-fx-text-fill: $rarityColor; -fx-font-weight: bold; -fx-font-size: 14px;" },
                        Label("${shard.rarity} Shard").apply { style = "-fx-text-fill: #888888; -fx-font-size: 11px;" }
                    )
                },
                Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
                VBox(4.0).apply {
                    alignment = Pos.CENTER_RIGHT
                    children.addAll(
                        Label(String.format("%,.0f/hr", shard.profitPerHour)).apply { style = "-fx-text-fill: #ffaa00; -fx-font-weight: bold; -fx-font-size: 14px;" },
                        HBox(4.0).apply {
                            alignment = Pos.CENTER_RIGHT
                            children.addAll(
                                Label(String.format("%,.2f shards/hr", shard.effectiveRatePerHour)).apply { style = "-fx-text-fill: #aaaaaa; -fx-font-size: 11px;" },
                                Label("${String.format("%,.0f", shard.price)} coins $sellSuffix").apply { style = "-fx-text-fill: #888888; -fx-font-size: 11px;" }
                            )
                        }
                    )
                }
            )
        }
    } catch (e: Exception) {
        println("[ERROR] Failed to create shard cell for ${shard.shardId}: ${e.message}")
        Label("Error loading ${shard.displayName}")
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
