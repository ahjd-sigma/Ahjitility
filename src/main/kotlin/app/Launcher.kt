package app

import MainApp
import javafx.animation.Animation
import javafx.animation.RotateTransition
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ScrollPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.shape.Circle
import javafx.stage.Stage
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import utils.*
import kotlin.system.exitProcess

class Launcher : Application() {
    private val scope = CoroutineScope(Dispatchers.JavaFx)

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Ahjitility - Launcher"
        
        val root = StackPane()
        root.style = "-fx-background-color: ${GeneralConfig.colorDarkBg};"
        
        val scene = Scene(root, 600.0, 400.0)
        primaryStage.scene = scene
        primaryStage.show()

        checkUpdates(root, primaryStage)
    }

    private fun checkUpdates(root: StackPane, stage: Stage) {
        val loadingBox = vbox(20.0, Pos.CENTER) {
            children.add("Ahjitility".label(color = "aqua", size = "32px", bold = true).apply {
                style += " -fx-effect: dropshadow(three-pass-box, rgba(0,255,255,0.8), 15, 0, 0, 0);"
            })
            
            children.add("Checking for updates...".label(color = "#888888", size = "14px"))
            
            val loader = Circle(10.0).apply {
                style = "-fx-stroke: ${GeneralConfig.colorAccentBlue}; -fx-fill: transparent; -fx-stroke-width: 3;"
                strokeDashArray.addAll(5.0, 5.0)
            }
            children.add(loader)
            
            RotateTransition(Duration.seconds(1.0), loader).apply {
                byAngle = 360.0
                cycleCount = Animation.INDEFINITE
                play()
            }
        }
        
        root.children.add(loadingBox)

        scope.launch {
            val updateInfo = UpdateChecker.checkForUpdates()
            
            if (updateInfo.isUpdateAvailable) {
                root.children.clear()
                showUpdateScreen(root, updateInfo, stage)
            } else {
                launchMainApp(stage)
            }
        }
    }

    private fun showUpdateScreen(root: StackPane, updateInfo: UpdateInfo, stage: Stage) {
        val content = vbox(20.0, Pos.CENTER) {
            padding = Insets(30.0)
            
            children.add("Ahjitility".label(color = "aqua", size = "32px", bold = true).apply {
                style += " -fx-effect: dropshadow(three-pass-box, rgba(0,255,255,0.8), 15, 0, 0, 0);"
            })
            
            children.add("New Version Available: v${updateInfo.version}".label(color = "orange", size = "18px", bold = true))
            
            val scroll = ScrollPane().apply {
                style = "-fx-background-color: transparent; -fx-background: transparent;"
                content = "Changelog:\n${updateInfo.changelog}".label(color = "#aaaaaa", size = "13px").apply {
                    isWrapText = true
                    maxWidth = 500.0
                }
            }
            children.add(scroll)
            
            val buttons = hbox(15.0, Pos.CENTER) {
                children.addAll(
                    "Update Now".button().apply {
                        style += " -fx-background-color: #00aa00; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20;"
                        setOnAction { performUpdate(this, root, updateInfo) }
                    },
                    "Later".button(onClick = { launchMainApp(stage) }).apply {
                        style += " -fx-background-color: #555555; -fx-font-size: 14px; -fx-padding: 10 20;"
                    }
                )
            }
            children.add(buttons)
        }
        
        root.children.add(content)
    }

    private fun performUpdate(btn: Button, root: StackPane, updateInfo: UpdateInfo) {
        btn.isDisable = true
        btn.text = "Updating..."
        
        val statusLabel = "Starting download...".label(color = "orange", size = "14px").apply {
            padding = Insets(10.0)
        }
        (root.children[0] as VBox).children.add(statusLabel)

        scope.launch {
            try {
                UpdateChecker.downloadAndInstallUpdate(updateInfo) { status ->
                    Platform.runLater {
                        statusLabel.text = status
                    }
                }
                // If we get here, the script was launched successfully
                Platform.exit()
                exitProcess(0)
            } catch (e: Exception) {
                Log.debug(this, "Failed to download or install update", e)
                 Platform.runLater {
                    statusLabel.text = "Error: ${e.message}"
                    statusLabel.style = "-fx-text-fill: #e74c3c; -fx-font-size: 14px;"
                    btn.text = "Retry Update"
                    btn.isDisable = false
                }
            }
        }
    }

    private fun launchMainApp(stage: Stage) {
        stage.close()
        try {
            val mainApp = MainApp()
            val newStage = Stage()
            mainApp.start(newStage)
        } catch (e: Exception) {
            Log.debug(this, "Failed to launch MainApp", e)
        }
    }
}
