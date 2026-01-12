package utils

/**
 * UI configuration for the Kat module.
 */
object KatUIConfig : BaseConfig("kat_ui.yaml") {
    // Card dimensions
    var cardMinWidth: Double = 280.0
    var cardPadding: Double = 12.0
    var cardSpacing: Double = 10.0

    // Colors (Kat specific)
    var craftCardBg: String = "#2b2b2b"
    val craftCardBorderProfit get() = GeneralConfig.colorAccentGreen
    val craftCardBorderLoss get() = GeneralConfig.colorAccentRed
    var craftCardBorderExcluded: String = "#444444"
    var upgradeCardBg: String = "#2b2b2b"
    var instantCraftBg: String = "#1a2634"
    val costRed get() = GeneralConfig.colorAccentRed
    var cardSeparator: String = "#444444"
    var labelColorPrimary: String = "#ffffff"
    var labelColorSecondary: String = "#cccccc"
    var labelColorMuted: String = "#888888"
    val accentBlue get() = GeneralConfig.colorAccentBlue
    val accentGreen get() = GeneralConfig.colorAccentGreen
    val accentRed get() = GeneralConfig.colorAccentRed
    val accentOrange get() = GeneralConfig.colorAccentOrange
    val accentWarning get() = GeneralConfig.colorAccentOrange
    val accentCyan get() = GeneralConfig.colorAccentCyan
    val accentMagenta get() = GeneralConfig.colorAccentMagenta
    val colorFieldBg get() = GeneralConfig.colorFieldBg
    val colorButtonBg get() = GeneralConfig.colorButtonBg

    // Sizes and Spacing
    var fontSizeExtraSmall: Double = 11.0
    var fontSizeSmall: Double = 13.0
    var fontSizeNormal: Double = 14.0
    var fontSizeMedium: Double = 14.0
    var fontSizeLarge: Double = 16.0
    var fontSizeExtraLarge: Double = 20.0
    var fontSizeTitle: Double = 18.0
    var spacingTiny: Double = 5.0
    var spacingSmall: Double = 10.0
    var paddingTiny: Double = 2.0
    var paddingSmall: Double = 5.0
    var paddingMedium: Double = 10.0
    var borderRadiusSmall: Double = 4.0
    var borderRadiusMedium: Double = 6.0
    var borderRadiusLarge: Double = 8.0
    var borderWidthThin: Double = 1.0
    var borderWidthThick: Double = 2.0

    // Style Strings
    var styleLabelSecondaryBold: String = "-fx-text-fill: #cccccc; -fx-font-weight: bold; -fx-font-size: 13px;"
    var styleLabelSecondary: String = "-fx-text-fill: #cccccc; -fx-font-size: 13px;"
    var styleLabelSmallYellow: String = "-fx-text-fill: #FFD700; -fx-font-size: 12px;"
    var styleLabelSmall: String = "-fx-text-fill: #ffffff; -fx-font-size: 12px;"
    var styleLabelNormal: String = "-fx-text-fill: #ffffff; -fx-font-size: 14px;"
    var styleLabelNormalBold: String = "-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14px;"
    var styleLabelExtraSmallBold: String = "-fx-font-weight: bold; -fx-font-size: 10px;"
    var styleBadgeSource: String = "-fx-background-color: rgba(60,60,60,0.5); -fx-padding: 1 4; -fx-background-radius: 3;"

    var rarityColorFallback: String = "#cccccc"
    var rarityColors: Map<String, String> = mapOf(
        "COMMON" to "#ffffff",
        "UNCOMMON" to "#55ff55",
        "RARE" to "#5555ff",
        "EPIC" to "#aa00aa",
        "LEGENDARY" to "#ffaa00",
        "MYTHIC" to "#ff55ff"
    )

    // Family box settings
    var familyBoxPadding: Double = 15.0
    var familyBoxSpacing: Double = 10.0
    var familyBoxRadius: Double = 10.0
    var familyBoxOpacityExcluded: Double = 0.6
    var familyBoxBgExcluded: String = "#2b2b2b"
    var familyBoxBgNormal: String = "#3c3f41"
    var familyBoxBorderExcluded: String = "#444444"
    var familyBoxBorderPartial: String = "#888888"

    // Layout
    var mainPadding: Double = 10.0
    var mainSpacing: Double = 10.0
    var controlSpacing: Double = 15.0
    var controlGroupSpacing: Double = 20.0
    var labelSpacing: Double = 5.0
    var sidebarWidth: Double = 300.0
    
    // Formats
    var formatDuration: String = "%.1fh"
    var formatCoins: String = "%,.0f"
    var formatPrice: String = "%,.0f"
    var formatPercent: String = "%.1f%%"
    var formatSales: String = "%.1f"

    // Controls
    var searchFieldWidth: Double = 200.0
    var sortByWidth: Double = 200.0
    var rarityFilterWidth: Double = 150.0
    var buyModeWidth: Double = 120.0
    var taxFieldWidth: Double = 50.0

    // Timing
    var uiUpdateDebounceMs: Long = 100
    var statusDisplayMs: Long = 5000
    var progressPollingMs: Long = 500

    var cardSeparatorWidth: Double = 260.0

    init {
        val sizeRange = range(0.0, 1000.0)
        val spacingRange = range(0.0, 100.0)
        val opacityRange = range(0.0, 1.0)

        register(::cardMinWidth, "card_min_width", validate = range(100.0, 1000.0))
        register(::cardPadding, "card_padding", validate = spacingRange)
        register(::cardSpacing, "card_spacing", validate = spacingRange)
        register(::craftCardBg, "craft_card_bg")
        register(::craftCardBorderExcluded, "craft_card_border_excluded")
        register(::upgradeCardBg, "upgrade_card_bg")
        register(::instantCraftBg, "instant_craft_bg")
        register(::cardSeparator, "card_separator")
        register(::labelColorPrimary, "label_color_primary")
        register(::labelColorSecondary, "label_color_secondary")
        register(::labelColorMuted, "label_color_muted")
        register(::fontSizeExtraSmall, "font_size_extra_small", validate = range(8.0, 16.0))
        register(::fontSizeSmall, "font_size_small", validate = range(10.0, 20.0))
        register(::fontSizeNormal, "font_size_normal", validate = range(12.0, 24.0))
        register(::fontSizeLarge, "font_size_large", validate = range(14.0, 32.0))
        register(::fontSizeTitle, "font_size_title", validate = range(16.0, 48.0))
        register(::spacingTiny, "spacing_tiny", validate = spacingRange)
        register(::spacingSmall, "spacing_small", validate = spacingRange)
        register(::borderRadiusSmall, "border_radius_small", validate = spacingRange)
        register(::paddingSmall, "padding_small", validate = spacingRange)
        register(::paddingMedium, "padding_medium", validate = spacingRange)
        register(::styleLabelSecondaryBold, "style_label_secondary_bold")
        register(::styleLabelSmallYellow, "style_label_small_yellow")
        register(::styleLabelSmall, "style_label_small")
        register(::styleLabelNormal, "style_label_normal")
        register(::styleLabelExtraSmallBold, "style_label_extra_small_bold")
        register(::styleBadgeSource, "style_badge_source")
        register(::familyBoxPadding, "family_box_padding", validate = spacingRange)
        register(::familyBoxSpacing, "family_box_spacing", validate = spacingRange)
        register(::familyBoxRadius, "family_box_radius", validate = spacingRange)
        register(::familyBoxOpacityExcluded, "family_box_opacity_excluded", validate = opacityRange)
        register(::familyBoxBgExcluded, "family_box_bg_excluded")
        register(::familyBoxBgNormal, "family_box_bg_normal")
        register(::familyBoxBorderExcluded, "family_box_border_excluded")
        register(::familyBoxBorderPartial, "family_box_border_partial")
        register(::mainPadding, "main_padding", validate = spacingRange)
        register(::mainSpacing, "main_spacing", validate = spacingRange)
        register(::controlSpacing, "control_spacing", validate = spacingRange)
        register(::controlGroupSpacing, "control_group_spacing", validate = spacingRange)
        register(::labelSpacing, "label_spacing", validate = spacingRange)
        register(::sidebarWidth, "sidebar_width", validate = range(100.0, 600.0))
        register(::searchFieldWidth, "search_field_width", validate = sizeRange)
        register(::sortByWidth, "sort_by_width", validate = sizeRange)
        register(::rarityFilterWidth, "rarity_filter_width", validate = sizeRange)
        register(::buyModeWidth, "buy_mode_width", validate = sizeRange)
        register(::taxFieldWidth, "tax_field_width", validate = sizeRange)
        register(::uiUpdateDebounceMs, "ui_update_debounce_ms", validate = range(0L, 5000L))
        register(::statusDisplayMs, "status_display_ms", validate = range(0L, 30000L))
        register(::progressPollingMs, "progress_polling_ms", validate = range(100L, 5000L))
        register(::cardSeparatorWidth, "card_separator_width", validate = sizeRange)
        register(::formatDuration, "format_duration")
        register(::formatCoins, "format_coins")
        register(::formatPrice, "format_price")
        register(::formatPercent, "format_percent")

        loadConfig()
    }

    override fun resetToDefaults() {
        cardMinWidth = 280.0
        cardPadding = 12.0
        cardSpacing = 10.0
        craftCardBg = "#2b2b2b"
        craftCardBorderExcluded = "#444444"
        upgradeCardBg = "#2b2b2b"
        instantCraftBg = "#1a2634"
        cardSeparator = "#444444"
        labelColorPrimary = "#ffffff"
        labelColorSecondary = "#cccccc"
        labelColorMuted = "#888888"
        fontSizeExtraSmall = 11.0
        fontSizeSmall = 13.0
        fontSizeNormal = 14.0
        fontSizeMedium = 14.0
        fontSizeLarge = 16.0
        fontSizeExtraLarge = 20.0
        fontSizeTitle = 18.0
        spacingTiny = 5.0
        spacingSmall = 10.0
        paddingTiny = 2.0
        paddingSmall = 5.0
        paddingMedium = 10.0
        borderRadiusSmall = 4.0
        borderRadiusMedium = 6.0
        borderRadiusLarge = 8.0
        borderWidthThin = 1.0
        borderWidthThick = 2.0
        
        uiUpdateDebounceMs = 100
        statusDisplayMs = 5000
        progressPollingMs = 500
        
        saveConfig()
        ConfigEvents.fire()
    }
}
