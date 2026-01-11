package utils

/**
 * Configuration for the Shard module.
 */
object ShardConfig : BaseConfig("shard.yaml") {
    // Files
    var recipesPath: String = "/shard/ShardRecipes.json"

    // Defaults
    var defaultBazaarTax: Double = 1.25
    var defaultAhMultiplier: Double = 1.0

    init {
        register(::recipesPath, "recipes_path")
        register(::defaultBazaarTax, "default_bazaar_tax")
        register(::defaultAhMultiplier, "default_ah_multiplier")

        loadConfig()
    }

    override fun resetToDefaults() {
        defaultBazaarTax = 1.25
        defaultAhMultiplier = 1.0
        
        saveConfig()
        ConfigEvents.fire()
    }
}
