package business.forge

import com.google.gson.annotations.SerializedName

data class ForgeRecipe(
    @SerializedName("outputName") val displayName: String,
    @SerializedName("outputItemId") val itemId: String,
    val durationSeconds: Int,
    val ingredients: List<Ingredient>,
    val coinCost: Int? = 0
)

data class Ingredient(
    val itemId: String,
    @SerializedName("name") var displayName: String?,
    val quantity: Int
)

data class ForgeResult(
    val recipe: ForgeRecipe,
    val sellValue: Double,
    val totalCost: Double,
    val profitPerHour: Double,
    val isBazaar: Boolean, // True if the PRICE used is from Bazaar
    val hasBazaar: Boolean, // True if the item EXISTS on Bazaar (for sorting)
    val taxRate: Double,
    val effectiveDuration: Int
) {
    val profit get() = sellValue - totalCost
}

data class ForgeConfig(
    val searchText: String = "",
    val isInstantSell: Boolean = true,
    val isInstantBuy: Boolean = true,
    val slots: Int = 1,
    val quickForgeLevel: Int = 0,
    val bazaarTax: Double = 1.25,
    val ahMultiplier: Double = 1.0,
    val sortMode: SortMode = SortMode.PROFIT,
    val sourcePriority: SourcePriority = SourcePriority.ALL,
    val durationFilter: DurationFilter = DurationFilter.ALL
)

enum class SortMode { PROFIT, PROFIT_PER_HOUR }
enum class SourcePriority { ALL, BAZAAR_FIRST, AH_FIRST }
enum class DurationFilter {
    ALL, ZERO_TO_60, ONE_TO_30MIN, HALF_TO_ONE_HOUR,
    ONE_TO_6_HOURS, SIX_TO_18_HOURS, OVER_18_HOURS;

    fun matches(seconds: Int) = when (this) {
        ALL -> true
        ZERO_TO_60 -> seconds in 0..60
        ONE_TO_30MIN -> seconds in 61..1800
        HALF_TO_ONE_HOUR -> seconds in 1801..3600
        ONE_TO_6_HOURS -> seconds in 3601..21600
        SIX_TO_18_HOURS -> seconds in 21601..64800
        OVER_18_HOURS -> seconds > 64800
    }
}
