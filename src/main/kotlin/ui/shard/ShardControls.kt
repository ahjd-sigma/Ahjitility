package ui.shard

import javafx.geometry.Insets
import utils.*

class ShardControls {
    private val searchField = textField(prompt = "Search by name...", width = ShardUIConfig.searchFieldWidth)
    private val sortBox = comboBox(listOf("Profit/Hour", "Alphabetical"), "Profit/Hour")
    private val sellModeBox = comboBox(listOf("Instant Sell", "Sell Order"), "Sell Order")
    private val hunterFortuneField = textField(text = "0", prompt = "0-200", width = 60.0)
    private val refreshButton = "Refresh".button()

    val node = hbox(20.0) {
        padding = Insets(12.0)
        style = "-fx-background-color: ${Styles.DARK_BG}; -fx-border-color: #333333; -fx-border-width: 0 0 1 0;"
        children.addAll(
            vbox(2.0) {
                children.addAll("Search".label(color = "#888888", size = 10, bold = true), searchField)
            },
            vbox(2.0) {
                children.addAll("Sort".label(color = "#888888", size = 10, bold = true), sortBox)
            },
            vbox(2.0) {
                children.addAll("Sell Mode".label(color = "#888888", size = 10, bold = true), sellModeBox)
            },
            vbox(2.0) {
                children.addAll("Hunter Fortune".label(color = "#888888", size = 10, bold = true), hunterFortuneField)
            },
            vbox(2.0) {
                alignment = javafx.geometry.Pos.BOTTOM_LEFT
                children.add(refreshButton)
            }
        )
    }

    val searchText: String get() = searchField.text.lowercase()
    val sortMode: String get() = sortBox.value ?: "Profit/Hour"
    val isInstantSell: Boolean get() = sellModeBox.value == "Instant Sell"
    val sellMode: String get() = sellModeBox.value ?: "Sell Order"
    val hunterFortune: Int get() = hunterFortuneField.text.toIntOrNull()?.coerceIn(0, 200) ?: 0

    fun onChange(handler: () -> Unit) {
        searchField.textProperty().addListener { _, _, _ -> handler() }
        sortBox.setOnAction { handler() }
        hunterFortuneField.textProperty().addListener { _, _, newValue ->
            val numeric = newValue.filter { it.isDigit() }
            if (numeric != newValue) hunterFortuneField.text = numeric
            handler()
        }
    }

    fun onSellModeChange(handler: () -> Unit) {
        sellModeBox.setOnAction { handler() }
    }

    fun onRefresh(handler: () -> Unit) {
        refreshButton.setOnAction { handler() }
    }
}
