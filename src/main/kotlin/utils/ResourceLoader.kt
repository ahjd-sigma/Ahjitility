package utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ResourceLoader {
    val gson = Gson()

    inline fun <reified T> load(path: String): T? = try {
        object {}.javaClass.getResourceAsStream(path)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?.let { gson.fromJson(it, object : TypeToken<T>() {}.type) }
    } catch (e: Exception) {
        null
    }
}