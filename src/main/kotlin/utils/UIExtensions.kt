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
    
    addEventFilter(ScrollEvent.SCROLL) { event ->
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
            isAutoscrolling = true
            autoscrollStartY = event.y
            cursor = Cursor.CLOSED_HAND
            event.consume()
        }
    }

    addEventFilter(MouseEvent.MOUSE_DRAGGED) { event ->
        if (isAutoscrolling) {
            if (scrollBar == null) {
                scrollBar = lookup(".scroll-bar:vertical") as? ScrollBar
            }
            
            val diffY = event.y - autoscrollStartY
            val speed = diffY * GeneralConfig.autoscrollSpeed

            scrollBar?.let { bar ->
                val newVal = (bar.value + speed).coerceIn(bar.min, bar.max)
                bar.value = newVal
            }
            event.consume()
        }
    }

    addEventFilter(MouseEvent.MOUSE_RELEASED) { event ->
        if (event.button == MouseButton.MIDDLE || isAutoscrolling) {
            isAutoscrolling = false
            cursor = Cursor.DEFAULT
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
