package utils

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import kotlin.reflect.KMutableProperty0

/**
 * Base class for configuration objects to reduce boilerplate.
 * Handles YAML loading, saving, and property management.
 */
abstract class BaseConfig(fileName: String) {
    private val configFile = File("config/$fileName")
    private val registeredProperties = mutableMapOf<String, ConfigProp<*>>()

    protected class ConfigProp<T>(
        val property: KMutableProperty0<T>,
        val yamlKey: String,
        val loadTransform: ((Any?) -> T)? = null,
        val saveTransform: ((T) -> Any?)? = null,
        val validate: ((T) -> T)? = null
    ) {
        fun apply(yamlValue: Any) {
            val loadedValue = if (loadTransform != null) {
                loadTransform.invoke(yamlValue)
            } else {
                @Suppress("UNCHECKED_CAST")
                when (property.get()) {
                    is Double -> (yamlValue as? Number)?.toDouble() as T
                    is Int -> (yamlValue as? Number)?.toInt() as T
                    is Boolean -> yamlValue as T
                    is String -> yamlValue as T
                    is List<*> -> yamlValue as T
                    is Map<*, *> -> yamlValue as T
                    else -> yamlValue as T
                }
            }
            
            val finalValue = validate?.invoke(loadedValue) ?: loadedValue
            property.set(finalValue)
        }

        fun getValueToSave(): Any? {
            val value = property.get()
            return if (saveTransform != null) {
                saveTransform.invoke(value)
            } else {
                value
            }
        }
    }

    /**
     * Registers a property to be managed by the config system.
     * @param property The property reference (e.g., ::myProp)
     * @param yamlKey The key used in the YAML file
     * @param loadTransform Optional transformation from YAML value to property type
     * @param saveTransform Optional transformation from property type to YAML value
     * @param validate Optional validation function to constrain the value
     */
    protected fun <T> register(
        property: KMutableProperty0<T>,
        yamlKey: String,
        loadTransform: ((Any?) -> T)? = null,
        saveTransform: ((T) -> Any?)? = null,
        validate: ((T) -> T)? = null
    ) {
        registeredProperties[yamlKey] = ConfigProp(property, yamlKey, loadTransform, saveTransform, validate)
    }

    /**
     * Helper to constrain a Comparable value within a range.
     */
    protected fun <T : Comparable<T>> range(min: T, max: T): (T) -> T = { it.coerceIn(min, max) }

    /**
     * Helper to validate font size strings (e.g., "14px").
     */
    protected fun fontSize(min: Int = 8, max: Int = 72): (String) -> String = { input ->
        val numeric = input.filter { it.isDigit() }.toIntOrNull() ?: 14
        val clamped = numeric.coerceIn(min, max)
        "${clamped}px"
    }

    fun loadConfig() {
        if (!configFile.exists()) {
            saveConfig()
            return
        }

        try {
            val yaml = Yaml()
            val loadedConfig = yaml.load<Map<String, Any>>(FileInputStream(configFile))
            if (loadedConfig != null) {
                applyConfig(loadedConfig)
                saveConfig() // Ensure file is updated with new defaults or corrected values
            }
        } catch (e: Exception) {
            System.err.println("Error loading config ${configFile.name}: ${e.message}")
            saveConfig()
        }
    }

    private fun applyConfig(config: Map<String, Any>) {
        for ((key, prop) in registeredProperties) {
            val yamlValue = getValueByPath(config, key)
            if (yamlValue != null) {
                try {
                    prop.apply(yamlValue)
                } catch (e: Exception) {
                    System.err.println("Failed to apply config key '$key' in ${configFile.name}: ${e.message}")
                }
            }
        }
    }

    private fun getValueByPath(map: Map<String, Any>, path: String): Any? {
        val parts = path.split('.')
        var current: Any? = map
        for (part in parts) {
            if (current !is Map<*, *>) return null
            current = current[part]
        }
        return current
    }

    fun saveConfig() {
        try {
            val configDir = configFile.parentFile
            if (configDir != null && !configDir.exists()) {
                configDir.mkdirs()
            }

            val configMap = mutableMapOf<String, Any>()
            for ((key, prop) in registeredProperties) {
                val finalValue = prop.getValueToSave()
                if (finalValue != null) {
                    setValueByPath(configMap, key, finalValue)
                }
            }

            val options = DumperOptions()
            options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            val yaml = Yaml(options)
            FileWriter(configFile).use { it.write(yaml.dump(configMap)) }
        } catch (e: Exception) {
            System.err.println("Error saving config ${configFile.name}: ${e.message}")
        }
    }

    private fun setValueByPath(map: MutableMap<String, Any>, path: String, value: Any) {
        val parts = path.split('.')
        var current = map
        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            @Suppress("UNCHECKED_CAST")
            current = current.getOrPut(part) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
        }
        current[parts.last()] = value
    }

    abstract fun resetToDefaults()
}
