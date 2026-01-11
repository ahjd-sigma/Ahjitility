package ui.kat

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TextField
import javafx.scene.control.ComboBox
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.Node
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import utils.KatUIConfig
import utils.ConfigEvents
import utils.GeneralConfig

fun label(
    text: String, 
    color: String = KatUIConfig.labelColorPrimary, 
    size: Double = KatUIConfig.fontSizeNormal, 
    bold: Boolean = false,
    visible: Boolean = true
): Label = Label(text).apply { 
    style = buildString {
        append("-fx-text-fill: $color; -fx-font-size: ${size.toInt()}px;")
        if (bold) append(" -fx-font-weight: bold;")
    }
    isVisible = visible
}

fun hbox(
    spacing: Double = KatUIConfig.spacingSmall, 
    alignment: Pos = Pos.CENTER_LEFT,
    init: HBox.() -> Unit
): HBox = HBox(spacing).apply { 
    this.alignment = alignment
    init()
}

fun vbox(
    spacing: Double = KatUIConfig.spacingSmall,
    init: VBox.() -> Unit
): VBox = VBox(spacing).apply { init() }

fun spacer(): Region = Region().apply { 
    HBox.setHgrow(this, Priority.ALWAYS) 
}

fun controlRow(
    spacing: Double = KatUIConfig.controlSpacing,
    alignment: Pos = Pos.CENTER_LEFT,
    vararg controls: Pair<String, Node>
): HBox = HBox(spacing).apply {
    this.alignment = alignment
    controls.forEach { (labelText, control) ->
        if (labelText.isNotEmpty()) {
            children.add(label(
                text = labelText,
                color = KatUIConfig.labelColorPrimary,
                size = KatUIConfig.fontSizeLarge
            ))
        }
        children.add(control)
    }
}

fun titleRow(
    title: String,
    rightContent: Node
): HBox = HBox(KatUIConfig.controlGroupSpacing).apply {
    alignment = Pos.CENTER_LEFT
    children.addAll(
        label(
            text = title,
            color = KatUIConfig.labelColorPrimary,
            size = KatUIConfig.fontSizeTitle,
            bold = true
        ),
        spacer(),
        rightContent
    )
}

fun priceLabel(
    value: Double?, 
    format: String = KatUIConfig.formatPrice, 
    excluded: Boolean = false,
    color: String = KatUIConfig.labelColorSecondary
): Label = label(
    text = if (excluded || value == null) "N/A" else String.format(format, value),
    color = color,
    size = KatUIConfig.fontSizeSmall
)

fun valueLabel(
    value: Double?, 
    format: String = KatUIConfig.formatPrice, 
    excluded: Boolean = false,
    color: String = KatUIConfig.labelColorSecondary,
    size: Double = KatUIConfig.fontSizeSmall
): Label = label(
    text = if (excluded || value == null) "N/A" else String.format(format, value),
    color = color,
    size = size
)

fun comboBox(
    items: List<String>,
    defaultValue: String = items.first(),
    width: Double = KatUIConfig.sortByWidth,
    onChange: () -> Unit = {}
): ComboBox<String> = ComboBox<String>().apply {
    this.items.addAll(items)
    value = defaultValue
    setOnAction { onChange() }
    prefWidth = width
}

fun textField(
    text: String = "",
    prompt: String = "",
    width: Double = KatUIConfig.searchFieldWidth,
    onChange: () -> Unit = {}
): TextField = TextField(text).apply {
    if (prompt.isNotEmpty()) promptText = prompt
    style = buildString {
        append("-fx-background-color: ${KatUIConfig.colorFieldBg};")
        append(" -fx-text-fill: white;")
        if (prompt.isNotEmpty()) append(" -fx-prompt-text-fill: ${KatUIConfig.labelColorMuted};")
        append(" -fx-font-size: ${KatUIConfig.fontSizeNormal}px;")
        append(" -fx-background-radius: ${KatUIConfig.borderRadiusSmall};")
    }
    textProperty().addListener { _, _, _ -> onChange() }
    prefWidth = width
}

fun button(
    text: String,
    onClick: () -> Unit = {},
    width: Double? = null
): Button = Button(text).apply {
    style = buildString {
        append("-fx-background-color: ${KatUIConfig.colorButtonBg};")
        append(" -fx-text-fill: white;")
        append(" -fx-cursor: hand;")
        append(" -fx-font-size: ${KatUIConfig.fontSizeSmall + 3}px;")
        append(" -fx-padding: ${KatUIConfig.paddingSmall} ${KatUIConfig.paddingMedium};")
        append(" -fx-background-radius: ${KatUIConfig.borderRadiusSmall};")
    }
    setOnAction { onClick() }
    if (width != null) prefWidth = width
}

fun stylizedProgressBar(
    accentColor: String = KatUIConfig.accentBlue,
    initialProgress: Double = 0.0
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
        prefWidth = 260.0
        prefHeight = 14.0
        minHeight = 14.0
        maxHeight = 14.0
        
        fun updateBarStyle() {
            style = buildString {
                append("-fx-accent: $accentColor;")
                append(" -fx-control-inner-background: rgba(30,30,30,0.8);") // Dark track
                append(" -fx-background-color: rgba(60,60,60,0.5);") // Subtle outer glow/border
                append(" -fx-background-insets: -1;")
                append(" -fx-padding: 0;")
                append(" -fx-background-radius: 7;")
                append(" -fx-indeterminate-bar-animation-speed: 2.0;")
            }
        }
        
        updateBarStyle()
        val listener = { updateBarStyle() }
        ConfigEvents.subscribe(listener)
        // Store listener to prevent GC and allow potential cleanup
        properties["configListener"] = listener
    }
    
    children.addAll(label, bar)
    
    // Extension properties/methods aren't easy here, so we'll just expose children
    userData = Pair(label, bar)
}

// Helper to update stylized bar
@Suppress("UNCHECKED_CAST")
fun VBox.updateProgress(progress: Double, text: String) {
    val (label, bar) = userData as Pair<Label, ProgressBar>
    label.text = text
    bar.progress = progress
}