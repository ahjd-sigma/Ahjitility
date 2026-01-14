package ui.kat

import business.kat.KatFamilyResult
import business.kat.KatUpgradeCard
import business.kat.MaterialCost
import business.kat.PriceSource
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.FlowPane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import utils.*

class KatListView(private val onToggleExclude: (String) -> Unit) {
    private val items = FXCollections.observableArrayList<KatFamilyResult>()
    
    val node = ListView(items).apply {
        style = "-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;"
        setCellFactory {
            object : ListCell<KatFamilyResult>() {
                override fun updateItem(item: KatFamilyResult?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        graphic = null
                        style = "-fx-background-color: transparent;"
                    } else {
                        graphic = createFamilyBox(item)
                        style = "-fx-background-color: transparent; -fx-padding: 0 0 ${KatUIConfig.familyBoxPadding} 0;"
                    }
                }
            }
        }
        // Remove selection highlights
        selectionModel = null
        
        // Add advanced scrolling
        enableAdvancedScrolling { GeneralConfig.katScrollMultiplier }
    }

    fun updateItems(results: List<KatFamilyResult>) {
        items.setAll(results)
    }

    private fun createFamilyBox(family: KatFamilyResult) = vbox(KatUIConfig.familyBoxSpacing) {
        style = """
            -fx-background-color: ${if (family.isExcluded) KatUIConfig.familyBoxBgExcluded else KatUIConfig.familyBoxBgNormal};
            -fx-background-radius: ${KatUIConfig.borderRadiusLarge};
            -fx-padding: ${KatUIConfig.familyBoxPadding};
            -fx-border-color: ${if (family.isExcluded) KatUIConfig.accentRed else "transparent"};
            -fx-border-width: ${if (family.isExcluded) KatUIConfig.borderWidthThick else 0.0};
            -fx-border-radius: ${KatUIConfig.borderRadiusLarge};
            -fx-opacity: ${if (family.isExcluded) KatUIConfig.familyBoxOpacityExcluded else 1.0};
        """.trimIndent()
        
        // Determine the highest rarity color for the family name
        val highestRarity = family.upgradeCards
            .map { it.endRarity.uppercase() }
            .maxByOrNull { KatConfig.rarities.indexOf(it) } ?: "COMMON"
        val familyColor = KatUIConfig.rarityColors[highestRarity] ?: KatUIConfig.rarityColorFallback

        children.addAll(
            hbox(KatUIConfig.spacingSmall) {
                children.addAll(
                    family.family.name.replace("_", " ").label(familyColor, KatUIConfig.fontSizeExtraLarge, true),
                    spacer(),
                    (if (family.isExcluded) "Include" else "Exclude").button(onClick = { onToggleExclude(family.family.name) }).apply {
                        style = """
                            -fx-background-color: ${if (family.isExcluded) KatUIConfig.accentGreen else KatUIConfig.accentRed};
                            -fx-text-fill: white;
                            -fx-font-weight: bold;
                            -fx-font-size: ${KatUIConfig.fontSizeSmall}px;
                            -fx-background-radius: ${KatUIConfig.borderRadiusSmall};
                        """.trimIndent()
                    }
                )
            },
            FlowPane(KatUIConfig.cardSpacing, KatUIConfig.cardSpacing).apply {
                hgap = KatUIConfig.cardSpacing
                vgap = KatUIConfig.cardSpacing
                children.addAll(family.upgradeCards.map { createCard(it, family.isExcluded) })
            }
        )
    }



    private fun createCardContainer(isExcluded: Boolean, profit: Double, isCraftOnly: Boolean) = vbox(KatUIConfig.spacingSmall) {
        padding = Insets(KatUIConfig.cardPadding)
        minWidth = KatUIConfig.cardMinWidth
        style = """
            -fx-background-color: ${if (isCraftOnly) KatUIConfig.craftCardBg else KatUIConfig.upgradeCardBg};
            -fx-background-radius: ${KatUIConfig.borderRadiusMedium};
            -fx-border-color: ${if (isExcluded) KatUIConfig.craftCardBorderExcluded else if (profit > 0) KatUIConfig.craftCardBorderProfit else KatUIConfig.craftCardBorderLoss};
            -fx-border-radius: ${KatUIConfig.borderRadiusMedium};
            -fx-border-width: ${KatUIConfig.borderWidthThin};
        """.trimIndent()
    }

    private fun createSalesDisplay(sales: Double?, isExcluded: Boolean, alignment: Pos = Pos.CENTER) = hbox(KatUIConfig.spacingTiny, alignment) {
        children.addAll(
            "Sales/hr:".label(color = KatUIConfig.labelColorMuted, size = "${KatUIConfig.fontSizeExtraSmall}px"),
            valueLabel(
                value = sales,
                format = "%.1f",
                excluded = isExcluded,
                color = KatUIConfig.accentBlue,
                size = KatUIConfig.fontSizeExtraSmall + 1,
                bold = true
            )
        )
    }

    private fun createMaterialList(materials: List<MaterialCost>, isExcluded: Boolean, fee: Double? = null) = vbox(KatUIConfig.spacingTiny) {
        children.add("Materials:".label(
            color = KatUIConfig.labelColorSecondary,
            size = "${KatUIConfig.fontSizeNormal}px",
            bold = true
        ))
        
        // Add Kat Fee (Coins) if provided
        fee?.let {
            children.add(hbox(KatUIConfig.spacingSmall) {
                children.addAll(
                    "Kat Fee:".label(
                        color = KatUIConfig.labelColorPrimary,
                        size = "${KatUIConfig.fontSizeSmall}px"
                    ),
                    spacer(),
                    (if (isExcluded) "N/A" else String.format(KatUIConfig.formatCoins, it)).label(
                        color = KatUIConfig.labelColorSecondary,
                        size = "${KatUIConfig.fontSizeSmall}px"
                    )
                )
            })
        }
        
        materials.forEach { material ->
            children.add(hbox(KatUIConfig.spacingSmall) {
                children.addAll(
                    "• ${material.displayName} x${material.quantity}".label(
                        color = KatUIConfig.labelColorPrimary,
                        size = "${KatUIConfig.fontSizeSmall}px"
                    ),
                    if (material.isBazaar || material.totalPrice > 0) { // Only show source for craft-only or if price known
                        (if (material.isBazaar) "BZ" else "AH").label(
                            color = if (material.isBazaar) KatUIConfig.accentCyan else KatUIConfig.accentMagenta,
                            size = "${KatUIConfig.fontSizeExtraSmall}px",
                            bold = true
                        ).apply {
                            padding = Insets(0.0, 0.0, 0.0, KatUIConfig.paddingTiny)
                        }
                    } else "".label(),
                    spacer(),
                    valueLabel(
                        value = material.totalPrice,
                        format = KatUIConfig.formatPrice,
                        excluded = isExcluded,
                        color = KatUIConfig.labelColorSecondary,
                        size = KatUIConfig.fontSizeSmall
                    )
                )
            })
        }
    }

    private fun createCard(card: KatUpgradeCard, isExcluded: Boolean) = 
    createCardContainer(isExcluded, card.expectedProfit, card.isCraftOnly).apply {
        children += when {
            card.isCraftOnly -> createCraftContent(card, isExcluded)
            else -> createUpgradeContent(card, isExcluded)
        }
    }

private fun createCraftContent(card: KatUpgradeCard, isExcluded: Boolean) = 
    vbox(KatUIConfig.spacingTiny) {
        alignment = Pos.CENTER
        children.addAll(
            KatConfig.rarities.first().label(size = KatUIConfig.fontSizeLarge, bold = true),
            hbox(KatUIConfig.spacingSmall, Pos.CENTER) {
                children.addAll(
                    "AH Price:".label(KatUIConfig.labelColorSecondary, KatUIConfig.fontSizeSmall),
                    priceLabel(card.endPrice, KatUIConfig.formatPrice, isExcluded, KatUIConfig.accentGreen)
                )
            }
        )
        if (card.endHourlySales != null) {
            children.add(createSalesDisplay(card.endHourlySales, isExcluded))
        }
        
        // Craft label
        children.add(hbox(KatUIConfig.spacingSmall, Pos.CENTER) {
            padding = Insets(KatUIConfig.paddingSmall, 0.0, KatUIConfig.paddingSmall, 0.0)
            children.add("Craft".label(KatUIConfig.accentBlue, KatUIConfig.fontSizeSmall, true).apply {
                style += " -fx-background-color: ${KatUIConfig.instantCraftBg}; -fx-padding: ${KatUIConfig.paddingTiny} ${KatUIConfig.paddingSmall * 1.5}; -fx-background-radius: ${KatUIConfig.borderRadiusSmall};"
            })
        })
        
        // Material list
        children.add(createMaterialList(card.materialsBreakdown, isExcluded))
        
        // Separator
        children.add(Line(0.0, 0.0, KatUIConfig.cardSeparatorWidth, 0.0).apply {
            stroke = Color.valueOf(KatUIConfig.cardSeparator)
            strokeWidth = KatUIConfig.borderWidthThin
        })
        
        // Summary
        children.add(createProfitSummary(card, isExcluded))
    }

    private fun createSummaryRow(label: String, value: Double, format: String, color: String, isExcluded: Boolean, isBold: Boolean = false) = hbox(KatUIConfig.spacingSmall) {
        children.addAll(
            label.label().apply { style = KatUIConfig.styleLabelSecondary },
            spacer(),
            valueLabel(
            value = value,
            format = format,
            excluded = isExcluded,
            color = color,
            size = if (isBold) KatUIConfig.fontSizeNormal else KatUIConfig.fontSizeSmall,
            bold = isBold
        )
        )
    }

    private fun createUpgradeContent(card: KatUpgradeCard, isExcluded: Boolean) = 
    vbox(KatUIConfig.spacingSmall) {
        children.add(hbox(KatUIConfig.spacingSmall, Pos.CENTER) {
            children.addAll(
                createRaritySection(card.startRarity, card.startPrice, card.startPriceSource, card.startHourlySales, isExcluded),
                "→".label(KatUIConfig.labelColorSecondary, KatUIConfig.fontSizeSmall),
                createRaritySection(card.endRarity, card.endPrice, card.endPriceSource, card.endHourlySales, isExcluded)
            )
        })
        
        children.addAll(
            createDurationDisplay(card, isExcluded),
            createMaterialList(card.materialsBreakdown, isExcluded, card.recipe.cost.toDouble()),
            createTimeReductionDisplay(card, isExcluded),
            createPreviousTierDisplay(card, isExcluded),
            Line(0.0, 0.0, KatUIConfig.cardSeparatorWidth, 0.0).apply {
                stroke = Color.valueOf(KatUIConfig.cardSeparator)
                strokeWidth = KatUIConfig.borderWidthThin
            },
            createProfitSummary(card, isExcluded)
        )
    }

private fun createRaritySection(rarity: String, price: Double?, source: PriceSource, sales: Double?, isExcluded: Boolean) = 
    vbox(KatUIConfig.spacingTiny) {
        alignment = Pos.CENTER
        children.addAll(
            rarity.label(KatUIConfig.rarityColors[rarity] ?: KatUIConfig.rarityColorFallback, KatUIConfig.fontSizeMedium, true),
            hbox(KatUIConfig.spacingTiny, Pos.CENTER) {
                children.addAll(
                    priceLabel(price, KatUIConfig.formatPrice, isExcluded),
                    if (!isExcluded && source != PriceSource.UNKNOWN) {
                        (if (source == PriceSource.AH) "AH" else "Craft").label(
                            if (source == PriceSource.AH) KatUIConfig.accentBlue else KatUIConfig.accentWarning,
                            KatUIConfig.fontSizeExtraSmall,
                            true
                        )
                    } else "".label()
                )
            }
        )
        if (!isExcluded && sales != null) {
            children.add(createSalesDisplay(sales, false))
        }
    }

    private fun createDurationDisplay(card: KatUpgradeCard, isExcluded: Boolean) = hbox(KatUIConfig.spacingSmall) {
        val skipSeconds = ((card.baseDuration - card.reducedDuration) * 3600).toInt()
        children.addAll(
            "Time:".label().apply { style = KatUIConfig.styleLabelSecondaryBold },
            (if (isExcluded) "N/A" else formatDuration(skipSeconds)).label().apply {
                style = "${KatUIConfig.styleLabelNormalBold} -fx-text-fill: ${if (card.reducedDuration <= KatConfig.targetMaxDurationHours) KatUIConfig.accentGreen else KatUIConfig.accentWarning};"
            }
        )
    }

    private fun createTimeReductionDisplay(card: KatUpgradeCard, isExcluded: Boolean) = vbox(KatUIConfig.spacingTiny) {
        if (card.flowerCount > 0 || card.bouquetCount > 0) {
            children.add("Time Reduction:".label().apply {
                style = KatUIConfig.styleLabelSecondaryBold
            })

            if (card.flowerCount > 0) {
                children.add(createReductionRow("Kat Flowers: ${card.flowerCount}", card.totalFlowerCost, isExcluded))
            }

            if (card.bouquetCount > 0) {
                children.add(createReductionRow("Kat Bouquets: ${card.bouquetCount}", card.totalBouquetCost, isExcluded))
            }
        }
    }

    private fun createReductionRow(label: String, cost: Double, isExcluded: Boolean) = hbox(KatUIConfig.spacingSmall) {
        children.addAll(
            label.label().apply {
                style = KatUIConfig.styleLabelSmallYellow
            },
            spacer(),
            valueLabel(
                value = cost,
                format = KatUIConfig.formatPrice,
                excluded = isExcluded,
                color = KatUIConfig.labelColorSecondary,
                size = KatUIConfig.fontSizeExtraSmall
            )
        )
    }

    private fun createPreviousTierDisplay(card: KatUpgradeCard, isExcluded: Boolean) = hbox(KatUIConfig.spacingSmall) {
        children.addAll(
            "Previous Tier Cost:".label().apply {
                style = KatUIConfig.styleLabelSmall
            },
            if (!isExcluded && card.previousTierSource != PriceSource.UNKNOWN) {
                card.previousTierSource.name.label().apply {
                    style = "${KatUIConfig.styleBadgeSource} -fx-text-fill: ${if (card.previousTierSource == PriceSource.AH) KatUIConfig.accentBlue else KatUIConfig.accentWarning}; ${KatUIConfig.styleLabelExtraSmallBold}"
                }
            } else "".label(),
            spacer(),
            valueLabel(
                value = card.previousTierCost,
                format = KatUIConfig.formatPrice,
                excluded = isExcluded,
                color = KatUIConfig.labelColorSecondary,
                size = KatUIConfig.fontSizeNormal
            )
        )
    }

    private fun createProfitSummary(card: KatUpgradeCard, isExcluded: Boolean) = vbox(KatUIConfig.spacingSmall / 1.5) {
        val totalCostLabel = if (card.isCraftOnly) "Total Craft Cost:" else "Total Cost:"
        children.addAll(
            createSummaryRow(totalCostLabel, card.totalCost, KatUIConfig.formatPrice, KatUIConfig.costRed, isExcluded),
            createSummaryRow("Expected Profit:", card.expectedProfit, KatUIConfig.formatPrice, if (!isExcluded && card.expectedProfit > 0) KatUIConfig.accentGreen else if (!isExcluded && card.expectedProfit < 0) KatUIConfig.accentRed else KatUIConfig.labelColorSecondary, isExcluded, true),
            createSummaryRow("Margin:", card.profitMargin, KatUIConfig.formatPercent, if (!isExcluded && card.profitMargin > 0) KatUIConfig.accentGreen else if (!isExcluded && card.profitMargin < 0) KatUIConfig.accentRed else KatUIConfig.labelColorSecondary, isExcluded, true),
            createSummaryRow("Market Profit/hr:", card.expectedHourlyMarketProfit ?: 0.0, KatUIConfig.formatPrice, if (!isExcluded && (card.expectedHourlyMarketProfit ?: 0.0) > 0) KatUIConfig.accentGreen else if (!isExcluded && (card.expectedHourlyMarketProfit ?: 0.0) < 0) KatUIConfig.accentRed else KatUIConfig.labelColorSecondary, isExcluded || card.expectedHourlyMarketProfit == null, true)
        )
    }
}