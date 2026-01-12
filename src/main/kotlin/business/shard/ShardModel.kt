package business.shard

data class ShardInfo(
    val shardId: String,
    val displayName: String,
    val itemId: String,
    val rarity: String,
    val ratePerHour: Double,
    val price: Double,
    val sellMode: String,
    val hunterFortune: Int = 0,
    val chestPrice: Double = 0.0,
    val isFishingShard: Boolean = false,
    val baitCount: Double = 0.0, // Bait used per 5 mins
    val baitPrice: Double = 0.0
) {
    val effectiveRatePerHour: Double = run {
        if (hunterFortune == 0 || isDungeonShard()) return@run ratePerHour
        
        if (isFishingShard) {
            // Fishing shards: 100 fortune = +100% chance (2x drops)
            val multiplier = 1.0 + (hunterFortune / 100.0)
            return@run ratePerHour * multiplier
        }

        val multiplier = when (rarity.uppercase()) {
            "EPIC", "RARE", "LEGENDARY", "MYTHIC" -> 1.117 + (hunterFortune * 0.01364)
            else -> 1.134 + (hunterFortune * 0.01364)
        }
        ratePerHour * multiplier
    }
    
    val profitPerHour = run {
        val baseProfit = effectiveRatePerHour * (price - chestPrice)
        if (isFishingShard) {
            val baitPerHour = baitCount * 12.0 // 1 bait per 5 mins = 12 bait per hour
            baseProfit - (baitPerHour * baitPrice)
        } else {
            baseProfit
        }
    }

    fun isDungeonShard(): Boolean = displayName in listOf(
        "Scarf", "Thorn", "Wither",
        "Ananke", "Apex Dragon", "Power Dragon", "Kraken", "Daemon"
    )
}

data class ShardProperties(
    val name: String,
    val rarity: String,
    val category: String?,
    val family: List<String>?,
    val isFishingShard: Boolean = false
)