package ui.shard

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import utils.*

class ShardControls {
    private val searchField = textField("Search:", width = ShardUIConfig.searchFieldWidth).apply {
        promptText = "Search by name..."
    }
    private val sortBox = ComboBox<String>().apply {
        items.addAll("Profit/Hour", "Alphabetical")
        value = "Profit/Hour"
        style = Styles.combo
    }
    private val sellModeBox = ComboBox<String>().apply {
        items.addAll("Instant Sell", "Sell Order")
        value = "Sell Order"
        style = Styles.combo
    }
    private val hunterFortuneField = textField("0", width = 60.0).apply {
        promptText = "0-200"
    }
    private val refreshButton = Button("Refresh").apply { style = Styles.button }

    val node = HBox(10.0).apply {
        padding = Insets(10.0)
        alignment = Pos.CENTER_LEFT
        style = "-fx-background-color: ${Styles.DARK_BG}; -fx-border-color: #333333; -fx-border-width: 0 0 1 0;"
        children.addAll(
            "Search:".label(), searchField,
            separator(),
            "Sort:".label(), sortBox,
            separator(),
            "Sell:".label(), sellModeBox,
            separator(),
            "Hunter Fortune:".label(), hunterFortuneField,
            separator(),
            refreshButton
        )
    }

    val searchText get() = searchField.text.lowercase()
    val sortMode get() = sortBox.value
    val isInstantSell get() = sellModeBox.value == "Instant Sell"
    val sellMode get() = sellModeBox.value
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
