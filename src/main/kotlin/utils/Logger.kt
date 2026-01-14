package utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A simple, consistent logging utility for the application.
 */
object Log {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    enum class Level(val prefix: String) {
        DEBUG("[DEBUG]"),
    }

    private fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        if (level == Level.DEBUG && !GeneralConfig.debugMode) return

        val timestamp = LocalDateTime.now().format(formatter)
        val formattedMessage = "$timestamp ${level.prefix} $tag: $message"
        println(formattedMessage)
        throwable?.printStackTrace()
    }

    fun debug(tag: String, message: String, throwable: Throwable? = null) = log(Level.DEBUG, tag, message, throwable)
    fun debug(caller: Any, message: String, throwable: Throwable? = null) = debug(caller.javaClass.simpleName, message, throwable)
}
