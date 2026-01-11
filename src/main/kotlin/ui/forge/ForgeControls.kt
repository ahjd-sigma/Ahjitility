package ui.forge

import business.forge.*
import javafx.geometry.*
import javafx.scene.control.*
import javafx.scene.layout.*
import utils.*

class ForgeControls {
    private val searchField = textField("Search:", width = ForgeUIConfig.searchFieldWidth).apply { 
        textProperty().addListener { _, _, _ -> notifyChange() }
    }

    private val sellModeBox = ComboBox<String>().apply {
        items.addAll("Instant Sell", "Sell Order")
        value = "Sell Order"
        style = Styles.combo
        setOnAction { notifyChange() }
    }
    
    private val buyModeBox = ComboBox<String>().apply {
        items.addAll("Instant Buy", "Buy Order")
        value = "Buy Order"
        style = Styles.combo
        setOnAction { notifyChange() }
    }
    
    private val sortModeBox = ComboBox<String>().apply {
        items.addAll("Sort: Profit", "Sort: Profit/Hour")
        value = "Sort: Profit"
        style = Styles.combo
        setOnAction { notifyChange() }
    }
    
    private val sourcePriorityBox = ComboBox<String>().apply {
        items.addAll("All Sources", "Bazaar First", "AH First")
        value = "All Sources"
        style = Styles.combo
        setOnAction { notifyChange() }
    }
    
    private val slotsBox = ComboBox<Int>().apply { 
        items.addAll((1..7).toList())
        value = 1
        style = Styles.combo
        setOnAction { notifyChange() }
    }
    
    private val quickForgeBox = ComboBox<Int>().apply { 
        items.addAll((0..20).toList())
        value = ForgeModuleConfig.defaultQuickForgeLevel
        style = Styles.combo
        setOnAction { notifyChange() }
    }
    
    private val bazaarTaxField = textField(ForgeModuleConfig.defaultBazaarTax.toString(), width = ForgeUIConfig.taxFieldWidth).apply { 
        textProperty().addListener { _, _, _ -> notifyChange() }
    }
    
    private val ahTaxField = textField(ForgeModuleConfig.defaultAhMultiplier.toString(), width = ForgeUIConfig.taxFieldWidth).apply { 
        textProperty().addListener { _, _, _ -> notifyChange() }
    }
    
    private val durationRangeBox = ComboBox<String>().apply {
        items.addAll(
            "All Durations", "0s - 60s", "1min - 30min", "30min - 1hour", 
            "1hour - 6hours", "6h - 18h", "18h+"
        )
        value = "All Durations"
        style = Styles.combo
        setOnAction { notifyChange() }
    }
    
    private val refreshButton = Button("Refresh").apply { 
        style = Styles.button
        // OnAction will be handled by the calculator calling this
    }

    val node = HBox(10.0).apply {
        padding = Insets(10.0)
        alignment = Pos.CENTER_LEFT
        style = "-fx-background-color: ${Styles.DARK_BG}; -fx-border-color: #333333; -fx-border-width: 0 0 1 0;"
        
        children.addAll(
            "Search:".label(), searchField,
            separator(),
            "Sell:".label(), sellModeBox,
            "Buy:".label(), buyModeBox,
            separator(),
            "BZ %:".label(), bazaarTaxField,
            "AH x:".label(), ahTaxField,
            separator(),
            "Sort:".label(), sortModeBox,
            separator(),
            "Source:".label(), sourcePriorityBox,
            separator(),
            "Time:".label(), durationRangeBox,
            separator(),
            "Slots:".label(), slotsBox,
            "Quick:".label(), quickForgeBox,
            separator(),
            refreshButton
        )
    }

    private val listeners = mutableListOf<() -> Unit>()

    fun onChange(listener: () -> Unit) {
        listeners.add(listener)
    }
    
    fun onRefresh(action: () -> Unit) {
        refreshButton.setOnAction { action() }
    }

    private fun notifyChange() {
        listeners.forEach { it() }
    }

    fun toConfig() = ForgeConfig(
        searchText = searchField.text,
        isInstantSell = sellModeBox.value == "Instant Sell",
        isInstantBuy = buyModeBox.value == "Instant Buy",
        slots = slotsBox.value,
        quickForgeLevel = quickForgeBox.value,
        bazaarTax = bazaarTaxField.text.toDoubleOrNull() ?: ForgeModuleConfig.defaultBazaarTax,
        ahMultiplier = ahTaxField.text.toDoubleOrNull() ?: ForgeModuleConfig.defaultAhMultiplier,
        sortMode = when {
            sortModeBox.value.contains("Hour") -> SortMode.PROFIT_PER_HOUR
            else -> SortMode.PROFIT
        },
        sourcePriority = when {
            sourcePriorityBox.value.contains("Bazaar") -> SourcePriority.BAZAAR_FIRST
            sourcePriorityBox.value.contains("AH") -> SourcePriority.AH_FIRST
            else -> SourcePriority.ALL
        },
        durationFilter = when (durationRangeBox.selectionModel.selectedIndex) {
            1 -> DurationFilter.ZERO_TO_60
            2 -> DurationFilter.ONE_TO_30MIN
            3 -> DurationFilter.HALF_TO_ONE_HOUR
            4 -> DurationFilter.ONE_TO_6_HOURS
            5 -> DurationFilter.SIX_TO_18_HOURS
            6 -> DurationFilter.OVER_18_HOURS
            else -> DurationFilter.ALL
        }
    )
}
