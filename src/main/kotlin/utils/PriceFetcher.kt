package utils

import business.forge.SourcePriority
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

data class PriceResult(val price: Double, val isBazaar: Boolean)

class PriceFetcher {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val cache = ConcurrentHashMap<String, CachedPrice>()
    private val cacheTimeout get() = Duration.ofMinutes(KatConfig.priceTimeoutMinutes.toLong())
    private val salesCache = ConcurrentHashMap<String, Pair<Double, Instant>>()
    private val salesCacheTimeout get() = Duration.ofHours(KatConfig.salesTimeoutHours.toLong())
    private val baseFetchCache = ConcurrentHashMap<String, Instant>()

    internal data class CachedPrice(
        val bazaarPrices: Map<String, ProductInfo>?,
        val lbinPrices: Map<String, Double>?,
        val timestamp: Instant
    )

    fun fetchAllPrices(force: Boolean = false) {
        if (force) {
            Log.debug(this, "Forcing price fetch, clearing cache...")
            cache.clear()
        }
        getOrFetchPrices()
    }

    fun clearSalesCache() {
        Log.debug(this, "Clearing sales cache...")
        salesCache.clear()
        baseFetchCache.clear()
    }

    internal fun getOrFetchPrices(): CachedPrice {
        val cached = cache["prices"]
        if (cached != null && Duration.between(cached.timestamp, Instant.now()) < cacheTimeout) {
            Log.debug(this, "Using cached prices (age: ${Duration.between(cached.timestamp, Instant.now()).seconds}s)")
            return cached
        }

        Log.debug(this, "Prices expired or missing, fetching from APIs...")
        val bazaar = fetchBazaarPrices()
        val lbin = fetchLbinPrices()
        Log.debug(this, "Fetched ${bazaar?.size ?: 0} Bazaar prices and ${lbin?.size ?: 0} LBIN prices")
        val newCache = CachedPrice(bazaar, lbin, Instant.now())
        cache["prices"] = newCache
        return newCache
    }

    fun getBuyPrice(itemId: String?, isInstant: Boolean, priority: SourcePriority = SourcePriority.ALL): PriceResult {
        return getPrice(itemId, isInstant, priority, true)
    }

    fun getSellPrice(itemId: String?, isInstant: Boolean, priority: SourcePriority = SourcePriority.ALL): PriceResult {
        return getPrice(itemId, isInstant, priority, false)
    }

    private fun getPrice(itemId: String?, isInstant: Boolean, priority: SourcePriority, isBuy: Boolean): PriceResult {
        if (itemId == null) return PriceResult(0.0, false)

        // Handle Agatha Coupon fallback
        if (itemId == KatConfig.agathaCouponId) {
            val prices = getOrFetchPrices()
            val bz = prices.bazaarPrices?.get(itemId)
            val price = if (KatConfig.forceAgathaCouponPrice || bz == null) {
                KatConfig.defaultAgathaCouponPrice
            } else {
                if (isBuy) {
                    if (isInstant) bz.quickStatus.buyPrice else bz.quickStatus.sellPrice
                } else {
                    if (isInstant) bz.quickStatus.sellPrice else bz.quickStatus.buyPrice
                }
            }
            return PriceResult(price, true)
        }

        // Handle NPC items with custom costs
        KatConfig.npcItemCosts[itemId]?.let { costs ->
            var totalCost = 0.0
            var allComponentsKnown = true
            costs.forEach { (componentId, amount) ->
                val componentPrice = getPrice(componentId, isInstant, priority, isBuy).price
                if (componentPrice <= 0) {
                    allComponentsKnown = false
                }
                totalCost += componentPrice * amount
            }
            if (allComponentsKnown && totalCost > 0) {
                return PriceResult(totalCost, true)
            }
        }

        val prices = getOrFetchPrices()

        val bz = prices.bazaarPrices?.get(itemId)
        var lbin = prices.lbinPrices?.get(itemId)

        // Handle PET_ prefix for pets in LBIN (try both with and without prefix)
        if (lbin == null && itemId.contains(";")) {
            val altId = if (itemId.startsWith("PET_")) itemId.substring(4) else "PET_$itemId"
            lbin = prices.lbinPrices?.get(altId)
        }

        val bazaarPrice = if (bz != null) {
            if (isBuy) {
                if (isInstant) bz.quickStatus.buyPrice else bz.quickStatus.sellPrice
            } else {
                if (isInstant) bz.quickStatus.sellPrice else bz.quickStatus.buyPrice
            }
        } else 0.0

        return when (priority) {
            SourcePriority.BAZAAR_FIRST -> {
                if (bz != null) {
                    PriceResult(bazaarPrice, true)
                } else {
                    PriceResult(lbin ?: 0.0, false)
                }
            }
            SourcePriority.AH_FIRST -> {
                if (lbin != null) {
                    PriceResult(lbin, false)
                } else if (bz != null) {
                    PriceResult(bazaarPrice, true)
                } else {
                    PriceResult(0.0, false)
                }
            }
            SourcePriority.ALL -> {
                if (bz != null) {
                    PriceResult(bazaarPrice, true)
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

    fun fetchHourlySales(itemId: String): Double {
        val now = Instant.now()
        val cached = salesCache[itemId]
        if (cached != null && Duration.between(cached.second, now) < salesCacheTimeout) {
            Log.debug(this, "Using cached sales for $itemId")
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
        var auctions: List<CoflAuction>?

        fun tryFetch(id: String): List<CoflAuction>? {
            for (size in pageSizes) {
                val url = KatConfig.coflnetSoldAuctionsUrl
                    .replace("{itemId}", id)
                    .replace("{pageSize}", size.toString())
                
                val result = fetchJson(url, object : TypeToken<List<CoflAuction>>() {})
                if (result != null && result.isNotEmpty()) {
                    return result
                }
            }
            return null
        }

        return try {
            Log.debug(this, "Fetching hourly sales for $fetchId...")
            auctions = tryFetch(fetchId)
            
            // Try without PET_ prefix if it failed and was a pet
            if ((auctions == null || auctions.isEmpty()) && isPet && fetchId.startsWith("PET_")) {
                val altId = fetchId.substring(4)
                Log.debug(this, "Retrying without PET_ prefix for $altId")
                auctions = tryFetch(altId)
            }

            if (auctions == null || auctions.isEmpty()) {
                Log.debug(this, "No sales found for $fetchId")
                if (isPet) {
                    baseFetchCache[originalBaseId] = now
                    for (i in 0..5) salesCache["$originalBaseId;$i"] = Pair(0.0, now)
                } else {
                    salesCache[normalizedItemId] = Pair(0.0, now)
                }
                return 0.0
            }

            if (isPet) {
                Log.debug(this, "Processing pet sales for $originalBaseId")
                baseFetchCache[originalBaseId] = now
                val rarityMap = KatConfig.rarityNumbers
                
                // Initialize all rarities with 0 for this pet
                for (i in 0..5) salesCache["$originalBaseId;$i"] = Pair(0.0, now)

                // Group and calculate
                val grouped = auctions.groupBy { it.tier?.uppercase() ?: "UNKNOWN" }
                grouped.forEach { (tier, tierAuctions) ->
                    val rNum = rarityMap[tier]
                    if (rNum != null) {
                        val earliest = parseCoflDate(tierAuctions.last().end)
                        val durationHours = max(1.0, Duration.between(earliest, now).toMillis() / (1000.0 * 60.0 * 60.0))
                        val hourlySales = tierAuctions.size.toDouble() / durationHours
                        Log.debug(this, "Sales for $originalBaseId $tier: $hourlySales/hr")
                        salesCache["$originalBaseId;$rNum"] = Pair(hourlySales, now)
                    }
                }
                salesCache[normalizedItemId]?.first ?: 0.0
            } else {
                val earliest = parseCoflDate(auctions.last().end)
                val durationHours = max(1.0, Duration.between(earliest, now).toMillis() / (1000.0 * 60.0 * 60.0))
                val totalSales = auctions.size.toDouble()
                val hourlySales = totalSales / durationHours
                
                Log.debug(this, "Sales for $normalizedItemId: $hourlySales/hr")
                salesCache[normalizedItemId] = Pair(hourlySales, now)
                hourlySales
            }
        } catch (e: Exception) {
            Log.debug(this, "Failed to fetch sales for $itemId: ${e.message}")
            return 0.0
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
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.string()?.let { gson.fromJson(it, typeToken.type) }
            } else {
                Log.debug(this, "Failed to fetch $url - Code: ${response.code}")
                null
            }
        }
    } catch (e: Exception) {
        Log.debug(this, "Exception fetching $url: ${e.message}")
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
