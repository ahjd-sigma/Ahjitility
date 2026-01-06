package forge

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class PriceFetcher {
    private val client = OkHttpClient()
    private val gson = Gson()
    private var cachedBazaarPrices: Map<String, ProductInfo>? = null
    private var cachedLbinPrices: Map<String, Double>? = null

    fun fetchAllPrices(force: Boolean = false) {
        if (force) clearCache()
        fetchBazaarPrices()
        fetchLbinPrices()
    }

    fun clearCache() {
        cachedBazaarPrices = null
        cachedLbinPrices = null
    }

    private fun fetchBazaarPrices(): Map<String, ProductInfo> {
        // Return cached prices if available
        if (cachedBazaarPrices != null) return cachedBazaarPrices!!

        val url = "https://api.hypixel.net/v2/skyblock/bazaar"

        val request = Request.Builder()
            .url(url)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Failed to fetch bazaar data: ${response.code}")
                    return emptyMap()
                }

                val jsonResponse = response.body?.string() ?: return emptyMap()
                val bazaarData = gson.fromJson(jsonResponse, BazaarResponse::class.java)

                if (bazaarData.success) {
                    cachedBazaarPrices = bazaarData.products
                    bazaarData.products
                } else {
                    emptyMap()
                }
            }
        } catch (e: Exception) {
            println("Error fetching bazaar data: ${e.message}")
            emptyMap()
        }
    }

    private fun fetchLbinPrices(): Map<String, Double> {
        if (cachedLbinPrices != null) return cachedLbinPrices!!

        val url = "https://moulberry.codes/lowestbin.json"
        val request = Request.Builder().url(url).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Failed to fetch LBIN data: ${response.code}")
                    return emptyMap()
                }

                val jsonResponse = response.body?.string() ?: return emptyMap()
                val type = object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type
                val lbinData: Map<String, Double> = gson.fromJson(jsonResponse, type)

                cachedLbinPrices = lbinData
                lbinData
            }
        } catch (e: Exception) {
            println("Error fetching LBIN data: ${e.message}")
            emptyMap()
        }
    }

    fun getBuyPrice(itemId: String?, isInstant: Boolean): PriceResult {
        if (itemId == null) return PriceResult(0.0, false)
        val bazaarPrices = fetchBazaarPrices()
        val product = bazaarPrices[itemId]

        if (product != null) {
            // Instant Buy = buyPrice (Ask/Lowest Sell Offer)
            // Buy Order = sellPrice (Bid/Highest Buy Order)
            val price = if (isInstant) product.quickStatus.buyPrice else product.quickStatus.sellPrice
            return PriceResult(price, true)
        }

        val lbinPrices = fetchLbinPrices()
        return PriceResult(lbinPrices[itemId] ?: 0.0, false)
    }

    fun getSellPrice(itemId: String?, isInstant: Boolean): PriceResult {
        if (itemId == null) return PriceResult(0.0, false)
        val bazaarPrices = fetchBazaarPrices()
        val product = bazaarPrices[itemId]

        if (product != null) {
            // Instant Sell = sellPrice (Bid/Highest Buy Order)
            // Sell Order = buyPrice (Ask/Lowest Sell Offer)
            val price = if (isInstant) product.quickStatus.sellPrice else product.quickStatus.buyPrice
            return PriceResult(price, true)
        }

        val lbinPrices = fetchLbinPrices()
        return PriceResult(lbinPrices[itemId] ?: 0.0, false)
    }
}

data class PriceResult(val price: Double, val isBazaar: Boolean)

data class BazaarResponse(
    val success: Boolean,
    val lastUpdated: Long,
    val products: Map<String, ProductInfo>
)

data class ProductInfo(
    @SerializedName("product_id")
    val productId: String,
    @SerializedName("sell_summary")
    val sellSummary: List<OrderSummary>,
    @SerializedName("buy_summary")
    val buySummary: List<OrderSummary>,
    @SerializedName("quick_status")
    val quickStatus: QuickStatus
)

data class OrderSummary(
    val amount: Int,
    val pricePerUnit: Double,
    val orders: Int
)

data class QuickStatus(
    val productId: String,
    val sellPrice: Double,
    val sellVolume: Int,
    val sellMovingWeek: Int,
    val sellOrders: Int,
    val buyPrice: Double,
    val buyVolume: Int,
    val buyMovingWeek: Int,
    val buyOrders: Int
)