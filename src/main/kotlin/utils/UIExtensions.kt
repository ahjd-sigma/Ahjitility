package utils

import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.geometry.Orientation

import javafx.animation.AnimationTimer
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Cursor
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import kotlin.math.abs

fun String.label(style: String = Styles.label) = Label(this).apply { this.style = style }

fun separator() = Separator(Orientation.VERTICAL)

fun formatDuration(seconds: Int): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    
    return buildString {
        if (days > 0) append("${days}d ")
        if (hours > 0) append("${hours}h ")
        if (minutes > 0) append("${minutes}m ")
        if (remainingSeconds > 0 || (days == 0 && hours == 0 && minutes == 0)) append("${remainingSeconds}s")
    }.trim()
}

fun comboBox(vararg options: String, style: String = Styles.combo) =
    ComboBox<String>().apply {
        items.addAll(options)
        value = options[0]
        this.style = style
    }

fun textField(prompt: String = "", width: Double? = null, style: String = Styles.field) =
    TextField().apply {
        promptText = prompt
        width?.let { prefWidth = it }
        this.style = style
    }

fun text(str: String, color: String, size: Double = 13.0, bold: Boolean = false) =
    Text(str).apply {
        fill = Color.web(color)
        font = Font.font(size)
        if (bold) style = "-fx-font-weight: bold;"
    }

fun <T> ListView<T>.applyDarkStyle() = apply {
    style = Styles.listView
}

fun ScrollPane.applyDarkStyle() = apply {
    style = """
        -fx-background-color: transparent;
        -fx-background: transparent;
        -fx-viewport-background-color: transparent;
        -fx-border-color: transparent;
    """.trimIndent()
}

fun Control.enableAdvancedScrolling(multiplierOverride: (() -> Double)? = null) {
    var scrollBar: ScrollBar? = null
    var isAutoscrolling = false
    var autoscrollStartY = 0.0
    var currentMouseY = 0.0
    var hasMovedSignificantDistance = false
    
    val timer = object : AnimationTimer() {
        override fun handle(now: Long) {
            if (!isAutoscrolling) return
            
            if (scrollBar == null) {
                scrollBar = lookup(".scroll-bar:vertical") as? ScrollBar
            }
            
            scrollBar?.let { bar ->
                val diffY = currentMouseY - autoscrollStartY
                val deadzone = 15.0
                if (abs(diffY) > deadzone) {
                    val effectiveDiff = if (diffY > 0) diffY - deadzone else diffY + deadzone
                    val speed = effectiveDiff * GeneralConfig.autoscrollSpeed * 2.0
                    val newVal = (bar.value + speed).coerceIn(bar.min, bar.max)
                    bar.value = newVal
                }
            }
        }
    }

    fun stopAutoscroll() {
        if (!isAutoscrolling) return
        isAutoscrolling = false
        timer.stop()
        cursor = Cursor.DEFAULT
    }

    addEventFilter(ScrollEvent.SCROLL) { event ->
        if (isAutoscrolling) {
            stopAutoscroll()
        }
        
        if (event.deltaY == 0.0 || event.isControlDown) return@addEventFilter

        if (scrollBar == null) {
            scrollBar = lookup(".scroll-bar:vertical") as? ScrollBar
        }

        scrollBar?.let { bar ->
            event.consume()
            // Normalize scroll speed to be pixel-consistent regardless of content height.
            val contentScale = if (bar.visibleAmount > 0.0 && bar.visibleAmount < 1.0) {
                bar.visibleAmount / (1.0 - bar.visibleAmount)
            } else {
                1.0
            }
            val currentMultiplier = multiplierOverride?.invoke() ?: GeneralConfig.scrollWheelMultiplier
            val delta = event.deltaY * currentMultiplier * contentScale
            val newVal = (bar.value - delta).coerceIn(bar.min, bar.max)
            bar.value = newVal
        }
    }

    addEventFilter(MouseEvent.MOUSE_PRESSED) { event ->
        if (event.button == MouseButton.MIDDLE) {
            if (isAutoscrolling) {
                stopAutoscroll()
            } else {
                isAutoscrolling = true
                autoscrollStartY = event.y
                currentMouseY = event.y
                hasMovedSignificantDistance = false
                cursor = Cursor.MOVE
                timer.start()
            }
            event.consume()
        } else if (isAutoscrolling) {
            stopAutoscroll()
            event.consume()
        }
    }

    addEventFilter(MouseEvent.MOUSE_DRAGGED) { event ->
        if (isAutoscrolling) {
            currentMouseY = event.y
            if (abs(event.y - autoscrollStartY) > 10.0) {
                hasMovedSignificantDistance = true
            }
            event.consume()
        }
    }

    addEventFilter(MouseEvent.MOUSE_MOVED) { event ->
        if (isAutoscrolling) {
            currentMouseY = event.y
            event.consume()
        }
    }

    addEventFilter(MouseEvent.MOUSE_RELEASED) { event ->
        if (event.button == MouseButton.MIDDLE && isAutoscrolling) {
            if (hasMovedSignificantDistance) {
                stopAutoscroll()
            }
            event.consume()
        }
    }
}

fun HBox.spacer() = Pane().apply { HBox.setHgrow(this, Priority.ALWAYS) }
fun VBox.spacer() = Pane().apply { VBox.setVgrow(this, Priority.ALWAYS) }

fun stylizedProgressBar(color: String = GeneralConfig.colorAccentBlue) = ProgressBar().apply {
    style = "-fx-accent: $color;"
    ConfigEvents.subscribe {
        style = "-fx-accent: ${GeneralConfig.colorAccentBlue};"
    }
}
