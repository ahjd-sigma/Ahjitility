package utils

/**
 * Global configuration settings for the application.
 * Manages UI colors, font sizes, and scrolling behavior.
 */
object GeneralConfig : BaseConfig("general.yaml") {
    val VERSION = GeneralConfig::class.java.getResourceAsStream("/version.txt")?.bufferedReader()?.use { it.readText().trim() } ?: "unknown"
    private const val SCROLL_UNIT = 1_000_000.0
    
    // Scroll settings
    var scrollWheelMultiplier: Double = 0.002
    var katScrollMultiplier: Double = 0.002
    var forgeScrollMultiplier: Double = 0.002
    var shardScrollMultiplier: Double = 0.002
    var autoscrollSpeed: Double = 0.000025

    // Global Colors
    var colorDarkBg: String = "#2b2b2b"
    var colorDarkerBg: String = "#1e1e1e"
    var colorTextPrimary: String = "#ffffff"
    var colorAccentBlue: String = "#4a90e2"
    var colorAccentOrange: String = "#FF9800"
    var colorAccentRed: String = "#e74c3c"
    var colorAccentGreen: String = "#2ecc71"
    var colorAccentCyan: String = "#00BCD4"
    var colorAccentMagenta: String = "#E91E63"
    var colorFieldBg: String = "#333333"
    var colorButtonBg: String = "#3c3f41"
    var colorBorder: String = "#333333"
    var colorSeparator: String = "#444444"

    // Fonts
    var fontSizeSmall: String = "13px"
    var fontSizeMedium: String = "14px"
    var fontSizeLarge: String = "16px"
    var fontSizeTitle: String = "18px"

    init {
        val load = { v: Any? -> (v as? Number)?.toDouble()?.div(SCROLL_UNIT) ?: 0.002 }
        val save = { v: Double -> v * SCROLL_UNIT }
        val scrollRange = range(0.0001, 0.1)

        register(::scrollWheelMultiplier, "scroll_wheel_multiplier", load, save, scrollRange)
        register(::katScrollMultiplier, "kat_scroll_multiplier", load, save, scrollRange)
        register(::forgeScrollMultiplier, "forge_scroll_multiplier", load, save, scrollRange)
        register(::shardScrollMultiplier, "shard_scroll_multiplier", load, save, scrollRange)
        register(::autoscrollSpeed, "autoscroll_speed", { (it as? Number)?.toDouble()?.div(SCROLL_UNIT) ?: 0.000025 }, save, range(0.000001, 0.001))

        register(::colorDarkBg, "color_dark_bg")
        register(::colorDarkerBg, "color_darker_bg")
        register(::colorTextPrimary, "color_text_primary")
        register(::colorAccentBlue, "color_accent_blue")
        register(::colorAccentOrange, "color_accent_orange")
        register(::colorAccentRed, "color_accent_red")
        register(::colorAccentGreen, "color_accent_green")
        register(::colorAccentCyan, "color_accent_cyan")
        register(::colorAccentMagenta, "color_accent_magenta")
        register(::colorFieldBg, "color_field_bg")
        register(::colorButtonBg, "color_button_bg")
        register(::colorBorder, "color_border")
        register(::colorSeparator, "color_separator")

        register(::fontSizeSmall, "font_size_small", validate = fontSize(10, 20))
        register(::fontSizeMedium, "font_size_medium", validate = fontSize(12, 24))
        register(::fontSizeLarge, "font_size_large", validate = fontSize(14, 32))
        register(::fontSizeTitle, "font_size_title", validate = fontSize(16, 48))

        loadConfig()
    }

    override fun resetToDefaults() {
        scrollWheelMultiplier = 0.002
        katScrollMultiplier = 0.002
        forgeScrollMultiplier = 0.002
        shardScrollMultiplier = 0.002
        autoscrollSpeed = 0.000025

        colorDarkBg = "#2b2b2b"
        colorDarkerBg = "#1e1e1e"
        colorTextPrimary = "#ffffff"
        colorAccentBlue = "#4a90e2"
        colorAccentOrange = "#FF9800"
        colorAccentRed = "#e74c3c"
        colorAccentGreen = "#2ecc71"
        colorAccentCyan = "#00BCD4"
        colorAccentMagenta = "#E91E63"
        colorFieldBg = "#333333"
        colorButtonBg = "#3c3f41"
        colorBorder = "#333333"
        colorSeparator = "#444444"

        fontSizeSmall = "13px"
        fontSizeMedium = "14px"
        fontSizeLarge = "16px"
        fontSizeTitle = "18px"

        saveConfig()
        ConfigEvents.fire()
    }
}
