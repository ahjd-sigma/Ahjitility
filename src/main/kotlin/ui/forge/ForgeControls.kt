package ui.forge

import business.forge.DurationFilter
import business.forge.ForgeConfig
import business.forge.SortMode
import business.forge.SourcePriority
import javafx.geometry.Insets
import javafx.geometry.Pos
import utils.*

class ForgeControls {
    private val searchField = textField(prompt = "Search:", width = ForgeUIConfig.searchFieldWidth) { notifyChange() }

    private val sellModeBox = comboBox(listOf("Instant Sell", "Sell Order"), "Sell Order") { notifyChange() }
    private val buyModeBox = comboBox(listOf("Instant Buy", "Buy Order"), "Buy Order") { notifyChange() }
    private val sortModeBox = comboBox(listOf("Sort: Profit", "Sort: Profit/Hour"), "Sort: Profit") { notifyChange() }
    private val sourcePriorityBox = comboBox(listOf("All Sources", "Bazaar First", "AH First"), "All Sources") { notifyChange() }
    private val slotsBox = comboBox((1..7).toList(), 1) { notifyChange() }
    private val quickForgeBox = comboBox((0..20).toList(), ForgeModuleConfig.defaultQuickForgeLevel) { notifyChange() }
    
    private val bazaarTaxField = textField(ForgeModuleConfig.defaultBazaarTax.toString(), width = ForgeUIConfig.taxFieldWidth) { notifyChange() }
    private val ahTaxField = textField(ForgeModuleConfig.defaultAhMultiplier.toString(), width = ForgeUIConfig.taxFieldWidth) { notifyChange() }
    
    private val durationRangeBox = comboBox(
        listOf("All Durations", "0s - 60s", "1min - 30min", "30min - 1hour", "1hour - 6hours", "6h - 18h", "18h+"),
        "All Durations"
    ) { notifyChange() }
    
    private val refreshButton = "Refresh".button()

    val node = hbox(20.0) {
        padding = Insets(12.0)
        style = "-fx-background-color: ${Styles.DARK_BG}; -fx-border-color: #333333; -fx-border-width: 0 0 1 0;"
        
        children.addAll(
            vbox(2.0) {
                children.addAll("Search".label(color = "#888888", size = 10, bold = true), searchField)
            },
            vbox(2.0) {
                children.addAll("Market".label(color = "#888888", size = 10, bold = true), hbox(5.0) { children.addAll(sellModeBox, buyModeBox) })
            },
            vbox(2.0) {
                children.addAll("Tax % / AH x".label(color = "#888888", size = 10, bold = true), hbox(5.0) { children.addAll(bazaarTaxField, ahTaxField) })
            },
            vbox(2.0) {
                children.addAll("Sort".label(color = "#888888", size = 10, bold = true), sortModeBox)
            },
            vbox(2.0) {
                children.addAll("Priority".label(color = "#888888", size = 10, bold = true), sourcePriorityBox)
            },
            vbox(2.0) {
                children.addAll("Time".label(color = "#888888", size = 10, bold = true), durationRangeBox)
            },
            vbox(2.0) {
                children.addAll("Slots / Quick Forge lvl".label(color = "#888888", size = 10, bold = true), hbox(5.0) { children.addAll(slotsBox, quickForgeBox) })
            },
            vbox(2.0) {
                alignment = Pos.BOTTOM_LEFT
                children.add(refreshButton)
            }
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
