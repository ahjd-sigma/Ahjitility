package calculator

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import utils.*

abstract class BaseCalculator(protected val priceFetcher: PriceFetcher) : Calculator {
    override val preferredWidth = 900.0
    override val preferredHeight = 700.0

    protected val loading = SimpleBooleanProperty(false)
    
    private var rootNode: StackPane? = null
    private var mainContainer: VBox? = null
    private val configListener = { refreshTheme() }

    init {
        ConfigEvents.subscribe(configListener)
    }

    private fun refreshTheme() {
        rootNode?.style = """
            -fx-accent-color: ${GeneralConfig.colorAccentBlue};
            -fx-dark-bg: ${GeneralConfig.colorDarkBg};
            -fx-darker-bg: ${GeneralConfig.colorDarkerBg};
            -fx-text-primary: ${GeneralConfig.colorTextPrimary};
            -fx-field-bg: ${GeneralConfig.colorFieldBg};
            -fx-button-bg: ${GeneralConfig.colorButtonBg};
            -fx-border-color-global: ${GeneralConfig.colorBorder};
        """.trimIndent()
        
        mainContainer?.style = "-fx-background-color: ${Styles.DARK_BG};"
    }

    final override fun createScene(onBack: () -> Unit): Scene {
        val content = createContent()
        val controls = createControls()
        val overlay = createOverlay()

        val container = vbox(0.0) {
            children.addAll(
                createTopBar(onBack),
                controls,
                content.apply { VBox.setVgrow(this, Priority.ALWAYS) }
            )
        }
        mainContainer = container

        val root = StackPane(
            container,
            createLoadingOverlay()
        )
        rootNode = root

        overlay?.let {
            it.isMouseTransparent = true
            root.children.add(it)
        }

        refreshTheme()

        return Scene(root, preferredWidth, preferredHeight).apply {
            stylesheets.add(BaseCalculator::class.java.getResource("/style.css")?.toExternalForm())
        }
    }

    protected abstract fun createContent(): Region
    protected abstract fun createControls(): Region
    protected open fun createOverlay(): Region? = null

    protected open fun createTopBar(onBack: () -> Unit) = hbox(10.0) {
        padding = Insets(10.0)
        alignment = javafx.geometry.Pos.CENTER_LEFT
        children.addAll(
            spacer(),
            "← Main Menu".button(onClick = { onBack() }).apply {
                style = "-fx-background-color: #4a4a4a; -fx-text-fill: white; -fx-padding: 8 16; -fx-cursor: hand; -fx-background-radius: 5;"
            }
        )
    }

    private fun createLoadingOverlay() = "Loading...".label().apply {
        style = "-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: orange; -fx-padding: 20; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 18;"
        visibleProperty().bind(loading)
        isMouseTransparent = true
    }
}
