plugins {
    kotlin("jvm") version "2.2.20"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.sigma"
version = "1.1.0"

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
        "--app-version", "1.1.0",
        "--icon", project.projectDir.resolve("resources/icons/kotlin.ico").absolutePath,
        "--java-options", "--add-opens javafx.graphics/javafx.scene=ALL-UNNAMED --add-opens javafx.controls/javafx.scene.control=ALL-UNNAMED"
    )

    doLast {
        val appDir = File(outputDir, "Ahjitility")
        val configSource = project.projectDir.resolve("config")
        if (configSource.exists()) {
            configSource.copyRecursively(File(appDir, "config"), overwrite = true)
        }
        // No longer printing here, will print in final package task
    }
}

val packageRelease = tasks.register<Zip>("packageRelease") {
    dependsOn("shadowJar", createPortable)
    group = "distribution"
    description = "Packages both JAR and EXE versions into a single ZIP"

    val releaseDir = layout.buildDirectory.dir("release-staging").get().asFile
    
    // Set zip properties
    archiveFileName.set("Ahjitility-v${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))

    // Define what goes into the ZIP
    from(layout.buildDirectory.dir("dist/portable/Ahjitility")) {
        into("Windows-Portable-EXE")
    }
    
    // Add Standalone JAR version
    from(layout.buildDirectory.file("libs/Ahjitility.jar")) {
        into("Standalone-JAR")
    }
    
    // Add config and run.bat to Standalone JAR folder
    from(project.projectDir.resolve("config")) {
        into("Standalone-JAR/config")
    }

    doFirst {
        if (releaseDir.exists()) releaseDir.deleteRecursively()
        releaseDir.mkdirs()

        // Generate run.bat for the Standalone JAR folder inside the zip
        val runBatFile = File(releaseDir, "run.bat")
        runBatFile.writeText("""
            @echo off
            echo Starting Ahjitility (Standalone JAR version)...
            java -jar Ahjitility.jar
            if %ERRORLEVEL% neq 0 (
                echo.
                echo [ERROR] Application crashed or failed to start.
                echo Make sure you have Java 21+ installed.
                pause
            )
        """.trimIndent())
    }

    from(releaseDir) {
        into("Standalone-JAR")
    }

    doLast {
        println("\n============================================================")
        println("  BUILD SUCCESSFUL - RELEASE PACKAGED")
        println("============================================================")
        println("  Final Release ZIP: ${archiveFile.get().asFile.absolutePath}")
        println("  Contains:")
        println("    - /Windows-Portable-EXE (Full bundle, no Java needed)")
        println("    - /Standalone-JAR (Small file, requires Java 21 installed)")
        println("============================================================\n")
    }
}