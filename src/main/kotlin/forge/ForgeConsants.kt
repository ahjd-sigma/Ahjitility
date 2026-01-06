package forge

import com.google.gson.annotations.SerializedName

data class Item(
    @SerializedName("outputName") val displayName: String?,
    @SerializedName("outputItemId") val itemId: String?,
    @SerializedName("durationSeconds") val durationSeconds: Int?,
    @SerializedName("ingredients") val ingredients: List<ItemStack>?,
    @SerializedName("coinCost") val coinCost: Int?,
    var value: Double = 0.0,
    var totalRecipeCost: Double = 0.0,
    var profitPerHour: Double = 0.0,
    var isBazaar: Boolean = false,
    var appliedTaxRate: Double = 0.0
)

data class ItemStack(
    val itemId: String?,
    @SerializedName("itemName") val displayName: String?,
    val quantity: Int?
)