package business.shard

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import utils.ResourceLoader

object ShardDataLoader {
    private val gson = Gson()

    fun loadRates(): Map<String, Double> =
        ResourceLoader.load<Map<String, Double>>("/shards/rates.json") ?: emptyMap()

    fun loadProperties(): Map<String, ShardProperties> =
        ResourceLoader.load<Map<String, ShardProperties>>("/shards/fusion-properties.json") ?: emptyMap()

    fun saveRates(rates: Map<String, Double>) {
        try {
            val json = gson.newBuilder().setPrettyPrinting().create().toJson(rates)
            val resourcePath = "/shards/rates.json"

            // Try resource path (if file protocol)
            javaClass.getResource(resourcePath)?.let {
                if (it.protocol == "file") {
                    File(it.toURI()).writeText(json)
                    return
                }
            }

            // Try dev path
            File("src/main/resources$resourcePath").takeIf { it.exists() }?.writeText(json)
                ?: println("ERROR: Could not save rates.json")
        } catch (e: Exception) {
            println("ERROR: Failed to save rates: ${e.message}")
        }
    }
}
