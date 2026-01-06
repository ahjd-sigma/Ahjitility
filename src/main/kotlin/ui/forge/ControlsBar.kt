package ui.forge

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox

class ControlsBar {
    private val DARK_BG = "#2b2b2b"
    private val TEXT_COLOR = "#cccccc"

    val searchField = TextField().apply {
        promptText = "Search by name..."
        style = "-fx-background-color: #333333; -fx-text-fill: white; -fx-prompt-text-fill: #888888; -fx-font-size: 13px;"
    }

    val sellModeBox = ComboBox<String>().apply {
        items.addAll("Instant Sell", "Sell Order")
        value = "Instant Sell"
        style = "-fx-font-size: 13px;"
    }

    val buyModeBox = ComboBox<String>().apply {
        items.addAll("Instant Buy", "Buy Order")
        value = "Instant Buy"
        style = "-fx-font-size: 13px;"
    }

    val sortModeBox = ComboBox<String>().apply {
        items.addAll("Sort: Profit", "Sort: Profit/Hour")
        value = "Sort: Profit"
        style = "-fx-font-size: 13px;"
    }

    val sourcePriorityBox = ComboBox<String>().apply {
        items.addAll("All Sources", "Bazaar First", "AH First")
        value = "All Sources"
        style = "-fx-font-size: 13px;"
    }

    val slotsBox = ComboBox<Int>().apply {
        items.addAll((1..7).toList())
        value = 1
        style = "-fx-font-size: 13px;"
    }

    val quickForgeBox = ComboBox<Int>().apply {
        items.addAll((0..20).toList())
        value = 0
        style = "-fx-font-size: 13px;"
    }

    val bazaarTaxField = TextField("1.25").apply {
        prefWidth = 50.0
        style = "-fx-background-color: #333333; -fx-text-fill: white; -fx-font-size: 13px;"
    }

    val ahTaxField = TextField("1").apply {
        prefWidth = 50.0
        style = "-fx-background-color: #333333; -fx-text-fill: white; -fx-font-size: 13px;"
    }

    val durationRangeBox = ComboBox<String>().apply {
        items.addAll("All Durations", "0s - 60s", "1min - 30min", "30min - 1hour", "1hour - 6hours", "6h - 18h", "18h+")
        value = "All Durations"
        style = "-fx-font-size: 13px;"
    }

    val refreshButton = Button("Refresh Data").apply {
        style = "-fx-background-color: #3c3f41; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-font-size: 13px;"
    }

    val node: HBox = HBox(10.0).apply {
        padding = Insets(10.0)
        alignment = Pos.CENTER_LEFT
        style = "-fx-background-color: $DARK_BG; -fx-border-color: #333333; -fx-border-width: 0 0 1 0;"

        val searchLabel = Label("Search:").apply { style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;" }
        val sellLabel = Label("Sell:").apply { style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;" }
        val buyLabel = Label("Buy:").apply { style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;" }
        val sortLabel = Label("Sort:").apply { style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;" }
        val priorityLabel = Label("Source:").apply { style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;" }
        val durationLabel = Label("Time:").apply { style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;" }
        val slotsLabel = Label("Slots:").apply { style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;" }
        val quickForgeLabel = Label("Quick:").apply { style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;" }
        val bzTaxLabel = Label("BZ Tax %:").apply { style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;" }
        val ahTaxLabel = Label("AH Tax Multi:").apply { style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;" }

        children.addAll(
            searchLabel, searchField,
            Separator(javafx.geometry.Orientation.VERTICAL),
            sellLabel, sellModeBox,
            buyLabel, buyModeBox,
            Separator(javafx.geometry.Orientation.VERTICAL),
            bzTaxLabel, bazaarTaxField,
            ahTaxLabel, ahTaxField,
            Separator(javafx.geometry.Orientation.VERTICAL),
            sortLabel, sortModeBox,
            Separator(javafx.geometry.Orientation.VERTICAL),
            priorityLabel, sourcePriorityBox,
            Separator(javafx.geometry.Orientation.VERTICAL),
            durationLabel, durationRangeBox,
            Separator(javafx.geometry.Orientation.VERTICAL),
            slotsLabel, slotsBox,
            quickForgeLabel, quickForgeBox,
            Separator(javafx.geometry.Orientation.VERTICAL),
            refreshButton
        )
    }

    fun setupListeners(onChanged: () -> Unit) {
        val validateDecimal = { field: TextField, oldText: String, newText: String ->
            if (newText.isNotEmpty() && (newText.toDoubleOrNull() == null || newText.contains("-"))) {
                field.text = oldText
            } else {
                onChanged()
            }
        }

        bazaarTaxField.textProperty().addListener { _, old, new -> validateDecimal(bazaarTaxField, old, new) }
        ahTaxField.textProperty().addListener { _, old, new -> validateDecimal(ahTaxField, old, new) }

        sellModeBox.setOnAction { onChanged() }
        buyModeBox.setOnAction { onChanged() }
        sortModeBox.setOnAction { onChanged() }
        sourcePriorityBox.setOnAction { onChanged() }
        searchField.textProperty().addListener { _, _, _ -> onChanged() }
        slotsBox.setOnAction { onChanged() }
        quickForgeBox.setOnAction { onChanged() }
        durationRangeBox.setOnAction { onChanged() }
    }
}