package app

import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.*
import javafx.scene.shape.Circle
import javafx.stage.Stage
import javafx.animation.Animation
import javafx.animation.RotateTransition
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.javafx.JavaFx
import utils.GeneralConfig
import utils.UpdateChecker
import utils.UpdateInfo
import MainApp

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
        val loadingBox = VBox(20.0).apply {
            alignment = Pos.CENTER
            
            children.add(Label("Ahjitility").apply {
                style = """
                    -fx-text-fill: aqua; 
                    -fx-font-size: 32px; 
                    -fx-font-weight: bold;
                    -fx-effect: dropshadow(three-pass-box, rgba(0,255,255,0.8), 15, 0, 0, 0);
                """.trimIndent()
            })
            
            children.add(Label("Checking for updates...").apply {
                style = "-fx-text-fill: #888888; -fx-font-size: 14px;"
            })
            
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
        val content = VBox(20.0).apply {
            alignment = Pos.CENTER
            padding = Insets(30.0)
            
            children.add(Label("Ahjitility").apply {
                style = """
                    -fx-text-fill: aqua; 
                    -fx-font-size: 32px; 
                    -fx-font-weight: bold;
                    -fx-effect: dropshadow(three-pass-box, rgba(0,255,255,0.8), 15, 0, 0, 0);
                """.trimIndent()
            })
            
            children.add(Label("New Version Available: v${updateInfo.version}").apply {
                style = "-fx-text-fill: ${GeneralConfig.colorAccentBlue}; -fx-font-size: 18px; -fx-font-weight: bold;"
            })

            val changelogArea = ScrollPane().apply {
                style = "-fx-background: ${GeneralConfig.colorFieldBg}; -fx-background-color: transparent;"
                prefHeight = 150.0
                maxWidth = 500.0
                content = Label(updateInfo.changelog).apply {
                    style = "-fx-text-fill: #cccccc; -fx-font-size: 13px;"
                    isWrapText = true
                    maxWidth = 480.0
                    padding = Insets(10.0)
                }
            }
            children.add(changelogArea)
            
            val btnBox = HBox(20.0).apply {
                alignment = Pos.CENTER
                
                val updateBtn = Button("Update Now").apply {
                    style = """
                        -fx-background-color: ${GeneralConfig.colorAccentBlue};
                        -fx-text-fill: white;
                        -fx-font-size: 16px;
                        -fx-font-weight: bold;
                        -fx-padding: 10 20;
                        -fx-background-radius: 8;
                        -fx-cursor: hand;
                    """.trimIndent()
                    
                    setOnAction {
                        performUpdate(this, root, updateInfo)
                    }
                }
                
                val launchBtn = Button("Launch Anyway").apply {
                    style = """
                        -fx-background-color: ${GeneralConfig.colorButtonBg};
                        -fx-text-fill: white;
                        -fx-font-size: 16px;
                        -fx-font-weight: bold;
                        -fx-padding: 10 20;
                        -fx-background-radius: 8;
                        -fx-cursor: hand;
                    """.trimIndent()
                    
                    setOnAction {
                        launchMainApp(stage)
                    }
                }
                
                children.addAll(updateBtn, launchBtn)
            }
            children.add(btnBox)
        }
        
        root.children.add(content)
    }

    private fun performUpdate(btn: Button, root: StackPane, updateInfo: UpdateInfo) {
        btn.isDisable = true
        btn.text = "Updating..."
        
        val statusLabel = Label("Starting download...").apply {
             style = "-fx-text-fill: #888888; -fx-font-size: 14px;"
        }
        (root.children[0] as VBox).children.add(statusLabel)

        scope.launch {
            try {
                UpdateChecker.downloadAndInstallUpdate(updateInfo.downloadUrl) { status ->
                    Platform.runLater {
                        statusLabel.text = status
                    }
                }
                // If we get here, the script was launched successfully
                Platform.exit()
                System.exit(0)
            } catch (e: Exception) {
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
            e.printStackTrace()
        }
    }
}
