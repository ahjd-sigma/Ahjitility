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
            sidebar.show(shard) { newRate -> 
                shard?.let { saveRate(it.shardId, newRate) }
            }
        }
        refreshData()
    }

    private fun refreshData() {
        Platform.runLater { loading.set(true) }
        scope.launch {
            try {
                val rates = ShardDataLoader.loadRates()
                currentRates = rates.toMutableMap()
                priceFetcher.fetchAllPrices()

                val properties = ShardDataLoader.loadProperties()
                val priceIds = ShardCalculations.getPriceIds(rates, properties)
                val isInstant = controls.isInstantSell
                val prices = priceIds.associate { (shardId, itemId) ->
                    shardId to priceFetcher.getSellPrice(itemId, isInstant).price
                }

                allShards = ShardCalculations.combineData(rates, properties, prices, controls.sellMode, controls.hunterFortune)
                
                Platform.runLater {
                    updateUI()
                    loading.set(false)
                }
            } catch (e: Exception) {
                println("ERROR: Shard refresh failed: ${e.message}")
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

    private fun createShardCell(shard: ShardInfo) = HBox(10.0).apply {
        padding = Insets(8.0, 10.0, 8.0, 10.0)
        alignment = Pos.CENTER_LEFT

        val rarityColor = getRarityColor(shard.rarity)
        val sellSuffix = if (shard.sellMode == "Instant Sell") "(SI)" else "(SO)"

        children.addAll(
            ImageView(Image(javaClass.getResourceAsStream("/icons/shardIcons/${shard.shardId}.png"))).apply {
                fitWidth = ShardUIConfig.iconSize
                fitHeight = ShardUIConfig.iconSize
            },
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
