package ui

import com.google.gson.Gson
import forge.Item
import forge.PriceFetcher
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import ui.forge.*
import java.util.concurrent.CompletableFuture

class MainApp : Application() {
    private val DARK_BG = "#2b2b2b"

    private val priceFetcher = PriceFetcher()
    private val calculator = PriceCalculator(priceFetcher)
    private val controlsBar = ControlsBar()
    private val forgeList = ForgeListView()
    private val sidebar = RecipeSidebar(priceFetcher)

    private var currentItems: MutableList<Item> = mutableListOf()

    private val loadingOverlay = Label("Loading...").apply {
        style = "-fx-background-color: rgba(0, 0, 0, 0.5); -fx-text-fill: orange; -fx-padding: 15px; -fx-background-radius: 5px; -fx-font-weight: bold; -fx-font-size: 16px;"
        isVisible = false
        isMouseTransparent = true
    }

    override fun start(primaryStage: Stage) {
        controlsBar.setupListeners { updateUI() }

        forgeList.onItemSelected { item ->
            sidebar.showRecipe(
                item,
                controlsBar.buyModeBox.value == "Instant Buy",
                controlsBar.slotsBox.value ?: 1,
                controlsBar.quickForgeBox.value ?: 0
            )
        }

        controlsBar.refreshButton.setOnAction {
            loadingOverlay.isVisible = true
            CompletableFuture.runAsync {
                reloadAllData(forceRefreshPrices = true)
            }.thenRun {
                Platform.runLater {
                    forgeList.setItems(currentItems)
                    updateUI()
                    loadingOverlay.isVisible = false
                }
            }
        }

        loadingOverlay.isVisible = true
        CompletableFuture.runAsync {
            reloadAllData()
        }.thenRun {
            Platform.runLater {
                forgeList.setItems(currentItems)
                updateUI()
                loadingOverlay.isVisible = false
            }
        }

        val splitPane = SplitPane()
        splitPane.items.addAll(forgeList.listView, sidebar.node)
        splitPane.setDividerPositions(0.4)
        splitPane.style = "-fx-background-color: $DARK_BG;"

        val content = VBox()
        content.children.addAll(controlsBar.node, splitPane)
        VBox.setVgrow(splitPane, Priority.ALWAYS)

        val root = StackPane()
        root.children.addAll(content, loadingOverlay)
        StackPane.setAlignment(loadingOverlay, Pos.CENTER)

        val scene = Scene(root, 900.0, 700.0)
        primaryStage.title = "Ahjitility - Forge Manager"
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun updateUI() {
        calculator.assignPrices(
            currentItems,
            controlsBar.sellModeBox.value == "Instant Sell",
            controlsBar.buyModeBox.value == "Instant Buy",
            controlsBar.slotsBox.value ?: 1,
            controlsBar.quickForgeBox.value ?: 0,
            controlsBar.bazaarTaxField.text.toDoubleOrNull() ?: 1.25,
            controlsBar.ahTaxField.text.toDoubleOrNull() ?: 1.0
        )

        val quickForgeLevel = controlsBar.quickForgeBox.value ?: 0
        val reduction = calculator.getQuickForgeReduction(quickForgeLevel)

        val searchText = controlsBar.searchField.text.lowercase()
        val filteredItems = currentItems.filter { item ->
            val matchesSearch = if (searchText.isEmpty()) true
            else item.displayName?.lowercase()?.contains(searchText) == true

            val baseDuration = item.durationSeconds ?: 0
            val reducedDuration = (baseDuration.toDouble() * (1.0 - reduction)).toInt()

            val matchesDuration = when (controlsBar.durationRangeBox.value) {
                "0s - 60s" -> reducedDuration in 0..60
                "1min - 30min" -> reducedDuration in 61..1800
                "30min - 1hour" -> reducedDuration in 1801..3600
                "1hour - 6hours" -> reducedDuration in 3601..21600
                "6h - 18h" -> reducedDuration in 21601..64800
                "18h+" -> reducedDuration > 64800
                else -> true
            }

            matchesSearch && matchesDuration
        }.toMutableList()

        filteredItems.sortWith(compareByDescending<Item> {
            when (controlsBar.sourcePriorityBox.value) {
                "Bazaar First" -> if (it.isBazaar) 1 else 0
                "AH First" -> if (!it.isBazaar) 1 else 0
                else -> 0
            }
        }.thenByDescending {
            if (controlsBar.sortModeBox.value == "Sort: Profit/Hour") it.profitPerHour
            else (it.value - it.totalRecipeCost)
        })

        forgeList.setItems(filteredItems)
        sidebar.showRecipe(
            forgeList.getSelectedItem(),
            controlsBar.buyModeBox.value == "Instant Buy",
            controlsBar.slotsBox.value ?: 1,
            controlsBar.quickForgeBox.value ?: 0
        )
    }

    private fun reloadAllData(forceRefreshPrices: Boolean = false) {
        val items = loadItems()
        currentItems.clear()
        currentItems.addAll(items)

        priceFetcher.fetchAllPrices(force = forceRefreshPrices)

        calculator.assignPrices(
            currentItems,
            controlsBar.sellModeBox.value == "Instant Sell",
            controlsBar.buyModeBox.value == "Instant Buy",
            controlsBar.slotsBox.value ?: 1,
            controlsBar.quickForgeBox.value ?: 0,
            controlsBar.bazaarTaxField.text.toDoubleOrNull() ?: 1.25,
            controlsBar.ahTaxField.text.toDoubleOrNull() ?: 1.0
        )
    }

    private fun loadItems(): List<Item> {
        val gson = Gson()
        val jsonText = MainApp::class.java.getResourceAsStream("/forge/ForgeRecipes.json")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalStateException("ForgeRecipes.json not found in resources")

        return gson.fromJson(jsonText, Array<Item>::class.java).toList()
    }
}