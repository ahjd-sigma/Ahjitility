package ui.shard

import business.shard.*
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import utils.Styles
import utils.label
import utils.textField
import utils.separator

class ShardSidebar {
    private val nameLabel = Label().apply {
        style = "-fx-text-fill: ${Styles.TEXT_COLOR}; -fx-font-size: 14px;"
    }
    private val rateField = TextField().apply {
        style = Styles.field
    }
    private val chestPriceField = TextField().apply {
        style = Styles.field
    }
    private val baitCountField = TextField().apply {
        style = Styles.field
    }
    private val baitPriceLabel = Label().apply {
        style = "-fx-text-fill: #888888; -fx-font-size: 11px;"
    }
    private var onSave: ((Double) -> Unit)? = null
    private var onSaveChestPrice: ((Double) -> Unit)? = null
    private var onSaveBaitCount: ((Double) -> Unit)? = null

    private val contentBox = VBox(15.0).apply {
        padding = Insets(20.0)
    }

    val node = VBox().apply {
        style = "-fx-background-color: ${Styles.DARK_BG};"
        children.add(contentBox)
        VBox.setVgrow(contentBox, Priority.ALWAYS)
    }
    
    init {
         show(null, {}, {}, {})
    }

    fun show(shard: ShardInfo?, onSaveCallback: (Double) -> Unit, onSaveChestPriceCallback: (Double) -> Unit, onSaveBaitCountCallback: (Double) -> Unit) {
        contentBox.children.clear()
        
        if (shard == null) {
            contentBox.children.add(
                Label("Select a shard").apply {
                    style = "-fx-text-fill: #888888; -fx-font-size: 16px; -fx-font-weight: bold;"
                }
            )
            return
        }

        nameLabel.text = "${shard.displayName} (${shard.shardId})"
        rateField.text = shard.ratePerHour.toString()
        chestPriceField.text = shard.chestPrice.toString()
        baitCountField.text = shard.baitCount.toString()
        if (shard.isFishingShard) {
            baitPriceLabel.text = "Current Bait Price: ${String.format("%,.0f", shard.baitPrice)} coins"
        }
        onSave = onSaveCallback
        onSaveChestPrice = onSaveChestPriceCallback
        onSaveBaitCount = onSaveBaitCountCallback
        
        contentBox.children.addAll(
            Label("Modify Rate").apply {
                style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;"
            },
            nameLabel,
            separator().apply { style = "-fx-background-color: #444; -fx-opacity: 0.5;" },
            VBox(8.0,
                Label("Rate per hour:").apply { style = "-fx-text-fill: ${Styles.TEXT_COLOR};" },
                rateField
            )
        )

        if (shard.isDungeonShard()) {
            contentBox.children.add(
                VBox(8.0,
                    Label("Chest Price:").apply { style = "-fx-text-fill: ${Styles.TEXT_COLOR};" },
                    chestPriceField
                )
            )
        }

        if (shard.isFishingShard) {
            contentBox.children.add(
                VBox(8.0,
                    Label("Bait per 5 mins:").apply { style = "-fx-text-fill: ${Styles.TEXT_COLOR};" },
                    baitCountField,
                    baitPriceLabel
                )
            )
        }

        contentBox.children.add(
            VBox(10.0).apply {
                padding = Insets(10.0, 0.0, 0.0, 0.0)
                children.addAll(
                    Button("Save Changes").apply {
                        style = """
                            -fx-background-color: #4CAF50; -fx-text-fill: white; 
                            -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 5;
                            -fx-cursor: hand;
                        """.trimIndent()
                        maxWidth = Double.MAX_VALUE
                        setOnAction { save() }
                    },
                    Button("Close").apply {
                        style = """
                            -fx-background-color: #f44336; -fx-text-fill: white;
                            -fx-padding: 8; -fx-background-radius: 5; -fx-cursor: hand;
                        """.trimIndent()
                        maxWidth = Double.MAX_VALUE
                        setOnAction { hide() }
                    }
                )
            }
        )
    }

    fun hide() {
        show(null, {}, {}, {})
    }

    private fun save() {
        val rate = rateField.text.toDoubleOrNull()
        val chestPrice = chestPriceField.text.toDoubleOrNull()
        val baitCount = baitCountField.text.toDoubleOrNull()

        if (rate != null) {
            onSave?.invoke(rate)
        }
        if (chestPrice != null) {
            onSaveChestPrice?.invoke(chestPrice)
        }
        if (baitCount != null) {
            onSaveBaitCount?.invoke(baitCount)
        }
        
        if (rate == null && chestPrice == null && baitCount == null) {
            println("Invalid input")
        }
    }
}
