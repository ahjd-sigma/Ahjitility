package business.shard

object ShardCalculations {
    fun combineData(
        rates: Map<String, Double>,
        properties: Map<String, ShardProperties>,
        prices: Map<String, Double>,
        sellMode: String,
        hunterFortune: Int = 0,
        chestPrices: Map<String, Double> = emptyMap(),
        baitCounts: Map<String, Double> = emptyMap(),
        baitPrice: Double = 0.0
    ): List<ShardInfo> = rates.map { (id, rate) ->
        val prop = properties[id]
        val displayName = prop?.name ?: id
        ShardInfo(
            shardId = id,
            displayName = displayName,
            itemId = resolveItemId(displayName),
            rarity = prop?.rarity ?: "Common",
            ratePerHour = rate,
            price = prices[id] ?: 0.0,
            sellMode = sellMode,
            hunterFortune = hunterFortune,
            chestPrice = chestPrices[displayName] ?: 0.0,
            isFishingShard = prop?.isFishingShard ?: false,
            baitCount = baitCounts[displayName] ?: 0.0,
            baitPrice = baitPrice
        )
    }

    fun getPriceIds(
        rates: Map<String, Double>,
        properties: Map<String, ShardProperties>
    ): List<Pair<String, String>> = rates.map { (id, _) ->
        val name = properties[id]?.name ?: id
        id to resolveItemId(name)
    }

    private fun resolveItemId(displayName: String) = when (displayName) {
        "Stridersurfer" -> "SHARD_STRIDER_SURFER"
        "Abyssal Lanternfish" -> "SHARD_ABYSSAL_LANTERN"
        "Cinderbat" -> "SHARD_CINDER_BAT"
        "Bogged" -> "SHARD_SEA_ARCHER"
        "Loch Emperor" -> "SHARD_SEA_EMPEROR"
        else -> "SHARD_${displayName.uppercase().replace(" ", "_")}"
    }
}
