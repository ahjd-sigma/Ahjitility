package ui.forge

import forge.Item
import javafx.geometry.Insets
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Separator
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextFlow

class ForgeListView {
    private val DARKER_BG = "#1e1e1e"
    private val TEXT_COLOR = "#cccccc"

    val listView = ListView<Item>().apply {
        style = "-fx-background-color: $DARKER_BG; -fx-control-inner-background: $DARKER_BG;"

        setCellFactory {
            object : ListCell<Item>() {
                override fun updateItem(item: Item?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        graphic = null
                        style = "-fx-background-color: $DARKER_BG;"
                    } else {
                        val nameText = Text("${item.displayName} - ").apply {
                            fill = Color.web(TEXT_COLOR)
                            font = Font.font(13.0)
                        }
                        val priceText = if (item.value > 0.0) {
                            Text("${String.format("%,.1f", item.value)} ").apply {
                                fill = Color.ORANGE
                                font = Font.font(13.0)
                            }
                        } else {
                            Text("Price N/A ").apply {
                                fill = Color.web(TEXT_COLOR)
                                font = Font.font(13.0)
                            }
                        }

                        val profit = item.value - item.totalRecipeCost
                        val profitText = if (item.value > 0 && item.totalRecipeCost > 0) {
                            val sign = if (profit >= 0) "+" else ""
                            Text("($sign${String.format("%,.1f", profit)}) ").apply {
                                fill = if (profit >= 0) Color.GREEN else Color.RED
                                font = Font.font(13.0)
                            }
                        } else {
                            Text("(N/A) ").apply {
                                fill = Color.web("#666666")
                                font = Font.font(13.0)
                            }
                        }

                        val phText = if (item.profitPerHour != 0.0) {
                            val sign = if (item.profitPerHour >= 0) "+" else ""
                            Text("[$sign${String.format("%,.1f", item.profitPerHour)}/hr]").apply {
                                fill = if (item.profitPerHour >= 0) Color.GREEN else Color.RED
                                font = Font.font(13.0)
                            }
                        } else {
                            Text("[N/A/hr]").apply {
                                fill = Color.web("#666666")
                                font = Font.font(13.0)
                            }
                        }

                        val separator = Text(" - ").apply {
                            fill = Color.web("#666666")
                            font = Font.font(13.0)
                        }
                        val textFlow = TextFlow(nameText, priceText, profitText, separator, phText).apply {
                            padding = Insets(8.0, 10.0, 8.0, 10.0)
                        }

                        val line = Separator().apply {
                            style = "-fx-background-color: #333333; -fx-opacity: 0.3;"
                        }

                        graphic = VBox(textFlow, line)
                        style = "-fx-background-color: $DARKER_BG;"
                    }
                }
            }
        }
    }

    fun setItems(items: List<Item>) {
        listView.items.setAll(items)
        listView.refresh()
    }

    fun onItemSelected(handler: (Item?) -> Unit) {
        listView.selectionModel.selectedItemProperty().addListener { _, _, newItem ->
            handler(newItem)
        }
    }

    fun getSelectedItem(): Item? = listView.selectionModel.selectedItem
}