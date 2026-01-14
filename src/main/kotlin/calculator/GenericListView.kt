package calculator

import javafx.scene.control.*
import javafx.scene.Node
import utils.Styles
import utils.applyDarkStyle
import utils.enableAdvancedScrolling

class GenericListView<T>(
    private val scrollMultiplier: (() -> Double)? = null,
    private val cellFactory: (T) -> Node
) {
    val listView = ListView<T>().apply {
        applyDarkStyle()
        enableAdvancedScrolling(scrollMultiplier)
        setCellFactory {
            object : ListCell<T>() {
                override fun updateItem(item: T?, empty: Boolean) {
                    super.updateItem(item, empty)
                    graphic = if (empty || item == null) null else cellFactory(item)
                    style = "-fx-background-color: ${Styles.DARKER_BG};"
                }
            }
        }
    }

    var items: List<T>
        get() = listView.items
        set(value) {
            listView.items.setAll(value)
        }

    fun onSelectionChanged(handler: (T?) -> Unit) {
        listView.selectionModel.selectedItemProperty().addListener { _, _, newItem ->
            handler(newItem)
        }
    }
}
