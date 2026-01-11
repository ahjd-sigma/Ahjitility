package calculator

import javafx.scene.Scene

interface Calculator {
    val preferredWidth: Double
    val preferredHeight: Double
    fun createScene(onBack: () -> Unit): Scene
}
