package utils

/**
 * Configuration for the Forge module.
 */
object ForgeModuleConfig : BaseConfig("forge.yaml") {
    // Files
    var recipesPath: String = "/forge/ForgeRecipes.json"

    // Defaults
    var defaultBazaarTax: Double = 1.25
    var defaultAhMultiplier: Double = 1.0
    var defaultQuickForgeLevel: Int = 0

    init {
        register(::recipesPath, "recipes_path")
        register(::defaultBazaarTax, "default_bazaar_tax")
        register(::defaultAhMultiplier, "default_ah_multiplier")
        register(::defaultQuickForgeLevel, "default_quick_forge_level")

        loadConfig()
    }

    override fun resetToDefaults() {
        defaultBazaarTax = 1.25
        defaultAhMultiplier = 1.0
        defaultQuickForgeLevel = 0
        
        saveConfig()
        ConfigEvents.fire()
    }
}
