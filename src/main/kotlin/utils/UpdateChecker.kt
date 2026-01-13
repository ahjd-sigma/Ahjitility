package utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class UpdateInfo(
    val version: String,
    val changelog: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean
)

object UpdateChecker {
    private val client = OkHttpClient()
    private val gson = Gson()
    private const val REPO_OWNER = "Sigma"
    private const val REPO_NAME = "Ahjitility"
    
    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Failed to check for updates: ${response.code}")
                    return@withContext UpdateInfo(GeneralConfig.VERSION, "", "", false)
                }

                val body = response.body?.string() ?: return@withContext UpdateInfo(GeneralConfig.VERSION, "", "", false)
                val json = gson.fromJson(body, JsonObject::class.java)
                
                val tagName = json.get("tag_name").asString
                val latestVersion = tagName.removePrefix("v")
                val currentVersion = GeneralConfig.VERSION
                
                if (isNewer(latestVersion, currentVersion)) {
                    val changelog = json.get("body").asString
                    val downloadUrl = json.get("zipball_url").asString
                    
                    return@withContext UpdateInfo(tagName, changelog, downloadUrl, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext UpdateInfo(GeneralConfig.VERSION, "", "", false)
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        
        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    suspend fun downloadAndInstallUpdate(downloadUrl: String, onProgress: (String) -> Unit) = withContext(Dispatchers.IO) {
        onProgress("Downloading update...")
        val request = Request.Builder().url(downloadUrl).build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Failed to download update")
                
                val zipFile = File("update.zip")
                val body = response.body ?: throw Exception("Empty response body")
                
                FileOutputStream(zipFile).use { output ->
                    body.byteStream().copyTo(output)
                }
                
                onProgress("Preparing update...")
                val stageDir = prepareUpdateStaging(zipFile)
                
                onProgress("Launching updater script...")
                createAndRunBatchScript(stageDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress("Update failed: ${e.message}")
            throw e // Re-throw to let UI know
        }
    }

    private fun prepareUpdateStaging(zipFile: File): File {
        // 1. Unzip to temp dir
        val tempDir = File("temp_update_raw")
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(tempDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile.mkdirs()
                    FileOutputStream(file).use { output ->
                        zis.copyTo(output)
                    }
                }
                entry = zis.nextEntry
            }
        }

        // 2. Find the 'src' directory inside the extracted structure
        // GitHub zips usually are: RepoName-Version/src/...
        val rootFolder = tempDir.listFiles()?.firstOrNull { it.isDirectory } ?: tempDir
        val sourceSrc = File(rootFolder, "src")
        
        if (!sourceSrc.exists()) {
            throw Exception("Invalid update format: 'src' directory not found in zip")
        }

        // 3. Move just the new 'src' to a clean staging folder
        val stagingDir = File("update_staging")
        if (stagingDir.exists()) stagingDir.deleteRecursively()
        stagingDir.mkdirs()
        
        sourceSrc.copyRecursively(File(stagingDir, "src"), overwrite = true)
        
        // Cleanup raw extract
        tempDir.deleteRecursively()
        zipFile.delete()
        
        return stagingDir
    }

    private fun createAndRunBatchScript(stagingDir: File) {
        val batchFile = File("update.bat")
        val script = """
            @echo off
            echo Waiting for application to close...
            timeout /t 3 /nobreak > NUL
            
            echo Removing old source code...
            rmdir /s /q src
            
            echo Installing new update...
            move /Y "${stagingDir.absolutePath}\src" src
            
            echo Cleaning up...
            rmdir /s /q "${stagingDir.absolutePath}"
            del update.bat
            
            echo Update Complete!
            echo You can now restart the application.
            pause
        """.trimIndent()
        
        batchFile.writeText(script)
        
        // Execute batch file detached
        Runtime.getRuntime().exec("cmd /c start update.bat")
    }
}
