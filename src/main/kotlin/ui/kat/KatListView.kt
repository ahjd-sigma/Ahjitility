package ui.kat

import business.kat.*
import javafx.geometry.Pos
import javafx.geometry.Insets
import javafx.scene.layout.VBox
import javafx.scene.layout.HBox
import javafx.scene.layout.FlowPane
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.scene.shape.Line

import javafx.collections.FXCollections
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.ScrollBar
import javafx.animation.AnimationTimer
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.input.ScrollEvent
import kotlin.math.abs
import utils.KatUIConfig
import utils.KatConfig
import utils.GeneralConfig
import utils.enableAdvancedScrolling
import business.kat.PriceSource

class KatListView(private val onToggleExclude: (String) -> Unit) {
    private val items = FXCollections.observableArrayList<KatFamilyResult>()
    
    val node = ListView<KatFamilyResult>(items).apply {
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
        enableAdvancedScrolling({ GeneralConfig.katScrollMultiplier })
    }

    fun updateItems(results: List<KatFamilyResult>) {
        items.setAll(results)
    }

    private fun createFamilyBox(result: KatFamilyResult) = VBox(KatUIConfig.familyBoxSpacing).apply {
        padding = Insets(KatUIConfig.familyBoxPadding)
        val isExcluded = result.isExcluded
        style = """
            -fx-background-color: ${if (isExcluded) KatUIConfig.familyBoxBgExcluded else KatUIConfig.familyBoxBgNormal};
            -fx-background-radius: ${KatUIConfig.borderRadiusLarge};
            -fx-border-color: ${if (isExcluded) KatUIConfig.familyBoxBorderExcluded else if (result.family.isFullFamily) KatUIConfig.accentBlue else KatUIConfig.familyBoxBorderPartial};
            -fx-border-radius: ${KatUIConfig.borderRadiusLarge};
            -fx-border-width: ${KatUIConfig.borderWidthThick};
            -fx-opacity: ${if (isExcluded) KatUIConfig.familyBoxOpacityExcluded else 1.0};
        """.trimIndent()

        children.addAll(
            HBox().apply {
                alignment = Pos.CENTER_LEFT
                children.addAll(
                    Label(if (result.family.isFullFamily) "Full Family" else "Partial").apply {
                        style = "-fx-text-fill: ${KatUIConfig.labelColorSecondary}; -fx-font-size: ${KatUIConfig.fontSizeLarge}px;"
                    },
                    Label(result.family.name).apply {
                        style = "-fx-text-fill: white; -fx-font-size: ${KatUIConfig.fontSizeExtraLarge}px; -fx-font-weight: bold;"
                    },
                    spacer(),
                    Button(if (isExcluded) "Include" else "Exclude").apply {
                        style = """
                            -fx-background-color: ${if (isExcluded) KatUIConfig.accentGreen else KatUIConfig.accentRed};
                            -fx-text-fill: white;
                            -fx-font-size: ${KatUIConfig.fontSizeSmall}px;
                            -fx-padding: ${KatUIConfig.paddingSmall / 2} ${KatUIConfig.paddingSmall * 1.5};
                            -fx-background-radius: ${KatUIConfig.borderRadiusSmall};
                        """.trimIndent()
                        setOnAction { onToggleExclude(result.family.name) }
                    }
                )
            },
            FlowPane(KatUIConfig.cardSpacing, KatUIConfig.cardSpacing).apply {
                padding = Insets(KatUIConfig.paddingSmall, 0.0, 0.0, 0.0)
                result.upgradeCards.forEach { card ->
                    children.add(createCard(card, isExcluded))
                }
            }
        )
    }



    private fun createCardContainer(isExcluded: Boolean, profit: Double, isCraftOnly: Boolean) = VBox(KatUIConfig.spacingSmall).apply {
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

    private fun createSalesDisplay(sales: Double?, isExcluded: Boolean, alignment: Pos = Pos.CENTER) = HBox(KatUIConfig.spacingTiny).apply {
        this.alignment = alignment
        children.addAll(
            Label("Sales/hr:").apply { style = "-fx-text-fill: ${KatUIConfig.labelColorMuted}; -fx-font-size: ${KatUIConfig.fontSizeExtraSmall}px;" },
            valueLabel(
                value = sales,
                format = "%.1f",
                excluded = isExcluded,
                color = KatUIConfig.accentBlue,
                size = KatUIConfig.fontSizeExtraSmall + 1
            ).apply {
                style = "-fx-font-weight: bold;"
            }
        )
    }

    private fun createMaterialList(materials: List<MaterialCost>, isExcluded: Boolean, fee: Double? = null) = VBox(KatUIConfig.spacingTiny).apply {
        children.add(Label("Materials:").apply {
            style = "-fx-text-fill: ${KatUIConfig.labelColorSecondary}; -fx-font-size: ${KatUIConfig.fontSizeNormal}px; -fx-font-weight: bold;"
        })
        
        // Add Kat Fee (Coins) if provided
        fee?.let {
            children.add(HBox(KatUIConfig.spacingSmall).apply {
                alignment = Pos.CENTER_LEFT
                children.addAll(
                    Label("Kat Fee:").apply {
                        style = "-fx-text-fill: ${KatUIConfig.labelColorPrimary}; -fx-font-size: ${KatUIConfig.fontSizeSmall}px;"
                    },
                    spacer(),
                    Label(if (isExcluded) "N/A" else String.format(KatUIConfig.formatCoins, it)).apply {
                        style = "-fx-text-fill: ${KatUIConfig.labelColorSecondary}; -fx-font-size: ${KatUIConfig.fontSizeSmall}px;"
                    }
                )
            })
        }
        
        materials.forEach { material ->
            children.add(HBox(KatUIConfig.spacingSmall).apply {
                alignment = Pos.CENTER_LEFT
                children.addAll(
                    Label("• ${material.displayName} x${material.quantity}").apply {
                        style = "-fx-text-fill: ${KatUIConfig.labelColorPrimary}; -fx-font-size: ${KatUIConfig.fontSizeSmall}px;"
                    },
                    if (material.isBazaar || material.totalPrice > 0) { // Only show source for craft-only or if price known
                        Label(if (material.isBazaar) "BZ" else "AH").apply {
                            style = "-fx-text-fill: ${if (material.isBazaar) KatUIConfig.accentCyan else KatUIConfig.accentMagenta}; -fx-font-size: ${KatUIConfig.fontSizeExtraSmall}px; -fx-font-weight: bold;"
                            padding = Insets(0.0, 0.0, 0.0, KatUIConfig.paddingTiny)
                        }
                    } else Label(),
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
            label(KatConfig.rarities.first(), size = KatUIConfig.fontSizeLarge, bold = true),
            hbox(KatUIConfig.spacingSmall, Pos.CENTER) {
                children.addAll(
                    label("AH Price:", KatUIConfig.labelColorSecondary, KatUIConfig.fontSizeSmall),
                    priceLabel(card.endPrice, KatUIConfig.formatPrice, isExcluded, KatUIConfig.accentGreen)
                )
            }
        )
        if (card.endHourlySales != null) {
            children.add(createSalesDisplay(card.endHourlySales, isExcluded))
        }
        
        // Instant craft label
        children.add(hbox(KatUIConfig.spacingSmall, Pos.CENTER) {
            padding = Insets(KatUIConfig.paddingSmall, 0.0, KatUIConfig.paddingSmall, 0.0)
            children.add(label("Instant Craft", KatUIConfig.accentBlue, KatUIConfig.fontSizeSmall, true).apply {
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
        children.add(vbox(KatUIConfig.spacingSmall / 1.5) {
            children.addAll(
                createSummaryRow("Total Craft Cost:", card.totalCost, KatUIConfig.formatPrice, KatUIConfig.costRed, isExcluded),
                createSummaryRow("Expected Profit:", card.expectedProfit, KatUIConfig.formatPrice, if (!isExcluded && card.expectedProfit > 0) KatUIConfig.accentGreen else KatUIConfig.accentRed, isExcluded, true),
                createSummaryRow("Margin:", card.profitMargin, KatUIConfig.formatPercent, if (!isExcluded && card.profitMargin > 0) KatUIConfig.accentGreen else KatUIConfig.accentRed, isExcluded, true)
            )
        })
    }

    private fun createSummaryRow(label: String, value: Double, format: String, color: String, isExcluded: Boolean, isBold: Boolean = false) = HBox(KatUIConfig.spacingSmall).apply {
        alignment = Pos.CENTER_LEFT
        children.addAll(
            Label(label).apply { style = KatUIConfig.styleLabelSecondary },
            spacer(),
            valueLabel(
                value = value,
                format = format,
                excluded = isExcluded,
                color = color,
                size = if (isBold) KatUIConfig.fontSizeNormal else KatUIConfig.fontSizeSmall
            ).apply {
                style = if (isBold) "-fx-font-weight: bold;" else ""
            }
        )
    }

    private fun createUpgradeContent(card: KatUpgradeCard, isExcluded: Boolean) = 
    vbox(KatUIConfig.spacingSmall) {
        children.add(hbox(KatUIConfig.spacingSmall, Pos.CENTER) {
            children.addAll(
                createRaritySection(card.startRarity, card.startPrice, card.startPriceSource, card.startHourlySales, isExcluded),
                label("→", KatUIConfig.labelColorSecondary, KatUIConfig.fontSizeSmall),
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
            label(rarity, KatUIConfig.rarityColors[rarity] ?: KatUIConfig.rarityColorFallback, KatUIConfig.fontSizeMedium, true),
            hbox(KatUIConfig.spacingTiny, Pos.CENTER) {
                children.addAll(
                    priceLabel(price, KatUIConfig.formatPrice, isExcluded),
                    if (!isExcluded && source != PriceSource.UNKNOWN) {
                        label(
                            if (source == PriceSource.AH) "AH" else "Craft",
                            if (source == PriceSource.AH) KatUIConfig.accentBlue else KatUIConfig.accentWarning,
                            KatUIConfig.fontSizeExtraSmall,
                            true
                        )
                    } else Label()
                )
            }
        )
        if (!isExcluded && sales != null) {
            children.add(createSalesDisplay(sales, isExcluded))
        }
    }

    private fun createDurationDisplay(card: KatUpgradeCard, isExcluded: Boolean) = HBox(KatUIConfig.spacingSmall).apply {
        alignment = Pos.CENTER_LEFT
        children.addAll(
            Label("Time:").apply { style = KatUIConfig.styleLabelSecondaryBold },
            Label(if (isExcluded) "N/A" else String.format(KatUIConfig.formatDuration, card.baseDuration)).apply {
                style = "${KatUIConfig.styleLabelNormalBold} -fx-text-fill: ${if (card.reducedDuration <= KatConfig.targetMaxDurationHours) KatUIConfig.accentGreen else KatUIConfig.accentWarning};"
            }
        )
    }

    private fun createTimeReductionDisplay(card: KatUpgradeCard, isExcluded: Boolean) = VBox(KatUIConfig.spacingTiny).apply {
        if (card.flowerCount > 0 || card.bouquetCount > 0) {
            children.add(Label("Time Reduction:").apply {
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

    private fun createReductionRow(label: String, cost: Double, isExcluded: Boolean) = HBox(KatUIConfig.spacingSmall).apply {
        alignment = Pos.CENTER_LEFT
        children.addAll(
            Label(label).apply {
                style = KatUIConfig.styleLabelSmallYellow
            },
            spacer(),
            valueLabel(
                value = cost,
                format = KatUIConfig.formatPrice,
                excluded = isExcluded,
                color = KatUIConfig.labelColorSecondary,
                size = KatUIConfig.fontSizeExtraSmall
            ).apply {
                style = KatUIConfig.styleLabelSmall
            }
        )
    }

    private fun createPreviousTierDisplay(card: KatUpgradeCard, isExcluded: Boolean) = HBox(KatUIConfig.spacingSmall).apply {
        alignment = Pos.CENTER_LEFT
        children.addAll(
            Label("Previous Tier Cost:").apply {
                style = KatUIConfig.styleLabelSmall
            },
            if (!isExcluded && card.previousTierSource != PriceSource.UNKNOWN) {
                Label(card.previousTierSource.name).apply {
                    style = "${KatUIConfig.styleBadgeSource} -fx-text-fill: ${if (card.previousTierSource == PriceSource.AH) KatUIConfig.accentBlue else KatUIConfig.accentWarning}; ${KatUIConfig.styleLabelExtraSmallBold}"
                }
            } else Label(),
            spacer(),
            valueLabel(
                value = card.previousTierCost,
                format = KatUIConfig.formatPrice,
                excluded = isExcluded,
                color = KatUIConfig.labelColorSecondary,
                size = KatUIConfig.fontSizeNormal
            ).apply {
                style = KatUIConfig.styleLabelNormal
            }
        )
    }

    private fun createProfitSummary(card: KatUpgradeCard, isExcluded: Boolean) = VBox(KatUIConfig.spacingSmall / 1.5).apply {
        children.addAll(
            createSummaryRow("Total Cost:", card.totalCost, KatUIConfig.formatPrice, KatUIConfig.costRed, isExcluded),
            createSummaryRow("Expected Profit:", card.expectedProfit, KatUIConfig.formatPrice, if (!isExcluded && card.expectedProfit > 0) KatUIConfig.accentGreen else KatUIConfig.accentRed, isExcluded, true),
            createSummaryRow("Margin:", card.profitMargin, KatUIConfig.formatPercent, if (!isExcluded && card.profitMargin > 0) KatUIConfig.accentGreen else KatUIConfig.accentRed, isExcluded, true),
            createSummaryRow("Market Profit/hr:", card.expectedHourlyMarketProfit ?: 0.0, KatUIConfig.formatPrice, if (!isExcluded && (card.expectedHourlyMarketProfit ?: 0.0) > 0) KatUIConfig.accentBlue else KatUIConfig.accentRed, isExcluded || card.expectedHourlyMarketProfit == null, true)
        )
    }
}