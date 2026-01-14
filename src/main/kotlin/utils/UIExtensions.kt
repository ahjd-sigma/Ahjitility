package utils

import javafx.animation.AnimationTimer
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import kotlin.math.abs

fun String.label(
    color: String = GeneralConfig.colorTextPrimary, 
    size: Any = GeneralConfig.fontSizeSmall, 
    bold: Boolean = false,
    visible: Boolean = true
) = Label(this).apply { 
    val sizeStr = if (size is Number) "${size}px" else size.toString()
    style = buildString {
        append("-fx-text-fill: $color; -fx-font-size: $sizeStr;")
        if (bold) append(" -fx-font-weight: bold;")
    }.ensureSemicolon()
    isVisible = visible
}

private fun String.ensureSemicolon(): String = if (trim().endsWith(";")) this else "$this;"

fun priceLabel(
    value: Double?, 
    format: String = "%,.0f", 
    excluded: Boolean = false,
    color: String = GeneralConfig.colorTextPrimary,
    size: Any = GeneralConfig.fontSizeSmall,
    bold: Boolean = false
): Label = valueLabel(value, format, excluded, color, size, bold)

fun hbox(
    spacing: Double = 10.0, 
    alignment: Pos = Pos.CENTER_LEFT,
    init: HBox.() -> Unit = {}
): HBox = HBox(spacing).apply { 
    this.alignment = alignment
    init()
}

fun vbox(
    spacing: Double = 10.0,
    alignment: Pos = Pos.TOP_LEFT,
    init: VBox.() -> Unit = {}
): VBox = VBox(spacing).apply { 
    this.alignment = alignment
    init() 
}

fun spacer(): Region = Region().apply { 
    HBox.setHgrow(this, Priority.ALWAYS) 
}

fun separator() = Separator(Orientation.VERTICAL)

fun formatDuration(seconds: Int): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    
    return buildString {
        if (days > 0) {
            append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
        } else if (hours > 0) {
            append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (remainingSeconds > 0) append("${remainingSeconds}s")
        } else if (minutes > 0) {
            append("${minutes}m ")
            if (remainingSeconds > 0) append("${remainingSeconds}s")
        } else {
            append("${remainingSeconds}s")
        }
    }.trim()
}

fun String.button(
    onClick: () -> Unit = {},
    width: Double? = null
): Button = Button(this).apply {
    style = Styles.button
    setOnAction { onClick() }
    if (width != null) prefWidth = width
}

fun <T> comboBox(
    items: List<T>,
    defaultValue: T? = items.firstOrNull(),
    width: Double? = null,
    onChange: (T) -> Unit = {}
): ComboBox<T> = ComboBox<T>().apply {
    this.items.addAll(items)
    value = defaultValue
    style = Styles.combo
    setOnAction { value?.let { onChange(it) } }
    if (width != null) prefWidth = width
}

fun textField(
    text: String = "",
    prompt: String = "",
    width: Double? = null,
    onChange: (String) -> Unit = {}
): TextField = TextField(text).apply {
    if (prompt.isNotEmpty()) promptText = prompt
    style = Styles.field
    textProperty().addListener { _, _, newValue -> onChange(newValue) }
    if (width != null) prefWidth = width
}

fun stylizedProgressBar(
    accentColor: String = GeneralConfig.colorAccentBlue,
    initialProgress: Double = 0.0,
    width: Double = 260.0
): VBox = VBox(4.0).apply {
    isMouseTransparent = true
    alignment = Pos.CENTER
    
    val label = Label("").apply {
        style = buildString {
            append("-fx-text-fill: white;")
            append(" -fx-font-size: 11px;")
            append(" -fx-font-weight: bold;")
            append(" -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0, 0, 1);")
        }
    }
    
    val bar = ProgressBar(initialProgress).apply {
        prefWidth = width
        prefHeight = 14.0
        minHeight = 14.0
        maxHeight = 14.0
        
        fun updateBarStyle() {
            style = buildString {
                append("-fx-accent: $accentColor;")
                append(" -fx-control-inner-background: rgba(30,30,30,0.8);")
                append(" -fx-background-color: rgba(60,60,60,0.5);")
                append(" -fx-background-insets: -1;")
                append(" -fx-padding: 0;")
                append(" -fx-background-radius: 7;")
                append(" -fx-indeterminate-bar-animation-speed: 2.0;")
            }
        }
        
        updateBarStyle()
        val listener = { updateBarStyle() }
        ConfigEvents.subscribe(listener)
        properties["configListener"] = listener
    }
    
    children.addAll(label, bar)
    userData = Pair(label, bar)
}

fun VBox.updateProgress(progress: Double, text: String = "") {
    val pair = userData as? Pair<*, *> ?: return
    val label = pair.first as? Label ?: return
    val bar = pair.second as? ProgressBar ?: return
    
    bar.progress = progress
    if (text.isNotEmpty()) label.text = text
}

fun valueLabel(
    value: Double?, 
    format: String = "%,.0f", 
    excluded: Boolean = false,
    color: String = GeneralConfig.colorTextPrimary,
    size: Any = GeneralConfig.fontSizeSmall,
    bold: Boolean = false
): Label = (if (excluded || value == null) "N/A" else String.format(format, value)).label(
    color = color,
    size = size,
    bold = bold
)

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