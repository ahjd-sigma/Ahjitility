package utils

object ShardUIConfig : BaseConfig("shard_ui.yaml") {
    // Layout
    var dividerPosition: Double = 0.65
    var searchFieldWidth: Double = 150.0
    var taxFieldWidth: Double = 50.0
    var sidebarPadding: Double = 15.0
    var contentSpacing: Double = 8.0
    var ingredientSpacing: Double = 2.0
    var iconSize: Double = 32.0

    init {
        val sizeRange = range(0.0, 1000.0)
        val spacingRange = range(0.0, 100.0)

        register(::dividerPosition, "divider_position", validate = range(0.1, 0.9))
        register(::searchFieldWidth, "search_field_width", validate = sizeRange)
        register(::taxFieldWidth, "tax_field_width", validate = sizeRange)
        register(::sidebarPadding, "sidebar_padding", validate = spacingRange)
        register(::contentSpacing, "content_spacing", validate = spacingRange)
        register(::ingredientSpacing, "ingredient_spacing", validate = spacingRange)
        register(::iconSize, "icon_size", validate = range(16.0, 128.0))
        loadConfig()
    }

    override fun resetToDefaults() {
        dividerPosition = 0.65
        searchFieldWidth = 150.0
        taxFieldWidth = 50.0
        sidebarPadding = 15.0
        contentSpacing = 8.0
        ingredientSpacing = 2.0
        iconSize = 32.0
        
        saveConfig()
        ConfigEvents.fire()
    }
}
