package ui.forge

import business.forge.SourcePriority
import business.forge.ForgeResult
import utils.*
import javafx.geometry.Insets
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.TextFlow

class RecipeSidebar(private val priceFetcher: PriceFetcher) {

    private val titleLabel = "Select an item".label(size = "18px", bold = true, color = Styles.ACCENT)
    
    private val content = vbox(12.0)

    val node = vbox(15.0) {
        padding = Insets(20.0)
        style = "-fx-background-color: ${Styles.DARK_BG};"
        children.addAll(
            titleLabel,
            content
        )
        VBox.setVgrow(content, Priority.ALWAYS)
        minWidth = GeneralConfig.sidebarWidth
        prefWidth = GeneralConfig.sidebarWidth
    }

    private fun sectionHeader(title: String) = title.label(color = "#888888", size = "12px", bold = true).apply {
        padding = Insets(5.0, 0.0, 0.0, 0.0)
    }

    private fun infoCard(init: VBox.() -> Unit) = vbox(8.0) {
        padding = Insets(12.0)
        style = " -fx-background-color: ${Styles.DARKER_BG}; -fx-background-radius: 8; -fx-border-color: #444; -fx-border-radius: 8; -fx-border-width: 1;"
        init()
    }

    private fun infoRow(label: String, value: String, valueColor: String = "white") = hbox(5.0) {
        children.addAll(
            label.label(color = "#aaaaaa", size = 13),
            value.label(color = valueColor, size = 13, bold = true)
        )
    }

    fun showRecipe(result: ForgeResult?, priority: SourcePriority = SourcePriority.ALL) {
        content.children.clear()
        if (result == null) {
            titleLabel.text = "Select an item"
            content.children.add(vbox(20.0, javafx.geometry.Pos.CENTER) {
                children.add("Choose a recipe from the list to see details".label(color = "#666", size = "14px").apply {
                    isWrapText = true
                    textAlignment = javafx.scene.text.TextAlignment.CENTER
                })
            })
            return
        }

        val recipe = result.recipe
        titleLabel.text = recipe.displayName
        
        // --- Details Section ---
        content.children.add(sectionHeader("DETAILS"))
        content.children.add(infoCard {
            children.addAll(
                infoRow("Total Sell Price:", String.format("%,.0f", result.sellValue), "orange"),
                infoRow("Tax Rate:", String.format("%.1f%%", result.taxRate), "#888888"),
                infoRow("Duration:", formatDuration(result.effectiveDuration), "cyan")
            )
        })

        // --- Ingredients Section ---
        content.children.add(sectionHeader("INGREDIENTS"))
        
        val ingredientsBox = vbox(8.0)
        
        // Base Coin Cost if any
        val coinCost = recipe.coinCost ?: 0
        if (coinCost > 0) {
            ingredientsBox.children.add(infoCard {
                children.add(infoRow("Base Coins:", String.format("%,d", coinCost), "orange"))
            })
        }

        recipe.ingredients.forEach { ing ->
            val priceData = priceFetcher.getBuyPrice(ing.itemId, true, priority) 
            val totalItemCost = priceData.price * ing.quantity
            
            ingredientsBox.children.add(infoCard {
                children.addAll(
                    TextFlow(
                        text("${ing.quantity}x ", "cyan", bold = true),
                        text(ing.displayName ?: ing.itemId, "white", bold = true)
                    ),
                    hbox(5.0) {
                        children.addAll(
                            text("Cost: ", "#888888", 12.0),
                            text(String.format("%,.0f", totalItemCost), "orange", 12.0, bold = true),
                            text(" (${String.format("%,.0f", priceData.price)}/ea)", "#666666", 11.0)
                        )
                    }
                )
            })
        }
        content.children.add(ingredientsBox)

        // --- Summary Section ---
        content.children.add(sectionHeader("PROFIT SUMMARY"))
        
        val profitColor = if (result.profit >= 0) "green" else "red"
        content.children.add(vbox(8.0) {
            padding = Insets(15.0)
            style = " -fx-background-color: ${if (result.profit >= 0) "rgba(0, 255, 0, 0.05)" else "rgba(255, 0, 0, 0.05)"}; -fx-background-radius: 10; -fx-border-color: ${if (result.profit >= 0) "green" else "red"}; -fx-border-radius: 10; -fx-border-width: 1;"
            
            children.addAll(
                summaryLine("Total Cost:", result.totalCost, "orange"),
                summaryLine("Net Profit:", result.profit, profitColor, 16.0),
                summaryLine("Profit / Hr:", result.profitPerHour, profitColor, 14.0)
            )
        })
    }

    private fun summaryLine(label: String, value: Double, color: String, size: Double = 14.0) = TextFlow(
        text("$label ", Styles.TEXT_COLOR, size, bold = true),
        text(String.format("%,.0f", value), color, size, bold = true)
    )
}
