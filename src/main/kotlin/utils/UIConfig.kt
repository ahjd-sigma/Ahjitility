package utils

object UIConfig {
    data class Dimensions(val width: Double, val height: Double)
    data class Spacing(val small: Double = 5.0, val medium: Double = 10.0, val large: Double = 20.0)
    
    val calculator = Dimensions(900.0, 700.0)
    val spacing = Spacing()
    
    val SIDEBAR_WIDTH get() = KatUIConfig.sidebarWidth
}
