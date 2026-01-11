package utils

import okhttp3.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class RecipeFetcher {
    private val client = OkHttpClient()
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val recipeCache = ConcurrentHashMap<String, Map<String, Int>>()
    private val cacheFile = File("recipe_cache.json")

    init {
        loadCacheFromFile()
    }

    private fun loadCacheFromFile() {
        if (cacheFile.exists()) {
            try {
                val json = cacheFile.readText()
                val type = object : TypeToken<Map<String, Map<String, Int>>>() {}.type
                val loadedCache: Map<String, Map<String, Int>> = gson.fromJson(json, type)
                recipeCache.putAll(loadedCache)
                println("[DEBUG] RecipeFetcher: Loaded ${recipeCache.size} recipes from cache file")
            } catch (e: Exception) {
                println("[DEBUG] RecipeFetcher: Failed to load cache from file: ${e.message}")
            }
        }
    }

    private fun saveCacheToFile() {
        try {
            val json = gson.toJson(recipeCache)
            cacheFile.writeText(json)
            println("[DEBUG] RecipeFetcher: Saved ${recipeCache.size} recipes to cache file")
        } catch (e: Exception) {
            println("[DEBUG] RecipeFetcher: Failed to save cache to file: ${e.message}")
        }
    }

    fun clearCache() {
        recipeCache.clear()
        if (cacheFile.exists()) cacheFile.delete()
        println("[DEBUG] RecipeFetcher: Cache cleared manually")
    }

    // Rate limiting: 30 requests per 10 seconds (as recommended in API-INFO.md)
    private var requestCount = 0
    private var windowStart = System.currentTimeMillis()
    private val lock = Any()

    private fun checkRateLimit() {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            if (now - windowStart > 10000) {
                // Reset window
                requestCount = 0
                windowStart = now
            }
            
            if (requestCount >= 30) {
                val waitTime = 10000 - (now - windowStart)
                if (waitTime > 0) {
                    Thread.sleep(waitTime)
                }
                requestCount = 0
                windowStart = System.currentTimeMillis()
            }
            
            requestCount++
        }
    }

    private val pendingRequests = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var totalRequested = 0
    private var completedRequested = 0

    fun hasPendingRequests(): Boolean = pendingRequests.isNotEmpty()
    
    fun getProgress(): Double {
        if (totalRequested == 0) return 1.0
        return completedRequested.toDouble() / totalRequested.toDouble()
    }

    fun resetProgress() {
        totalRequested = 0
        completedRequested = 0
    }

    fun getCraftingMaterials(petName: String, rarityNum: Int): Map<String, Int>? {
        val cacheKey = "$petName;$rarityNum"
        return recipeCache[cacheKey]
    }

    fun fetchRecipeInBackground(petName: String, rarityNum: Int, onComplete: () -> Unit) {
        val cacheKey = "$petName;$rarityNum"
        if (recipeCache.containsKey(cacheKey) || !pendingRequests.add(cacheKey)) {
            return
        }

        totalRequested++

        // Use a background thread for fetching
        Thread {
            try {
                val materials = fetchRecipeBlocking(petName, rarityNum)
                if (materials != null) {
                    onComplete()
                }
            } catch (e: Exception) {
                // Ignore background errors
            } finally {
                completedRequested++
                pendingRequests.remove(cacheKey)
            }
        }.start()
    }

    private fun fetchRecipeBlocking(petName: String, rarityNum: Int): Map<String, Int>? {
        val cacheKey = "$petName;$rarityNum"
        recipeCache[cacheKey]?.let { return it }

        checkRateLimit()

        // Format: BLAZE%3B0 (where %3B is ;)
        val formattedName = petName.uppercase().replace(" ", "_")
        val url = "https://sky.coflnet.com/api/craft/recipe/$formattedName%3B$rarityNum"
        
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body.isNullOrBlank() || body == "null") return null

                    // Response is a map of slot to itemID:Quantity, e.g., {"A1":"EGG:1", "A2":"ENCHANTED_CHICKEN:64", ...}
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val rawRecipe: Map<String, String>? = try {
                        gson.fromJson(body, type)
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (rawRecipe == null) return null
                    
                    val materials = mutableMapOf<String, Int>()
                    rawRecipe.values.forEach { entry ->
                        if (entry.contains(":")) {
                            val parts = entry.split(":")
                            val itemId = parts[0]
                            val amount = parts[1].toIntOrNull() ?: 1
                            materials[itemId] = materials.getOrDefault(itemId, 0) + amount
                        }
                    }
                    recipeCache[cacheKey] = materials
                    saveCacheToFile()
                    materials
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
