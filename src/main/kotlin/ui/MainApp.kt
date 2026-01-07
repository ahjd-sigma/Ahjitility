package ui

import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import ui.forge.ForgeCalculator

class MainApp : Application() {
    private val DARK_BG = "#2b2b2b"
    private val ACCENT_COLOR = "#4a90e2"

    private lateinit var primaryStage: Stage
    private lateinit var mainMenuScene: Scene

    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage

        // Create main menu
        mainMenuScene = createMainMenu()

        primaryStage.title = "Ahjitility"
        primaryStage.scene = mainMenuScene
        primaryStage.width = 600.0
        primaryStage.height = 500.0
        primaryStage.show()
    }

    private fun createMainMenu(): Scene {
        val root = VBox(20.0)
        root.alignment = Pos.CENTER
        root.padding = Insets(40.0)
        root.style = "-fx-background-color: $DARK_BG;"

        // Title with gradient effect
        val title = Label("Ahjitility")
        title.style = """
            -fx-text-fill: linear-gradient(to right, #4a90e2, #00d4ff, #4a90e2);
            -fx-font-size: 32px;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(gaussian, rgba(74, 144, 226, 0.6), 10, 0.5, 0, 2);
        """.trimIndent()

        // Calculator buttons
        val forgeButton = createCalculatorButton(
            "Forge Calculator",
            "Calculate profits for forge recipes"
        ) {
            openCalculator(ForgeCalculator())
        }

        val bazaarButton = createCalculatorButton(
            "Bazaar Flipper",
            "Coming soon..."
        ) {
            // openCalculator(ui.calculators.BazaarFlipperCalculator())
        }
        bazaarButton.isDisable = true
        bazaarButton.opacity = 0.5

        val auctionButton = createCalculatorButton(
            "Auction House Tracker",
            "Coming soon..."
        ) {
            // openCalculator(ui.calculators.AuctionTrackerCalculator())
        }
        auctionButton.isDisable = true
        auctionButton.opacity = 0.5

        // Container for buttons
        val buttonContainer = VBox(15.0)
        buttonContainer.alignment = Pos.CENTER
        buttonContainer.children.addAll(forgeButton, bazaarButton, auctionButton)

        VBox.setVgrow(buttonContainer, Priority.ALWAYS)

        root.children.addAll(title, buttonContainer)

        val scene = Scene(root)

        // Make buttons responsive to window width
        scene.widthProperty().addListener { _, _, newWidth ->
            val buttonWidth = (newWidth.toDouble() * 0.7).coerceIn(300.0, 500.0)
            forgeButton.prefWidth = buttonWidth
            bazaarButton.prefWidth = buttonWidth
            auctionButton.prefWidth = buttonWidth
        }

        return scene
    }

    private fun createCalculatorButton(
        title: String,
        description: String,
        action: () -> Unit
    ): Button {
        val button = Button()

        val titleLabel = Label(title)
        titleLabel.style = """
            -fx-text-fill: white;
            -fx-font-size: 18px;
            -fx-font-weight: bold;
        """.trimIndent()

        val descLabel = Label(description)
        descLabel.style = """
            -fx-text-fill: #cccccc;
            -fx-font-size: 12px;
        """.trimIndent()

        val content = VBox(5.0)
        content.alignment = Pos.CENTER_LEFT
        content.children.addAll(titleLabel, descLabel)

        button.graphic = content
        button.minWidth = 300.0
        button.maxWidth = 500.0
        button.prefWidth = 400.0
        button.prefHeight = 80.0
        button.style = """
            -fx-background-color: #3a3a3a;
            -fx-text-fill: white;
            -fx-background-radius: 8px;
            -fx-cursor: hand;
            -fx-border-color: #555555;
            -fx-border-width: 1px;
            -fx-border-radius: 8px;
        """.trimIndent()

        button.setOnMouseEntered {
            button.style = """
                -fx-background-color: #4a4a4a;
                -fx-text-fill: white;
                -fx-background-radius: 8px;
                -fx-cursor: hand;
                -fx-border-color: $ACCENT_COLOR;
                -fx-border-width: 2px;
                -fx-border-radius: 8px;
            """.trimIndent()
        }

        button.setOnMouseExited {
            button.style = """
                -fx-background-color: #3a3a3a;
                -fx-text-fill: white;
                -fx-background-radius: 8px;
                -fx-cursor: hand;
                -fx-border-color: #555555;
                -fx-border-width: 1px;
                -fx-border-radius: 8px;
            """.trimIndent()
        }

        button.setOnAction { action() }

        return button
    }

    private fun openCalculator(calculator: Calculator) {
        // Just swap the scene, keep window size/position as-is
        val scene = calculator.createScene { returnToMainMenu() }
        primaryStage.scene = scene
    }

    private fun returnToMainMenu() {
        // Just swap back to main menu, keep window size/position as-is
        primaryStage.scene = mainMenuScene
    }
}

// Base interface for all calculators
interface Calculator {
    val preferredWidth: Double
    val preferredHeight: Double
    fun createScene(onBack: () -> Unit): Scene
}