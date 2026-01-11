import calculator.Calculator
import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.*
import javafx.stage.Stage
import ui.forge.ForgeCalculator
import ui.shard.ShardGrindingCalculator
import ui.kat.KatCalculator
import ui.settings.SettingsUI
import utils.PriceFetcher
import utils.Styles

class MainApp : Application() {
    private lateinit var primaryStage: Stage
    private val priceFetcher = PriceFetcher()

    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage
        primaryStage.title = "Ahjitility"
        primaryStage.scene = createMainMenu()
        primaryStage.width = 900.0
        primaryStage.height = 700.0
        primaryStage.centerOnScreen()
        primaryStage.show()
    }

    private fun createMainMenu(): Scene {
        val root = VBox(20.0).apply {
            alignment = Pos.CENTER
            padding = Insets(40.0)
            
            style = """
                -fx-background-color: ${Styles.DARK_BG};
                -fx-accent-color: ${utils.GeneralConfig.colorAccentBlue};
                -fx-dark-bg: ${utils.GeneralConfig.colorDarkBg};
                -fx-darker-bg: ${utils.GeneralConfig.colorDarkerBg};
                -fx-text-primary: ${utils.GeneralConfig.colorTextPrimary};
                -fx-field-bg: ${utils.GeneralConfig.colorFieldBg};
                -fx-button-bg: ${utils.GeneralConfig.colorButtonBg};
                -fx-border-color-global: ${utils.GeneralConfig.colorBorder};
            """.trimIndent()

            children.addAll(
                VBox(5.0).apply {
                    alignment = Pos.CENTER
                    children.addAll(
                        Label("Ahjitility").apply {
                            style = "-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;"
                        },
                        Label("Skyblock Utility Suite").apply {
                            style = "-fx-text-fill: #888888; -fx-font-size: 14px;"
                        }
                    )
                },
                
                VBox(15.0).apply {
                    alignment = Pos.CENTER
                    maxWidth = 400.0
                    
                    children.addAll(
                        createMenuButton("Forge Calculator", "Calculate profits for forge recipes") { open(ForgeCalculator(priceFetcher)) },
                        createMenuButton("Kat Flipper", "Profits from upgrading pets via Kat") { open(KatCalculator(priceFetcher)) },
                        createMenuButton("Shard Grinding", "Hourly profits of manual grinding") { open(ShardGrindingCalculator(priceFetcher)) },
                        createMenuButton("Options", "Modify settings and configurations") { 
                            val settings = SettingsUI { 
                                val menu = createMainMenu()
                                primaryStage.scene = menu 
                            }
                            primaryStage.scene = settings.createScene()
                        }
                    )
                    VBox.setVgrow(this, Priority.ALWAYS)
                },

                Label("v1.0.0").apply {
                    style = "-fx-text-fill: #555555; -fx-font-size: 12px;"
                }
            )
        }

        return Scene(root, 900.0, 700.0).apply {
            stylesheets.add(MainApp::class.java.getResource("/style.css")?.toExternalForm())
        }
    }

    private fun createMenuButton(title: String, desc: String, action: () -> Unit = {}) = Button().apply {
        maxWidth = Double.MAX_VALUE
        val baseColor = utils.GeneralConfig.colorButtonBg
        val hoverColor = "#4a4d50" // We could also make this dynamic
        
        style = """
            -fx-background-color: $baseColor; -fx-text-fill: white; -fx-padding: 15;
            -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: center-left;
        """.trimIndent()
        
        graphic = VBox(5.0).apply {
            children.addAll(
                Label(title).apply { style = "-fx-text-fill: ${Styles.ACCENT}; -fx-font-size: 16px; -fx-font-weight: bold;" },
                Label(desc).apply { style = "-fx-text-fill: #888888; -fx-font-size: 12px;" }
            )
        }
        setOnAction { action() }
        
        setOnMouseEntered { style = style.replace(baseColor, hoverColor) }
        setOnMouseExited { style = style.replace(hoverColor, baseColor) }
    }

    private fun open(calc: Calculator) {
        val scene = calc.createScene { 
            primaryStage.scene = createMainMenu()
        }
        scene.stylesheets.add(MainApp::class.java.getResource("/style.css")?.toExternalForm())
        primaryStage.scene = scene
    }
}
