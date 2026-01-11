package business.kat

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import utils.KatConfig
import java.io.File
import java.net.URI
import java.net.URL

object KatDataLoader {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val jsonFile get() = File(KatConfig.katJsonPath)

    private val rarityMap get() = KatConfig.rarityNumbers

    fun load(): List<KatFamily> {
        if (!jsonFile.exists()) return fetchAndSave()

        return try {
            val listType = object : TypeToken<List<KatRecipe>>() {}.type
            val recipes: List<KatRecipe> = gson.fromJson(jsonFile.readText(), listType)
            groupIntoFamilies(recipes)
        } catch (e: Exception) {
            // Log error or handle silently in production
            fetchAndSave()
        }
    }

    private fun fetchAndSave(): List<KatFamily> = try {
        val jsonString = URI(KatConfig.coflnetKatDataUrl).toURL().readText()
        val listType = object : TypeToken<List<KatRecipe>>() {}.type
        val recipes: List<KatRecipe> = gson.fromJson(jsonString, listType)

        recipes.forEach { recipe ->
            val rarityNum = rarityMap[recipe.baseRarity.uppercase()] ?: 0
            val namePart = recipe.itemTag.removePrefix(KatConfig.petPrefix).replace(KatConfig.space, KatConfig.underscore).uppercase()
            recipe.itemTag = "$namePart${KatConfig.itemTagSeparator}$rarityNum"
        }

        if (!jsonFile.parentFile.exists()) jsonFile.parentFile.mkdirs()
        jsonFile.writeText(gson.toJson(recipes))

        groupIntoFamilies(recipes)
    } catch (e: Exception) {
        emptyList()
    }

    private fun groupIntoFamilies(recipes: List<KatRecipe>) =
        recipes.groupBy { it.name }
            .map { (name, familyRecipes) ->
                KatFamily(
                    name = name,
                    recipes = familyRecipes.sortedBy { rarityMap[it.baseRarity.uppercase()] ?: 0 },
                    isFullFamily = familyRecipes.any { it.baseRarity.uppercase() == KatConfig.rarities.first() }
                )
            }
            .sortedByDescending { it.isFullFamily }
}