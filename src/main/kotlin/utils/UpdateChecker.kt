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
    val isUpdateAvailable: Boolean,
    val isSourceUpdate: Boolean = true
)

object UpdateChecker {
    private val client = OkHttpClient()
    private val gson = Gson()
    private const val REPO_OWNER = "ahjd-sigma"
    private const val REPO_NAME = "Ahjitility"
    
    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        val releasesUrl = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        val tagsUrl = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/tags"
        
        try {
            Log.debug(this, "Checking for releases at: $releasesUrl")
            val releaseInfo = fetchFromUrl(releasesUrl)
            if (releaseInfo != null) return@withContext releaseInfo

            Log.debug(this, "No official release found. Checking tags at: $tagsUrl")
            val tagInfo = fetchLatestTag(tagsUrl)
            if (tagInfo != null) return@withContext tagInfo

        } catch (e: Exception) {
            Log.debug(this, "Error during update check", e)
        }
        
        return@withContext UpdateInfo(GeneralConfig.VERSION, "", "", false)
    }

    private fun fetchFromUrl(url: String): UpdateInfo? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "Ahjitility-Launcher")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val json = gson.fromJson(body, JsonObject::class.java)
            
            val tagName = json.get("tag_name").asString
            val latestVersion = tagName.removePrefix("v").trim()
            val currentVersion = GeneralConfig.VERSION.trim()
            
            Log.debug(this, "Latest Release Found: $latestVersion")
            Log.debug(this, "Current Local Version: $currentVersion")
            
            if (isNewer(latestVersion, currentVersion)) {
                val changelog = json.get("body").asString
                
                // Decide between JAR and Source update
                val isSourceEnv = File("src").exists()
                val isRunningFromJar = UpdateChecker::class.java.protectionDomain.codeSource.location.path.lowercase().endsWith(".jar")
                val assets = json.getAsJsonArray("assets")
                
                // Look for a raw .jar asset
                val jarAsset = assets?.firstOrNull { 
                    it.asJsonObject.get("name").asString.lowercase().endsWith(".jar")
                }?.asJsonObject

                return if (isRunningFromJar && jarAsset != null) {
                    // If we are running from a jar, prioritize jar update if available
                    UpdateInfo(tagName, changelog, jarAsset.get("browser_download_url").asString, true, isSourceUpdate = false)
                } else if (!isSourceEnv && jarAsset != null) {
                    UpdateInfo(tagName, changelog, jarAsset.get("browser_download_url").asString, true, isSourceUpdate = false)
                } else {
                    UpdateInfo(tagName, changelog, json.get("zipball_url").asString, true, isSourceUpdate = true)
                }
            }
        }
        return null
    }

    private fun fetchLatestTag(url: String): UpdateInfo? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "Ahjitility-Launcher")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val jsonArray = gson.fromJson(body, com.google.gson.JsonArray::class.java)
            if (jsonArray.size() == 0) return null
            
            val latestTag = jsonArray.get(0).asJsonObject
            val tagName = latestTag.get("name").asString
            val latestVersion = tagName.removePrefix("v").trim()
            val currentVersion = GeneralConfig.VERSION.trim()
            
            Log.debug(this, "Latest Tag Found: $latestVersion")
            Log.debug(this, "Current Local Version: $currentVersion")
            
            if (isNewer(latestVersion, currentVersion)) {
                val isSourceEnv = File("src").exists()
                val downloadUrl = latestTag.get("zipball_url").asString
                
                // Tags only have source zip, so if we're in packaged mode, we can't update via just a tag
                if (!isSourceEnv) {
                    Log.debug(this, "Found newer tag, but no JAR asset available in a release. Skipping packaged update.")
                    return null
                }
                
                return UpdateInfo(tagName, "New tag found: $tagName (No official changelog provided)", downloadUrl, true, isSourceUpdate = true)
            }
        }
        return null
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

    suspend fun downloadAndInstallUpdate(updateInfo: UpdateInfo, onProgress: (String) -> Unit) = withContext(Dispatchers.IO) {
        onProgress("Downloading update...")
        val request = Request.Builder().url(updateInfo.downloadUrl).build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Failed to download update")
                
                val body = response.body ?: throw Exception("Empty response body")
                
                if (updateInfo.isSourceUpdate) {
                    val zipFile = File("update.zip")
                    FileOutputStream(zipFile).use { output -> body.byteStream().copyTo(output) }
                    onProgress("Preparing update...")
                    val stageDir = prepareUpdateStaging(zipFile)
                    onProgress("Launching updater script...")
                    createAndRunBatchScript(stageDir, isSource = true)
                } else {
                    // Packaged Update: Pull the raw JAR directly
                    val currentJarFile = File(UpdateChecker::class.java.protectionDomain.codeSource.location.toURI())
                    val currentJarPath = currentJarFile.absolutePath
                    val newJar = File("update_new.jar")
                    
                    // Try to find the EXE launcher (jpackage structure: Ahjitility.exe is parent of 'app' folder)
                    val appDir = currentJarFile.parentFile
                    val rootDir = appDir?.parentFile
                    val launcherExe = if (rootDir != null && appDir.name == "app") {
                        rootDir.listFiles()?.firstOrNull { it.name.endsWith(".exe") && !it.name.contains("update") }
                    } else null

                    onProgress("Downloading JAR...")
                    FileOutputStream(newJar).use { output -> body.byteStream().copyTo(output) }
                    
                    onProgress("Launching updater script...")
                    createAndRunBatchScript(newJar, isSource = false, targetPath = currentJarPath, launcherPath = launcherExe?.absolutePath ?: "")
                }
            }
        } catch (e: Exception) {
            Log.debug("UpdateChecker", "Download and install failed", e)
            onProgress("Update failed: ${e.message}")
            throw e
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

    private fun createAndRunBatchScript(updateFile: File, isSource: Boolean, targetPath: String = "", launcherPath: String = "") {
        val batchFile = File("update.bat")
        val pid = ProcessHandle.current().pid()
        
        // Use a self-deleting exit strategy for the batch file
        val selfDeleteExit = "(goto) 2>nul & del \"%~f0\" & exit"
        
        // Common wait logic using PID (works for both JAR and EXE)
        val waitLogic = """
            echo Waiting for application (PID: $pid) to close...
            :wait_pid
            tasklist /FI "PID eq $pid" 2>NUL | find /I /N "$pid">NUL
            if "%ERRORLEVEL%"=="0" (
                timeout /t 1 /nobreak > NUL
                goto wait_pid
            )
        """.trimIndent()

        // Define the core update actions based on type
        val updateAction = if (isSource) {
            """
                echo Removing old source code...
                :retry_rmdir
                if exist src (
                    rmdir /s /q src
                    if exist src (
                        echo Failed to remove src directory, retrying in 2 seconds...
                        timeout /t 2 /nobreak > NUL
                        goto retry_rmdir
                    )
                )
                
                echo Installing new update...
                :retry_move
                move /Y "${updateFile.absolutePath}\src" src
                if errorlevel 1 (
                    echo Access denied or file locked, retrying in 2 seconds...
                    timeout /t 2 /nobreak > NUL
                    goto retry_move
                )
                
                echo Cleaning up...
                rmdir /s /q "${updateFile.absolutePath}"
                
                echo Update Complete!
                echo You can now restart the application.
                pause
            """.trimIndent()
        } else {
            val restartCommand = if (launcherPath.isNotEmpty()) {
                "start \"\" \"$launcherPath\""
            } else {
                "start \"\" \"$targetPath\""
            }
            
            """
                echo Replacing application file...
                :retry
                move /Y "${updateFile.absolutePath}" "$targetPath"
                if errorlevel 1 (
                    echo File is still locked, retrying in 2 seconds...
                    timeout /t 2 /nobreak > NUL
                    goto retry
                )
                
                echo Update Complete!
                echo Restarting...
                $restartCommand
            """.trimIndent()
        }

        val script = """
            @echo off
            title Ahjitility Updater
            $waitLogic
            
            $updateAction
            $selfDeleteExit
        """.trimIndent()
        
        batchFile.writeText(script)
        
        val absoluteBatchPath = batchFile.absolutePath
        Log.debug(this, "Launching updater at: $absoluteBatchPath")
        
        try {
            // Use 'explorer.exe' to launch the batch file. 
            ProcessBuilder("explorer.exe", absoluteBatchPath).start()
            
            // Give the OS a moment to fully spawn the detached process
            Thread.sleep(1000)
        } catch (e: Exception) {
            Log.debug(this, "Failed to launch batch via Explorer", e)
            // Fallback to basic start
            try {
                 ProcessBuilder("cmd", "/c", "start", "Ahjitility Updater", "cmd", "/c", "\"$absoluteBatchPath\"").start()
            } catch (e2: Exception) {
                 Log.debug(this, "Critical failure launching batch", e2)
            }
        }
    }
}
