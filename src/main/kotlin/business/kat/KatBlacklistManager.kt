package business.kat

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import utils.KatConfig
import utils.Log
import java.io.File

object KatBlacklistManager {
    private val gson = Gson()
    private val blacklistFile get() = File(KatConfig.katBlacklistPath)
    private var blacklistedFamilies = mutableSetOf<String>()

    init {
        load()
    }

    private fun load() {
        try {
            if (blacklistFile.exists()) {
                val content = blacklistFile.readText()
                val type = object : TypeToken<Set<String>>() {}.type
                blacklistedFamilies = gson.fromJson<Set<String>>(content, type).toMutableSet()
            }
        } catch (e: Exception) {
            Log.debug(this, "Failed to load Kat blacklist", e)
        }
    }

    private fun save() {
        // Run save in a background thread to not block UI
        Thread {
            try {
                synchronized(this) {
                    if (!blacklistFile.parentFile.exists()) {
                        blacklistFile.parentFile.mkdirs()
                    }
                    blacklistFile.writeText(gson.toJson(blacklistedFamilies))
                }
            } catch (e: Exception) {
                Log.debug(this, "Failed to save Kat blacklist", e)
            }
        }.start()
    }

    fun isBlacklisted(familyName: String): Boolean {
        return blacklistedFamilies.contains(familyName)
    }

    fun toggleBlacklist(familyName: String) {
        if (blacklistedFamilies.contains(familyName)) {
            blacklistedFamilies.remove(familyName)
        } else {
            blacklistedFamilies.add(familyName)
        }
        save()
    }
}
