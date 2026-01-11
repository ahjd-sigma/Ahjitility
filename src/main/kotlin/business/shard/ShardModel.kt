package business.shard

data class ShardInfo(
    val shardId: String,
    val displayName: String,
    val itemId: String,
    val rarity: String,
    val ratePerHour: Double,
    val price: Double,
    val sellMode: String,
    val hunterFortune: Int = 0
) {
    val effectiveRatePerHour: Double = run {
        if (hunterFortune == 0) return@run ratePerHour
        val multiplier = when (rarity.uppercase()) {
            "EPIC", "RARE", "LEGENDARY", "MYTHIC" -> 1.015376 + (hunterFortune * 0.0124)
            else -> 1.030752 + (hunterFortune * 0.0124)
        }
        ratePerHour * multiplier
    }
    val profitPerHour = effectiveRatePerHour * price
}

data class ShardProperties(
    val name: String,
    val rarity: String,
    val category: String?,
    val family: List<String>?
)