package business.shard

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import utils.Log
import java.io.File
import utils.ResourceLoader

object ShardDataLoader {
    private val gson = Gson()

    fun loadRates(): Map<String, Double> =
        ResourceLoader.load<Map<String, Double>>("/shards/rates.json") ?: emptyMap()

    fun loadChestPrices(): Map<String, Double> {
        val file = File("config/shard_chest_prices.json")
        if (!file.exists()) return emptyMap()
        return try {
            gson.fromJson(file.readText(), object : TypeToken<Map<String, Double>>() {}.type) ?: emptyMap()
        } catch (e: Exception) {
            Log.debug(this, "Failed to load chest prices", e)
            emptyMap()
        }
    }

    fun loadBaitCounts(): Map<String, Double> {
        val file = File("config/shard_bait_counts.json")
        if (!file.exists()) return emptyMap()
        return try {
            gson.fromJson(file.readText(), object : TypeToken<Map<String, Double>>() {}.type) ?: emptyMap()
        } catch (e: Exception) {
            Log.debug(this, "Failed to load bait counts", e)
            emptyMap()
        }
    }

    fun loadProperties(): Map<String, ShardProperties> =
        ResourceLoader.load<Map<String, ShardProperties>>("/shards/fusion-properties.json") ?: emptyMap()

    fun saveChestPrices(prices: Map<String, Double>) {
        try {
            val file = File("config/shard_chest_prices.json")
            file.parentFile.mkdirs()
            val json = gson.newBuilder().setPrettyPrinting().create().toJson(prices)
            file.writeText(json)
            Log.debug(this, "Saved chest prices")
        } catch (e: Exception) {
            Log.debug(this, "Failed to save chest prices", e)
        }
    }

    fun saveBaitCounts(counts: Map<String, Double>) {
        try {
            val file = File("config/shard_bait_counts.json")
            file.parentFile.mkdirs()
            val json = gson.newBuilder().setPrettyPrinting().create().toJson(counts)
            file.writeText(json)
            Log.debug(this, "Saved bait counts")
        } catch (e: Exception) {
            Log.debug(this, "Failed to save bait counts", e)
        }
    }

    fun saveRates(rates: Map<String, Double>) {
        try {
            val json = gson.newBuilder().setPrettyPrinting().create().toJson(rates)
            val resourcePath = "/shards/rates.json"

            // Try resource path (if file protocol)
            javaClass.getResource(resourcePath)?.let {
                if (it.protocol == "file") {
                    File(it.toURI()).writeText(json)
                    Log.debug(this, "Saved rates to resource file")
                    return
                }
            }

            // Try dev path
            val devFile = File("src/main/resources$resourcePath")
            if (devFile.exists()) {
                devFile.writeText(json)
                Log.debug(this, "Saved rates to dev path")
            } else {
                Log.debug(this, "Could not save rates.json - file not found")
            }
        } catch (e: Exception) {
            Log.debug(this, "Failed to save rates", e)
        }
    }
}
