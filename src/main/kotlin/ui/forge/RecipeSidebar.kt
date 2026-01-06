package ui.forge

import forge.Item
import forge.PriceFetcher
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextFlow

class RecipeSidebar(private val priceFetcher: PriceFetcher) {
    private val DARK_BG = "#2b2b2b"
    private val TEXT_COLOR = "#cccccc"

    private val recipeTitle = Label("Select an item to see recipe").apply {
        style = "-fx-font-weight: bold; -fx-font-size: 17px; -fx-text-fill: #ffffff;"
    }

    private val ingredientsList = VBox(5.0)

    val node = VBox(10.0).apply {
        padding = Insets(15.0)
        style = "-fx-background-color: $DARK_BG;"
        children.addAll(
            recipeTitle,
            Separator().apply { style = "-fx-background-color: #444444;" },
            ingredientsList
        )
        VBox.setVgrow(ingredientsList, Priority.ALWAYS)
    }

    fun showRecipe(item: Item?, isInstantBuy: Boolean, slots: Int, quickForgeLevel: Int) {
        ingredientsList.children.clear()
        if (item == null) {
            recipeTitle.text = "Select an item to see recipe"
            return
        }

        recipeTitle.text = "Recipe: ${item.displayName}"

        val sellPriceLabel = Text("Sell Price: ").apply {
            fill = Color.web(TEXT_COLOR)
            font = Font.font(13.0)
        }
        val sellPriceValue = Text("${String.format("%,.0f", item.value)} coins").apply {
            fill = Color.ORANGE
            font = Font.font(13.0)
        }
        val taxInfo = Text(" (after ${String.format("%.2f", item.appliedTaxRate)}% tax)").apply {
            fill = Color.web("#888888")
            font = Font.font(11.0)
        }
        ingredientsList.children.add(TextFlow(sellPriceLabel, sellPriceValue, taxInfo))
        ingredientsList.children.add(Separator().apply { style = "-fx-background-color: #444444;" })

        val ingredients = item.ingredients
        if (ingredients.isNullOrEmpty()) {
            ingredientsList.children.add(Label("No recipe data available.").apply {
                style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;"
            })
            return
        }

        var totalRecipeCostSum = 0.0

        if (item.coinCost != null && item.coinCost > 0) {
            val totalCoinCost = item.coinCost.toLong() * slots
            totalRecipeCostSum += totalCoinCost
            val costLabel = Text("• $slots x Coins - ").apply {
                fill = Color.web(TEXT_COLOR)
                font = Font.font(13.0)
            }
            val costValue = Text("${String.format("%,d", totalCoinCost)} coins").apply {
                fill = Color.ORANGE
                font = Font.font(13.0)
            }
            ingredientsList.children.add(TextFlow(costLabel, costValue))
        }

        ingredients.forEach { ingredient ->
            val result = priceFetcher.getBuyPrice(ingredient.itemId, isInstantBuy)
            val unitPrice = result.price
            val source = if (result.isBazaar) " (BZ)" else " (AH)"
            val quantity = (ingredient.quantity ?: 0).toLong() * slots
            val totalCost = unitPrice * quantity
            totalRecipeCostSum += totalCost

            val ingredientText = Text("• $quantity x ${ingredient.displayName ?: ingredient.itemId}").apply {
                fill = Color.web(TEXT_COLOR)
                font = Font.font(13.0)
            }
            val priceFlow = TextFlow().apply {
                children.add(ingredientText)
                if (totalCost > 0) {
                    val unitPriceText = Text(" (${String.format("%,.0f", unitPrice)}/ea$source)").apply {
                        fill = Color.web("#888888")
                        font = Font.font(13.0)
                    }
                    val totalCostText = Text(" -> ${String.format("%,.0f", totalCost)} coins").apply {
                        fill = Color.ORANGE
                        font = Font.font(13.0)
                    }
                    children.addAll(unitPriceText, totalCostText)
                }
            }
            ingredientsList.children.add(priceFlow)
        }

        ingredientsList.children.add(Separator().apply { style = "-fx-background-color: #444444; -fx-margin: 5 0 5 0;" })
        val totalLabel = Text("Total Cost: ").apply {
            fill = Color.web(TEXT_COLOR)
            style = "-fx-font-weight: bold;"
            font = Font.font(13.0)
        }
        val totalValue = Text("${String.format("%,.0f", totalRecipeCostSum)} coins").apply {
            fill = Color.ORANGE
            style = "-fx-font-weight: bold;"
            font = Font.font(13.0)
        }
        ingredientsList.children.add(TextFlow(totalLabel, totalValue))

        if (item.durationSeconds != null) {
            val reduction = when {
                quickForgeLevel == 0 -> 0.0
                quickForgeLevel == 20 -> 0.30
                else -> 0.10 + (quickForgeLevel.toDouble() * 0.005)
            }
            val reducedSeconds = (item.durationSeconds.toDouble() * (1.0 - reduction)).toInt()

            val hours = reducedSeconds / 3600
            val minutes = (reducedSeconds % 3600) / 60
            ingredientsList.children.add(Label("Duration: ${hours}h ${minutes}m").apply {
                style = "-fx-text-fill: $TEXT_COLOR; -fx-font-size: 13px;"
            })
        }
    }
}