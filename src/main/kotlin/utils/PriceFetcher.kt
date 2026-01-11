package utils

import okhttp3.*
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

import business.forge.SourcePriority
import utils.KatConfig

data class PriceResult(val price: Double, val isBazaar: Boolean)

class PriceFetcher {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val cache = ConcurrentHashMap<String, CachedPrice>()
    private val cacheTimeout get() = Duration.ofMinutes(KatConfig.priceTimeoutMinutes.toLong())
    private val salesCache = ConcurrentHashMap<String, Pair<Double, Instant>>()
    private val salesCacheTimeout get() = Duration.ofHours(KatConfig.salesTimeoutHours.toLong())
    private val baseFetchCache = ConcurrentHashMap<String, Instant>()

    private data class CachedPrice(
        val bazaarPrices: Map<String, ProductInfo>?,
        val lbinPrices: Map<String, Double>?,
        val timestamp: Instant
    )

    fun fetchAllPrices(force: Boolean = false) {
        if (force) cache.clear()
        getOrFetchPrices()
    }

    private fun getOrFetchPrices(): CachedPrice {
        val cached = cache["prices"]
        if (cached != null && Duration.between(cached.timestamp, Instant.now()) < cacheTimeout) {
            return cached
        }

        val bazaar = fetchBazaarPrices()
        val lbin = fetchLbinPrices()
        val newCache = CachedPrice(bazaar, lbin, Instant.now())
        cache["prices"] = newCache
        return newCache
    }

    fun getBuyPrice(itemId: String?, isInstant: Boolean, priority: SourcePriority = SourcePriority.ALL): PriceResult {
        if (itemId == null) return PriceResult(0.0, false)
        val prices = getOrFetchPrices()

        val bz = prices.bazaarPrices?.get(itemId)
        var lbin = prices.lbinPrices?.get(itemId)

        // Handle PET_ prefix for pets in LBIN (try both with and without prefix)
        if (lbin == null && itemId.contains(";")) {
            val altId = if (itemId.startsWith("PET_")) itemId.substring(4) else "PET_$itemId"
            lbin = prices.lbinPrices?.get(altId)
        }

        return when (priority) {
            SourcePriority.BAZAAR_FIRST -> {
                if (bz != null) {
                    val price = if (isInstant) bz.quickStatus.buyPrice else bz.quickStatus.sellPrice
                    PriceResult(price, true)
                } else {
                    PriceResult(lbin ?: 0.0, false)
                }
            }
            SourcePriority.AH_FIRST -> {
                if (lbin != null) {
                    PriceResult(lbin, false)
                } else if (bz != null) {
                    val price = if (isInstant) bz.quickStatus.buyPrice else bz.quickStatus.sellPrice
                    PriceResult(price, true)
                } else {
                    PriceResult(0.0, false)
                }
            }
            SourcePriority.ALL -> {
                // Default: check Bazaar first as it's more common for stackables
                if (bz != null) {
                    val price = if (isInstant) bz.quickStatus.buyPrice else bz.quickStatus.sellPrice
                    PriceResult(price, true)
                } else {
                    PriceResult(lbin ?: 0.0, false)
                }
            }
        }
    }

    fun getSellPrice(itemId: String?, isInstant: Boolean, priority: SourcePriority = SourcePriority.ALL): PriceResult {
        if (itemId == null) return PriceResult(0.0, false)
        val prices = getOrFetchPrices()

        val bz = prices.bazaarPrices?.get(itemId)
        var lbin = prices.lbinPrices?.get(itemId)

        // Handle PET_ prefix for pets in LBIN (try both with and without prefix)
        if (lbin == null && itemId.contains(";")) {
            val altId = if (itemId.startsWith("PET_")) itemId.substring(4) else "PET_$itemId"
            lbin = prices.lbinPrices?.get(altId)
        }

        return when (priority) {
            SourcePriority.BAZAAR_FIRST -> {
                if (bz != null) {
                    val price = if (isInstant) bz.quickStatus.sellPrice else bz.quickStatus.buyPrice
                    PriceResult(price, true)
                } else {
                    PriceResult(lbin ?: 0.0, false)
                }
            }
            SourcePriority.AH_FIRST -> {
                if (lbin != null) {
                    PriceResult(lbin, false)
                } else if (bz != null) {
                    val price = if (isInstant) bz.quickStatus.sellPrice else bz.quickStatus.buyPrice
                    PriceResult(price, true)
                } else {
                    PriceResult(0.0, false)
                }
            }
            SourcePriority.ALL -> {
                if (bz != null) {
                    val price = if (isInstant) bz.quickStatus.sellPrice else bz.quickStatus.buyPrice
                    PriceResult(price, true)
                } else {
                    PriceResult(lbin ?: 0.0, false)
                }
            }
        }
    }

    fun hasBazaarPrice(itemId: String?): Boolean {
        if (itemId == null) return false
        val prices = getOrFetchPrices()
        return prices.bazaarPrices?.containsKey(itemId) == true
    }

    // Rate limiting for Coflnet
    private var requestCount = 0
    private var windowStart = System.currentTimeMillis()
    private val lock = Any()

    private fun checkRateLimit() {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val windowMs = KatConfig.coflnetWindowSeconds * 1000L
            if (now - windowStart > windowMs) {
                requestCount = 0
                windowStart = now
            }
            if (requestCount >= KatConfig.coflnetRequestsPerWindow) {
                val waitTime = windowMs - (now - windowStart)
                if (waitTime > 0) Thread.sleep(waitTime)
                requestCount = 0
                windowStart = System.currentTimeMillis()
            }
            requestCount++
        }
    }

    fun fetchHourlySales(itemId: String): Double? {
        val now = Instant.now()
        val cached = salesCache[itemId]
        if (cached != null && Duration.between(cached.second, now) < salesCacheTimeout) {
            return cached.first
        }

        val isPet = itemId.contains(";")
        // Normalize base ID (remove PET_ prefix for cache consistency)
        var originalBaseId = if (isPet) itemId.split(";")[0] else itemId
        if (originalBaseId.startsWith("PET_")) {
            originalBaseId = originalBaseId.substring(4)
        }
        
        val rarityNum = if (isPet) itemId.split(";").getOrNull(1) else null
        val normalizedItemId = if (isPet) "$originalBaseId;$rarityNum" else originalBaseId
        
        // Check cache again with normalized ID
        if (normalizedItemId != itemId) {
            val normalizedCached = salesCache[normalizedItemId]
            if (normalizedCached != null && Duration.between(normalizedCached.second, now) < salesCacheTimeout) {
                return normalizedCached.first
            }
        }

        var fetchId = originalBaseId.replace(" ", "_")
        if (isPet) {
            val lastBaseFetch = baseFetchCache[originalBaseId]
            if (lastBaseFetch != null && Duration.between(lastBaseFetch, now) < salesCacheTimeout) {
                return salesCache[normalizedItemId]?.first ?: 0.0
            }
            fetchId = "PET_$fetchId"
        }

        checkRateLimit()

        val pageSizes = listOf(500, 250, 100, 50)
        var auctions: List<CoflAuction>? = null
        
        fun tryFetch(id: String): List<CoflAuction>? {
            for (size in pageSizes) {
                val url = KatConfig.coflnetSoldAuctionsUrl
                    .replace("{itemId}", id)
                    .replace("{pageSize}", size.toString())
                
                val result = fetchJson<List<CoflAuction>>(url, object : TypeToken<List<CoflAuction>>() {})
                if (result != null && result.isNotEmpty()) {
                    println("[DEBUG] PriceFetcher: Success with id $id and pageSize $size (found ${result.size} auctions)")
                    return result
                }
            }
            println("[DEBUG] PriceFetcher: All page sizes failed or returned empty for id $id")
            return null
        }

        return try {
            auctions = tryFetch(fetchId)
            
            // Try without PET_ prefix if it failed and was a pet
            if ((auctions == null || auctions.isEmpty()) && isPet && fetchId.startsWith("PET_")) {
                val altId = fetchId.substring(4)
                auctions = tryFetch(altId)
            }

            if (auctions == null || auctions.isEmpty()) {
                if (isPet) {
                    baseFetchCache[originalBaseId] = now
                    for (i in 0..5) salesCache["$originalBaseId;$i"] = Pair(0.0, now)
                } else {
                    salesCache[normalizedItemId] = Pair(0.0, now)
                }
                return 0.0
            }

            if (isPet) {
                baseFetchCache[originalBaseId] = now
                val rarityMap = KatConfig.rarityNumbers
                
                // Initialize all rarities with 0 for this pet
                for (i in 0..5) salesCache["$originalBaseId;$i"] = Pair(0.0, now)

                // Group and calculate
                val grouped = auctions.groupBy { it.tier?.uppercase() ?: "UNKNOWN" }
                println("[DEBUG] PriceFetcher: Grouped ${auctions.size} auctions into tiers: ${grouped.keys}")
                grouped.forEach { (tier, tierAuctions) ->
                    val rNum = rarityMap[tier]
                    if (rNum != null) {
                        val earliest = parseCoflDate(tierAuctions.last().end)
                        val durationHours = max(1.0, Duration.between(earliest, now).toMillis() / (1000.0 * 60.0 * 60.0))
                        val hourlySales = tierAuctions.size.toDouble() / durationHours
                        salesCache["$originalBaseId;$rNum"] = Pair(hourlySales, now)
                        println("[DEBUG] PriceFetcher: Populated cache for $originalBaseId;$rNum ($tier) with $hourlySales sales/hr")
                    } else {
                        println("[DEBUG] PriceFetcher: Unknown tier '$tier' for pet $originalBaseId")
                    }
                }
                salesCache[normalizedItemId]?.first ?: 0.0
            } else {
                val earliest = parseCoflDate(auctions.last().end)
                val durationHours = max(1.0, Duration.between(earliest, now).toMillis() / (1000.0 * 60.0 * 60.0))
                val totalSales = auctions.size.toDouble()
                val hourlySales = totalSales / durationHours
                
                salesCache[normalizedItemId] = Pair(hourlySales, now)
                hourlySales
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCoflDate(dateStr: String): Instant {
        return if (dateStr.endsWith("Z")) {
            Instant.parse(dateStr)
        } else {
            Instant.parse("${dateStr}Z")
        }
    }

    private fun <T> fetchJson(url: String, typeToken: TypeToken<T>): T? = try {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.string()?.let { gson.fromJson(it, typeToken.type) }
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun fetchBazaarPrices(): Map<String, ProductInfo>? {
        val response = fetchJson(
            KatConfig.hypixelBazaarUrl,
            object : TypeToken<BazaarResponse>() {}
        )
        return if (response?.success == true) response.products else null
    }

    private fun fetchLbinPrices(): Map<String, Double>? = fetchJson(
        KatConfig.moulberryLbinUrl,
        object : TypeToken<Map<String, Double>>() {}
    )
}

data class BazaarResponse(
    val success: Boolean,
    val lastUpdated: Long,
    val products: Map<String, ProductInfo>
)

data class ProductInfo(
    @SerializedName("product_id") val productId: String,
    @SerializedName("quick_status") val quickStatus: QuickStatus
)

data class CoflAuction(
    val uuid: String,
    val end: String, // ISO date string
    val bin: Boolean,
    val tier: String?
)

data class QuickStatus(
    val productId: String,
    val sellPrice: Double,
    val buyPrice: Double
)
