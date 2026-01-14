import calculator.Calculator
import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.layout.*
import javafx.stage.Stage
import ui.forge.ForgeCalculator
import ui.shard.ShardGrindingCalculator
import ui.kat.KatCalculator
import ui.settings.SettingsUI
import utils.*

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
        val root = vbox(20.0, alignment = Pos.CENTER) {
            padding = Insets(40.0)
            
            style = """
                -fx-background-color: ${Styles.DARK_BG};
                -fx-accent-color: ${GeneralConfig.colorAccentBlue};
                -fx-dark-bg: ${GeneralConfig.colorDarkBg};
                -fx-darker-bg: ${GeneralConfig.colorDarkerBg};
                -fx-text-primary: ${GeneralConfig.colorTextPrimary};
                -fx-field-bg: ${GeneralConfig.colorFieldBg};
                -fx-button-bg: ${GeneralConfig.colorButtonBg};
                -fx-border-color-global: ${GeneralConfig.colorBorder};
            """.trimIndent()

            children.addAll(
                vbox(5.0, alignment = Pos.CENTER) {
                    children.addAll(
                        "Ahjitility".label(color = "aqua", size = "32px", bold = true).apply {
                            style += " -fx-effect: dropshadow(three-pass-box, rgba(0,255,255,0.8), 15, 0, 0, 0);"
                        },
                        "Skyblock Utility Calculators".label(color = "#888888", size = "14px")
                    )
                },
                
                vbox(15.0, alignment = Pos.CENTER) {
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

                "v${GeneralConfig.VERSION}".label(color = "#555555", size = "12px")
            )
        }

        return Scene(root, 900.0, 700.0).apply {
            stylesheets.add(MainApp::class.java.getResource("/style.css")?.toExternalForm())
        }
    }

    private fun createMenuButton(title: String, desc: String, action: () -> Unit = {}) = "".button().apply {
        maxWidth = Double.MAX_VALUE
        val baseColor = GeneralConfig.colorButtonBg
        val hoverColor = "#4a4d50" // We could also make this dynamic
        
        style = """
            -fx-background-color: $baseColor; -fx-text-fill: white; -fx-padding: 15;
            -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: center-left;
        """.trimIndent()
        
        graphic = vbox(5.0) {
            children.addAll(
                title.label(color = Styles.ACCENT, size = "16px", bold = true),
                desc.label(color = "#888888", size = "12px")
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
