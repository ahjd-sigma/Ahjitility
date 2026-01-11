package ui.forge

import business.forge.SourcePriority
import business.forge.ForgeResult
import utils.PriceFetcher
import utils.Styles
import utils.*
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.text.TextFlow

class RecipeSidebar(private val priceFetcher: PriceFetcher) {

    private val titleLabel = "Select an item".label().apply {
        style = "-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;"
    }
    
    private val content = VBox(8.0)

    val node = VBox(10.0).apply {
        padding = Insets(15.0)
        style = "-fx-background-color: ${Styles.DARK_BG};"
        children.addAll(titleLabel, separator().apply { style = "-fx-background-color: #444; -fx-opacity: 0.5;" }, content)
        VBox.setVgrow(content, Priority.ALWAYS)
        minWidth = UIConfig.SIDEBAR_WIDTH
        prefWidth = UIConfig.SIDEBAR_WIDTH
    }

    fun showRecipe(result: ForgeResult?, priority: SourcePriority = SourcePriority.ALL) {
        content.children.clear()
        if (result == null) {
            titleLabel.text = "Select an item"
            return
        }

        val recipe = result.recipe
        titleLabel.text = "Recipe: ${recipe.displayName}"
        
        // Sell Info
        content.children.add(TextFlow(
            text("Total Sell Price: ", Styles.TEXT_COLOR),
            text("${String.format("%,.0f", result.sellValue)} ", "orange"),
            text("(after ${String.format("%.1f", result.taxRate)}% tax)", "#888888", 11.0)
        ))
        content.children.add(separator().apply { style = "-fx-background-color: #444; -fx-opacity: 0.5;" })

        // Ingredients
        // Note: result.totalCost includes slots multiplier.
        // We want to show per-recipe ingredients but maybe total cost?
        // Let's show total for all slots as that's what the user cares about.
        
        // Coin cost
        val cost = recipe.coinCost ?: 0
        if (cost > 0) {
            // We need to know slots to show correct coin cost.
            // ForgeResult doesn't store slots explicitly but we can deduce or just show base.
            // Wait, result.totalCost is total.
            // Let's assume we want to show the breakdown for the *Total* batch.
            // But we don't have slots count in ForgeResult directly.
            // We can infer it or pass it. 
            // Actually, let's just show the base recipe ingredients and their current prices.
            
            content.children.add(TextFlow(
                text("• Base Coins: ", Styles.TEXT_COLOR), 
                text(String.format("%,d", cost), "orange")
            ))
        }

        recipe.ingredients.forEach { ing ->
            // We need to fetch the price again to show it.
            // We don't know if the user selected instant buy or buy order from here strictly speaking
            // without the config, but we can assume Instant Buy for display or check the result context if we added it.
            // For now, let's just show the LBIN/Bazaar price.
            
            val priceData = priceFetcher.getBuyPrice(ing.itemId, true, priority) // Use the priority!
            val totalItemCost = priceData.price * ing.quantity
            
            content.children.add(VBox(2.0).apply {
                children.addAll(
                    TextFlow(
                        text("• ${ing.quantity} x ", Styles.TEXT_COLOR),
                        text(ing.displayName ?: ing.itemId, Styles.TEXT_COLOR, bold = true)
                    ),
                    TextFlow(
                        text("  Cost: ", "#888888", 12.0),
                        text(String.format("%,.0f", totalItemCost), "orange", 12.0),
                        text(" (${String.format("%,.0f", priceData.price)}/ea)", "#666666", 11.0)
                    )
                )
            })
        }

        // Summary
        content.children.add(separator().apply { style = "-fx-background-color: #444;" })
        
        content.children.addAll(
            summaryLine("Total Cost:", result.totalCost, "orange"),
            summaryLine("Total Profit:", result.profit, if (result.profit >= 0) "green" else "red"),
            summaryLine("Profit / Hr:", result.profitPerHour, if (result.profitPerHour >= 0) "green" else "red")
        )
    }

    private fun summaryLine(label: String, value: Double, color: String) = TextFlow(
        text("$label ", Styles.TEXT_COLOR, bold = true),
        text(String.format("%,.0f", value), color, bold = true)
    )
}
