package utils

object Styles {
    val DARK_BG get() = GeneralConfig.colorDarkBg
    val DARKER_BG get() = GeneralConfig.colorDarkerBg
    val TEXT_COLOR get() = GeneralConfig.colorTextPrimary
    val ACCENT get() = GeneralConfig.colorAccentBlue

    private fun style(vararg pairs: Pair<String, String>) =
        pairs.joinToString("; ", postfix = ";") { "-fx-${it.first}: ${it.second}" }

    val field get() = style(
        "background-color" to GeneralConfig.colorFieldBg,
        "text-fill" to "white",
        "prompt-text-fill" to "#888888",
        "font-size" to GeneralConfig.fontSizeSmall
    )

    val label get() = style("text-fill" to TEXT_COLOR, "font-size" to GeneralConfig.fontSizeSmall)
    val combo get() = style(
        "background-color" to DARKER_BG,
        "text-fill" to "white",
        "font-size" to GeneralConfig.fontSizeSmall,
        "background-radius" to "4",
        "border-color" to "#555555",
        "border-radius" to "4",
        "border-width" to "1"
    ) + "; -fx-background-insets: 0"
    val button get() = style(
        "background-color" to GeneralConfig.colorButtonBg,
        "text-fill" to "white",
        "cursor" to "hand",
        "font-size" to GeneralConfig.fontSizeSmall
    )

    val listView get() = style(
        "background-color" to DARKER_BG,
        "control-inner-background" to DARKER_BG,
        "font-size" to GeneralConfig.fontSizeSmall
    )
}
