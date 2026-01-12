package utils

/**
 * Configuration for the Kat module, including API endpoints, rate limits, 
 * and calculation constants.
 */
object KatConfig : BaseConfig("kat_config.yaml") {
    // API
    var coflnetKatDataUrl: String = "https://sky.coflnet.com/api/kat/data"
    var coflnetSoldAuctionsUrl: String = "https://sky.coflnet.com/api/auctions/tag/{itemId}/sold?page=1&pageSize=100"
    var hypixelBazaarUrl: String = "https://api.hypixel.net/skyblock/bazaar"
    var moulberryLbinUrl: String = "https://moulberry.codes/lowestbin.json"

    // Rate Limits
    var coflnetRequestsPerWindow: Int = 30
    var coflnetWindowSeconds: Int = 10
    var priceTimeoutMinutes: Int = 15
    var salesTimeoutHours: Int = 24

    // Files
    var katJsonPath: String = "src/main/resources/kat/kat.json"
    var katBlacklistPath: String = "src/main/resources/kat_blacklist.json"
    var petPrefix: String = "PET_"
    var katFlowerId: String = "KAT_FLOWER"
    var katBouquetId: String = "KAT_BOUQUET"

    // Mappings and Separators
    var itemTagSeparator: String = ";"
    var space: String = " "
    var underscore: String = "_"
    var unknownRarity: String = "UNKNOWN"
    var itemNameMappings: MutableMap<String, String> = mutableMapOf(
        "ENCHANTED_RED_SAND_CUBE" to "Enchanted Red Sand",
        "ENCHANTED_COMPOST" to "Compost Bundle",
        "ENCHANTED_HUGE_MUSHROOM_1" to "Enchanted Brown Mushroom",
        "ENCHANTED_HUGE_MUSHROOM_2" to "Enchanted Red Mushroom",
        "END_STONE" to "Endstone",
        "RAW_PORKCHOP" to "Pork",
        "ENCHANTED_RAW_PORKCHOP" to "Enchanted Pork",
        "JUNGLE_WOOD" to "Jungle Log",
        "LOG-3" to "Jungle Log",
        "LOG-1" to "Spruce Log",
        "RAW_RABBIT" to "Rabbit",
        "RAW_MUTTON" to "Mutton",
        "MUTTON" to "Enchanted Mutton",
        "ASSISTANT" to "Move Jerry"
    )
    var itemIdMappings: MutableMap<String, String> = mutableMapOf(
        "ENCHANTED_RED_SAND_CUBE" to "ENCHANTED_RED_SAND",
        "ENCHANTED_HUGE_MUSHROOM_1" to "ENCHANTED_BROWN_MUSHROOM",
        "ENCHANTED_HUGE_MUSHROOM_2" to "ENCHANTED_RED_MUSHROOM",
        "END_STONE" to "ENDSTONE",
        "RAW_PORKCHOP" to "PORK",
        "ENCHANTED_RAW_PORKCHOP" to "ENCHANTED_PORK",
        "JUNGLE_WOOD" to "JUNGLE_LOG",
        "LOG-3" to "JUNGLE_LOG",
        "LOG-1" to "SPRUCE_LOG",
        "RAW_RABBIT" to "RABBIT",
        "RAW_MUTTON" to "MUTTON",
        "MUTTON" to "ENCHANTED_MUTTON",
        "ASSISTANT" to "MOVE_JERRY"
    )

    // NPC and Custom Pricing
    var agathaCouponId: String = "AGATHA_COUPON"
    var npcItemCosts: Map<String, Map<String, Int>> = mapOf(
        "SMALL_FROG_TREAT" to mapOf("AGATHA_COUPON" to 30),
        "MEDIUM_FROG_TREAT" to mapOf("AGATHA_COUPON" to 40),
        "LARGE_FROG_TREAT" to mapOf("AGATHA_COUPON" to 50),
        "GIANT_FROG_TREAT" to mapOf("AGATHA_COUPON" to 60)
    )

    // Time Reduction
    const val FLOWER_SKIP_HOURS: Double = 24.0
    const val BOUQUET_SKIP_HOURS: Double = 120.0
    var targetMaxDurationHours: Double = 0.05
    var enableTimeReduction: Boolean = true
    var maxFlowers: Int = 30
    var maxBouquets: Int = 10
    var defaultFlowerPrice: Double = 100000.0
    var defaultBouquetPrice: Double = 1000000.0
    var defaultAgathaCouponPrice: Double = 15000.0
    var forceFlowerPrice: Boolean = false
    var forceBouquetPrice: Boolean = false
    var forceAgathaCouponPrice: Boolean = false

    // Market
    var bazaarTax: Double = 1.25
    var ahMultiplier: Double = 1.0
    var defaultBazaarInstant: Boolean = false
    var ahTaxThresholds: List<Double> = listOf(1_000_000.0, 10_000_000.0, 100_000_000.0)
    var ahTaxRates: List<Double> = listOf(1.0, 2.0, 3.0, 3.5)

    // Rarity
    var rarities: List<String> = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC")
    var craftRarity: String = "CRAFT"
    var nextRarity: Map<String, String> = mapOf(
        "COMMON" to "UNCOMMON",
        "UNCOMMON" to "RARE",
        "RARE" to "EPIC",
        "EPIC" to "LEGENDARY",
        "LEGENDARY" to "MYTHIC"
    )
    var rarityNumbers: Map<String, Int> = mapOf(
        "COMMON" to 0, "UNCOMMON" to 1, "RARE" to 2,
        "EPIC" to 3, "LEGENDARY" to 4, "MYTHIC" to 5
    )

    init {
        register(::coflnetKatDataUrl, "api.coflnet_kat_data")
        register(::coflnetSoldAuctionsUrl, "api.coflnet_sold_auctions")
        register(::hypixelBazaarUrl, "api.hypixel_bazaar")
        register(::moulberryLbinUrl, "api.moulberry_lbin")

        register(::coflnetRequestsPerWindow, "rate_limits.coflnet_requests_per_window")
        register(::coflnetWindowSeconds, "rate_limits.coflnet_window_seconds")
        register(::priceTimeoutMinutes, "rate_limits.price_timeout_minutes")
        register(::salesTimeoutHours, "rate_limits.sales_timeout_hours")

        register(::katJsonPath, "files.kat_json_path")
        register(::katBlacklistPath, "files.kat_blacklist_path")

        register(::targetMaxDurationHours, "time_reduction.target_max_duration_hours")
        register(::enableTimeReduction, "time_reduction.enable_time_reduction")
        register(::maxFlowers, "time_reduction.max_flowers")
        register(::maxBouquets, "time_reduction.max_bouquets")
        register(::defaultFlowerPrice, "time_reduction.default_flower_price")
        register(::defaultBouquetPrice, "time_reduction.default_bouquet_price")
        register(::defaultAgathaCouponPrice, "time_reduction.default_agatha_coupon_price")
        register(::forceFlowerPrice, "time_reduction.force_flower_price")
        register(::forceBouquetPrice, "time_reduction.force_bouquet_price")
        register(::forceAgathaCouponPrice, "time_reduction.force_agatha_coupon_price")

        register(::bazaarTax, "market.bazaar_tax")
        register(::ahMultiplier, "market.ah_multiplier")
        register(::defaultBazaarInstant, "market.default_bazaar_instant")
        register(::ahTaxThresholds, "market.ah_tax_thresholds")
        register(::ahTaxRates, "market.ah_tax_rates")

        register(::rarities, "rarity.list")
        register(::nextRarity, "rarity.next")
        register(::rarityNumbers, "rarity.numbers")

        register(::agathaCouponId, "npc.agatha_coupon_id")
        register(::npcItemCosts, "npc.item_costs")

        loadConfig()
    }

    override fun resetToDefaults() {
        coflnetKatDataUrl = "https://sky.coflnet.com/api/kat/data"
        coflnetSoldAuctionsUrl = "https://sky.coflnet.com/api/auctions/tag/{itemId}/sold?page=1&pageSize=100"
        hypixelBazaarUrl = "https://api.hypixel.net/v2/skyblock/bazaar"
        moulberryLbinUrl = "https://moulberry.codes/lowestbin.json"

        coflnetRequestsPerWindow = 30
        coflnetWindowSeconds = 10
        priceTimeoutMinutes = 15
        salesTimeoutHours = 24

        katJsonPath = "src/main/resources/kat/kat.json"
        katBlacklistPath = "src/main/resources/kat_blacklist.json"

        targetMaxDurationHours = 0.05
        enableTimeReduction = true
        maxFlowers = 30
        maxBouquets = 10
        defaultFlowerPrice = 100000.0
        defaultBouquetPrice = 1000000.0
        defaultAgathaCouponPrice = 15000.0
        forceFlowerPrice = false
        forceBouquetPrice = false
        forceAgathaCouponPrice = false

        bazaarTax = 1.25
        ahMultiplier = 1.0
        defaultBazaarInstant = false
        ahTaxThresholds = listOf(1_000_000.0, 10_000_000.0, 100_000_000.0)
        ahTaxRates = listOf(1.0, 2.0, 3.0, 3.5)

        agathaCouponId = "AGATHA_COUPON"
        npcItemCosts = mapOf(
            "SMALL_FROG_TREAT" to mapOf("AGATHA_COUPON" to 30),
            "MEDIUM_FROG_TREAT" to mapOf("AGATHA_COUPON" to 40),
            "LARGE_FROG_TREAT" to mapOf("AGATHA_COUPON" to 50),
            "GIANT_FROG_TREAT" to mapOf("AGATHA_COUPON" to 60)
        )

        saveConfig()
        ConfigEvents.fire()
    }
}
