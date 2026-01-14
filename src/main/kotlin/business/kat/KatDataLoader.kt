package business.kat

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import utils.KatConfig
import utils.Log
import java.io.File
import java.net.URI

object KatDataLoader {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val jsonFile get() = File(KatConfig.katJsonPath)

    private val rarityMap get() = KatConfig.rarityNumbers

    fun load(): List<KatFamily> {
        if (!jsonFile.exists()) {
            Log.debug(this, "Local data file missing, fetching from remote...")
            return fetchAndSave()
        }

        return try {
            val recipes: List<KatRecipe> = gson.fromJson(jsonFile.readText(), object : TypeToken<List<KatRecipe>>() {}.type)
            Log.debug(this, "Loaded ${recipes.size} recipes from local file")
            val modifiedRecipes = injectMissingMaterials(recipes)
            groupIntoFamilies(modifiedRecipes)
        } catch (e: Exception) {
            Log.debug(this, "Failed to load local data, falling back to remote: ${e.message}")
            fetchAndSave()
        }
    }

    private fun injectMissingMaterials(recipes: List<KatRecipe>): List<KatRecipe> {
        Log.debug(this, "Injecting missing materials (Crow, etc.)")
        return recipes.map { recipe ->
            if (recipe.name.contains("Crow", ignoreCase = true)) {
                val newMaterials = recipe.materials.toMutableMap()
                when (recipe.baseRarity.uppercase()) {
                    "UNCOMMON" -> newMaterials["VEILSHROOM"] = 2
                    "RARE" -> newMaterials["DUSKBLOOM"] = 4
                    "EPIC" -> newMaterials["DO_NOT_EAT_SHROOM"] = 8
                }
                recipe.copy(materials = newMaterials)
            } else {
                recipe
            }
        }
    }

    private fun fetchAndSave(): List<KatFamily> = try {
        Log.debug(this, "Fetching recipes from ${KatConfig.coflnetKatDataUrl}")
        val jsonString = URI.create(KatConfig.coflnetKatDataUrl).toURL().readText()
        val recipes: List<KatRecipe> = gson.fromJson(jsonString, object : TypeToken<List<KatRecipe>>() {}.type)
        Log.debug(this, "Successfully fetched ${recipes.size} recipes")

        recipes.forEach { recipe ->
            val rarityNum = rarityMap[recipe.baseRarity.uppercase()] ?: 0
            val namePart = recipe.itemTag.removePrefix(KatConfig.petPrefix)
                .replace(KatConfig.space, KatConfig.underscore).uppercase()
            recipe.itemTag = "$namePart${KatConfig.itemTagSeparator}$rarityNum"
        }

        val modifiedRecipes = injectMissingMaterials(recipes)

        if (!jsonFile.parentFile.exists()) jsonFile.parentFile.mkdirs()
        jsonFile.writeText(gson.toJson(modifiedRecipes))
        Log.debug(this, "Saved modified recipes to ${jsonFile.absolutePath}")

        groupIntoFamilies(modifiedRecipes)
    } catch (e: Exception) {
        Log.debug(this, "Critical failure during fetch and save", e)
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
