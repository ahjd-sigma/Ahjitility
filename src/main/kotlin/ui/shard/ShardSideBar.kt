package ui.shard

import business.shard.ShardInfo
import javafx.geometry.Insets
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import utils.*

class ShardSidebar {
    private val nameLabel = "".label(size = "14px")
    private val rateField = textField()
    private val chestPriceField = textField()
    private val baitCountField = textField()
    private val baitPriceLabel = "".label(color = "#888888", size = "11px")
    private var onSave: ((Double) -> Unit)? = null
    private var onSaveChestPrice: ((Double) -> Unit)? = null
    private var onSaveBaitCount: ((Double) -> Unit)? = null

    private val contentBox = vbox(15.0) {
        padding = Insets(20.0)
    }

    val node = vbox {
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
                "Select a shard".label(color = "#888888", size = "16px", bold = true)
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
            "Modify Rate".label(color = "white", size = "18px", bold = true),
            nameLabel,
            vbox(8.0) {
                padding = Insets(10.0, 0.0, 0.0, 0.0)
                children.addAll(
                    "Rate per hour:".label(),
                    rateField
                )
            }
        )

        if (shard.isDungeonShard()) {
            contentBox.children.add(
                vbox(8.0) {
                    children.addAll(
                        "Chest Price:".label(),
                        chestPriceField
                    )
                }
            )
        }

        if (shard.isFishingShard) {
            contentBox.children.add(
                vbox(8.0) {
                    children.addAll(
                        "Bait per 5 mins:".label(),
                        baitCountField,
                        baitPriceLabel
                    )
                }
            )
        }

        contentBox.children.add(
            vbox(10.0) {
                padding = Insets(10.0, 0.0, 0.0, 0.0)
                children.addAll(
                    "Save Changes".button(onClick = { save() }).apply {
                        style = """
                            -fx-background-color: #4CAF50; -fx-text-fill: white; 
                            -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 5;
                            -fx-cursor: hand;
                        """.trimIndent()
                        maxWidth = Double.MAX_VALUE
                    },
                    "Close".button(onClick = { hide() }).apply {
                        style = """
                            -fx-background-color: #f44336; -fx-text-fill: white;
                            -fx-padding: 8; -fx-background-radius: 5; -fx-cursor: hand;
                        """.trimIndent()
                        maxWidth = Double.MAX_VALUE
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
            Log.debug(this, "Invalid input in ShardSideBar save")
        }
    }
}
