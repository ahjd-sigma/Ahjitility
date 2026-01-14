package ui.settings

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import utils.*
import kotlin.reflect.KMutableProperty0

class SettingsUI(private val onBack: () -> Unit) {
    private val root = vbox(0.0)
    private val header = hbox(20.0)
    private val contentWrapper = vbox(20.0)
    private val innerWrapper = vbox(15.0)
    private val optionsLabel = "Options".label()
    private val subLabel = "Configure your preferences and settings".label()
    private val tabPane = TabPane()
    private val configListener = { refreshStyles() }

    init {
        ConfigEvents.subscribe(configListener)
    }

    private fun refreshStyles() {
        root.style = """
            -fx-background-color: ${Styles.DARK_BG};
            -fx-accent-color: ${GeneralConfig.colorAccentBlue};
            -fx-dark-bg: ${GeneralConfig.colorDarkBg};
            -fx-darker-bg: ${GeneralConfig.colorDarkerBg};
            -fx-text-primary: ${GeneralConfig.colorTextPrimary};
            -fx-field-bg: ${GeneralConfig.colorFieldBg};
            -fx-button-bg: ${GeneralConfig.colorButtonBg};
            -fx-border-color-global: ${GeneralConfig.colorBorder};
        """.trimIndent()
        
        header.style = "-fx-background-color: ${Styles.DARKER_BG}; -fx-border-color: #444444; -fx-border-width: 0 0 1 0;"
        contentWrapper.style = "-fx-background-color: ${Styles.DARK_BG};"
        innerWrapper.style = "-fx-background-color: ${Styles.DARKER_BG}; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #444444;"
        optionsLabel.style = "-fx-text-fill: ${GeneralConfig.colorAccentBlue}; -fx-font-size: 22px; -fx-font-weight: bold;"
        subLabel.style = "-fx-text-fill: ${GeneralConfig.colorTextPrimary}; -fx-font-size: 12px;"
    }

    private fun refreshTabs() {
        val selectedIndex = tabPane.selectionModel.selectedIndex
        tabPane.tabs.setAll(
            createTab("General", createGeneralSettings()),
            createTab("Kat Config", createKatConfigSettings()),
            createTab("Kat UI", createKatUISettings()),
            createTab("Forge Config", createForgeConfigSettings()),
            createTab("Forge UI", createForgeUISettings()),
            createTab("Shard Config", createShardConfigSettings()),
            createTab("Shard UI", createShardUISettings())
        )
        if (selectedIndex >= 0) {
            tabPane.selectionModel.select(selectedIndex)
        }
    }
    
    fun createScene(): Scene {
        tabPane.apply {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tabMinWidth = 120.0
            tabMaxWidth = 200.0
            refreshTabs()
        }

        header.apply {
            padding = Insets(16.0, 20.0, 16.0, 20.0)
            alignment = Pos.CENTER_LEFT
            spacing = 12.0
            children.addAll(
                "← Back".button(onClick = { onBack() }),
                vbox(2.0) {
                    children.addAll(optionsLabel, subLabel)
                },
                spacer()
            )
        }

        contentWrapper.apply {
            padding = Insets(16.0, 20.0, 20.0, 20.0)
            children.add(
                innerWrapper.apply {
                    padding = Insets(16.0)
                    children.add(tabPane)
                }
            )
            VBox.setVgrow(this, Priority.ALWAYS)
        }

        root.apply {
            children.addAll(
                header,
                contentWrapper
            )
        }

        refreshStyles()

        return Scene(root, 900.0, 700.0).apply {
            stylesheets.add(SettingsUI::class.java.getResource("/style.css")?.toExternalForm())
        }
    }

    private fun createTab(title: String, content: Node): Tab {
        return Tab(title).apply {
            val scroll = ScrollPane(content).apply {
                applyDarkStyle()
                enableAdvancedScrolling()
                isFitToWidth = true
            }
            this.content = scroll
        }
    }

    private fun createGeneralSettings() = vbox(15.0) {
        padding = Insets(20.0)
        children.addAll(
            sectionHeader("Global Colors"),
            colorSetting("Dark BG", GeneralConfig::colorDarkBg),
            colorSetting("Darker BG", GeneralConfig::colorDarkerBg),
            colorSetting("Field BG", GeneralConfig::colorFieldBg),
            colorSetting("Button BG", GeneralConfig::colorButtonBg),
            colorSetting("Border Color", GeneralConfig::colorBorder),
            colorSetting("Separator Color", GeneralConfig::colorSeparator),
            colorSetting("Text Primary", GeneralConfig::colorTextPrimary),
            colorSetting("Accent Blue", GeneralConfig::colorAccentBlue),
            colorSetting("Accent Orange", GeneralConfig::colorAccentOrange),
            colorSetting("Accent Red (Error)", GeneralConfig::colorAccentRed),
            colorSetting("Accent Green (Success)", GeneralConfig::colorAccentGreen),

            sectionHeader("Logging"),
            booleanSetting("Debug Mode", GeneralConfig::debugMode),

            resetButton { GeneralConfig.resetToDefaults() }
        )
    }

    private fun createKatConfigSettings() = vbox(15.0) {
        padding = Insets(20.0)
        children.addAll(
            sectionHeader("Time Reduction"),
            booleanSetting("Enable Time Reduction", KatConfig::enableTimeReduction),
            doubleSetting("Target Duration (Hours)", KatConfig::targetMaxDurationHours),
            intSetting("Max Flowers", KatConfig::maxFlowers),
            intSetting("Max Bouquets", KatConfig::maxBouquets),
            
            sectionHeader("Kat Flower Pricing"),
            booleanSetting("Force Fixed Price", KatConfig::forceFlowerPrice),
            doubleSetting("Flower Price", KatConfig::defaultFlowerPrice),
            
            sectionHeader("Kat Bouquet Pricing"),
            booleanSetting("Force Fixed Price", KatConfig::forceBouquetPrice),
            doubleSetting("Bouquet Price", KatConfig::defaultBouquetPrice),

            sectionHeader("Agatha Coupon Pricing"),
            booleanSetting("Force Fixed Price", KatConfig::forceAgathaCouponPrice),
            doubleSetting("Coupon Price", KatConfig::defaultAgathaCouponPrice),

            sectionHeader("Market"),
            doubleSetting("Bazaar Tax", KatConfig::bazaarTax),
            doubleSetting("AH Multiplier", KatConfig::ahMultiplier),

            resetButton { KatConfig.resetToDefaults() }
        )
    }

    private fun createKatUISettings() = vbox(15.0) {
        padding = Insets(20.0)
        children.addAll(
            sectionHeader("Layout"),
            doubleSetting("Card Min Width", KatUIConfig::cardMinWidth),
            doubleSetting("Card Padding", KatUIConfig::cardPadding),
            doubleSetting("Card Spacing", KatUIConfig::cardSpacing),
            
            sectionHeader("Colors"),
            colorSetting("Craft Card BG", KatUIConfig::craftCardBg),

            resetButton { KatUIConfig.resetToDefaults() }
        )
    }

    private fun createForgeConfigSettings() = vbox(15.0) {
        padding = Insets(20.0)
        children.addAll(
            sectionHeader("Defaults"),
            doubleSetting("Bazaar Tax", ForgeModuleConfig::defaultBazaarTax),
            doubleSetting("AH Multiplier", ForgeModuleConfig::defaultAhMultiplier),

            resetButton { ForgeModuleConfig.resetToDefaults() }
        )
    }

    private fun createForgeUISettings() = vbox(15.0) {
        padding = Insets(20.0)
        children.addAll(
            sectionHeader("Layout"),
            doubleSetting("Divider Position", ForgeUIConfig::dividerPosition),
            doubleSetting("Search Field Width", ForgeUIConfig::searchFieldWidth),
            
            resetButton { ForgeUIConfig.resetToDefaults() }
        )
    }

    private fun createShardConfigSettings() = vbox(15.0) {
        padding = Insets(20.0)
        children.addAll(
            sectionHeader("Defaults"),
            doubleSetting("Bazaar Tax", ShardConfig::defaultBazaarTax),
            doubleSetting("AH Multiplier", ShardConfig::defaultAhMultiplier),

            resetButton { ShardConfig.resetToDefaults() }
        )
    }

    private fun createShardUISettings() = vbox(15.0) {
        padding = Insets(20.0)
        children.addAll(
            sectionHeader("Layout"),
            doubleSetting("Divider Position", ShardUIConfig::dividerPosition),
            doubleSetting("Icon Size", ShardUIConfig::iconSize),
            
            resetButton { ShardUIConfig.resetToDefaults() }
        )
    }

    // Helper UI Builders
    private fun resetButton(onReset: () -> Unit) = hbox {
        padding = Insets(20.0, 0.0, 0.0, 0.0)
        alignment = Pos.CENTER_RIGHT
        children.add("Reset to Defaults".button(onClick = {
            onReset()
            refreshTabs()
        }).apply {
            style += " -fx-background-color: ${GeneralConfig.colorAccentRed}; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;"
        })
    }
    private fun sectionHeader(title: String) = title.label(
        color = GeneralConfig.colorAccentBlue,
        size = "16px",
        bold = true
    ).apply {
        style += " -fx-padding: 10 0 5 0;"
    }

    private fun settingRow(label: String, control: Node) = hbox(10.0) {
        alignment = Pos.CENTER_LEFT
        children.addAll(
            label.label(size = "14px").apply { 
                minWidth = 200.0
            },
            control
        )
    }

    private fun doubleSetting(label: String, prop: KMutableProperty0<Double>, scale: Double = 1.0) = settingRow(label, textField((prop.get() * scale).toString()).apply {
        prefWidth = 100.0
        textProperty().addListener { _, _, newVal -> 
            val value = newVal.toDoubleOrNull()
            if (value != null) {
                style = Styles.field
                prop.set(value / scale)
                saveAll()
            } else {
                style = Styles.field + "; -fx-border-color: ${GeneralConfig.colorAccentRed}; -fx-border-width: 1;"
            }
        }
    })

    private fun intSetting(label: String, prop: KMutableProperty0<Int>) = settingRow(label, textField(prop.get().toString()).apply {
        prefWidth = 100.0
        textProperty().addListener { _, _, newVal -> 
            val value = newVal.toIntOrNull()
            if (value != null) {
                style = Styles.field
                prop.set(value)
                saveAll()
            } else {
                style = Styles.field + "; -fx-border-color: ${GeneralConfig.colorAccentRed}; -fx-border-width: 1;"
            }
        }
    })

    private fun booleanSetting(label: String, prop: KMutableProperty0<Boolean>) = settingRow(label, CheckBox().apply {
        isSelected = prop.get()
        selectedProperty().addListener { _, _, newVal -> 
            prop.set(newVal)
            saveAll()
        }
    })

    private fun colorSetting(label: String, prop: KMutableProperty0<String>) = settingRow(label, hbox(10.0) {
        val picker = ColorPicker(javafx.scene.paint.Color.web(prop.get())).apply {
            style = "-fx-background-color: ${GeneralConfig.colorFieldBg}; -fx-color-label-visible: false;"
            setOnAction {
                prop.set("#" + value.toString().substring(2, 8))
                saveAll()
            }
        }
        children.add(picker)
    })

    private fun saveAll() {
        GeneralConfig.saveConfig()
        KatConfig.saveConfig()
        KatUIConfig.saveConfig()
        ForgeModuleConfig.saveConfig()
        ForgeUIConfig.saveConfig()
        ShardConfig.saveConfig()
        ShardUIConfig.saveConfig()
        
        ConfigEvents.fire()
    }
}
