plugins {
    kotlin("jvm") version "2.2.20"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.sigma"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")
    implementation("org.yaml:snakeyaml:2.2")
    
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

application {
    mainClass = "app.MainKt"
    applicationDefaultJvmArgs = listOf(
        "--add-opens", "javafx.graphics/javafx.scene=ALL-UNNAMED",
        "--add-opens", "javafx.controls/javafx.scene.control=ALL-UNNAMED"
    )
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveFileName.set("Ahjitility.jar")
    mergeServiceFiles()
}

val createPortable = tasks.register<Exec>("createPortable") {
    dependsOn("shadowJar")
    group = "distribution"
    description = "Creates a portable Windows distribution (folder with EXE and JRE)"

    val outputDir = layout.buildDirectory.dir("dist/portable").get().asFile
    
    // Create a temporary directory containing ONLY the shadow JAR for jpackage
    val jpackageInputDir = layout.buildDirectory.dir("jpackage-input").get().asFile

    doFirst {
        if (outputDir.exists()) outputDir.deleteRecursively()
        outputDir.mkdirs()

        if (jpackageInputDir.exists()) jpackageInputDir.deleteRecursively()
        jpackageInputDir.mkdirs()
        
        // Only copy the shadow JAR to the jpackage input directory
        val shadowJarFile = layout.buildDirectory.file("libs/Ahjitility.jar").get().asFile
        shadowJarFile.copyTo(File(jpackageInputDir, "Ahjitility.jar"))
    }

    commandLine(
        "jpackage",
        "--type", "app-image",
        "--dest", outputDir.absolutePath,
        "--input", jpackageInputDir.absolutePath,
        "--main-jar", "Ahjitility.jar",
        "--main-class", "app.MainKt",
        "--name", "Ahjitility",
        "--vendor", "Sigma",
        "--app-version", "1.0.0",
        "--icon", project.projectDir.resolve("resources/icons/kotlin.ico").absolutePath,
        "--java-options", "--add-opens javafx.graphics/javafx.scene=ALL-UNNAMED --add-opens javafx.controls/javafx.scene.control=ALL-UNNAMED"
    )

    doLast {
        val appDir = File(outputDir, "Ahjitility")
        val configSource = project.projectDir.resolve("config")
        if (configSource.exists()) {
            configSource.copyRecursively(File(appDir, "config"), overwrite = true)
        }
        println("Portable distribution created in: ${appDir.absolutePath}")
    }
}

val zipPortable = tasks.register<Zip>("zipPortable") {
    dependsOn(createPortable)
    group = "distribution"
    description = "Zips the portable distribution for sharing"

    from(layout.buildDirectory.dir("dist/portable/Ahjitility"))
    archiveFileName.set("Ahjitility-Windows-Portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist/ready-to-ship"))

    doLast {
        println("Portable ZIP created in: ${archiveFile.get().asFile.absolutePath}")
    }
}

val packageApp = tasks.register("packageApp") {
    dependsOn("shadowJar", zipPortable)
    doLast {
        val distDir = layout.buildDirectory.dir("dist").get().asFile
        val standaloneDir = File(distDir, "standalone-jar-version")
        val portableDir = layout.buildDirectory.dir("dist/portable/Ahjitility").get().asFile
        val shipDir = layout.buildDirectory.dir("dist/ready-to-ship").get().asFile
        
        if (standaloneDir.exists()) standaloneDir.deleteRecursively()
        standaloneDir.mkdirs()
        
        // Copy JAR to standalone
        val jarFile = layout.buildDirectory.file("libs/Ahjitility.jar").get().asFile
        jarFile.copyTo(File(standaloneDir, "Ahjitility.jar"), overwrite = true)
        
        // Copy config to standalone
        val configSource = project.projectDir.resolve("config")
        if (configSource.exists()) {
            configSource.copyRecursively(File(standaloneDir, "config"), overwrite = true)
        }

        // Create run.bat in standalone with necessary JVM arguments for JavaFX reflection
        val runBat = File(standaloneDir, "run.bat")
        val jvmArgs = application.applicationDefaultJvmArgs.joinToString(" ")
        runBat.writeText("""
            @echo off
            echo Starting Ahjitility (Standalone JAR version)...
            java $jvmArgs -jar Ahjitility.jar
            if %ERRORLEVEL% neq 0 (
                echo.
                echo [ERROR] Application crashed or failed to start.
                pause
            )
        """.trimIndent())
        
        println("\n============================================================")
        println("  BUILD SUCCESSFUL - OUTPUTS REORGANIZED")
        println("============================================================")
        println("  1. FOR TESTING (Folder):")
        println("     - EXE Version: ${portableDir.absolutePath}")
        println("     - JAR Version: ${standaloneDir.absolutePath}")
        println("")
        println("  2. FOR SHARING (Single File):")
        println("     - ZIP Archive: ${shipDir.absolutePath}\\Ahjitility-Windows-Portable.zip")
        println("============================================================\n")
    }
}