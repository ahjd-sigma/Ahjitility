plugins {
    kotlin("jvm") version "2.2.20"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.sigma"
val appVersion = file("src/main/resources/version.txt").readText().trim()
version = appVersion

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
        "--app-version", appVersion,
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

val packageRelease = tasks.register("packageRelease") {
     dependsOn("shadowJar", createPortable)
     group = "distribution"
     description = "Packages the application into assets for GitHub Releases"
 
     val releasesDir = layout.buildDirectory.dir("releases").get().asFile
 
     doLast {
         if (releasesDir.exists()) releasesDir.deleteRecursively()
         releasesDir.mkdirs()
 
         // 1. Create the Portable ZIP (contains app, runtime, exe)
         val portableZip = File(releasesDir, "Ahjitility-v${project.version}-portable-windows.zip")
         val portableSource = layout.buildDirectory.dir("dist/portable/Ahjitility").get().asFile
         
         ant.withGroovyBuilder {
             "zip"("destfile" to portableZip, "basedir" to portableSource.parentFile, "includes" to "${portableSource.name}/**")
         }
 
         // 2. Copy the Standalone JAR directly (no ZIP)
         val shadowJarFile = layout.buildDirectory.file("libs/Ahjitility.jar").get().asFile
         val releaseJar = File(releasesDir, "Ahjitility-v${project.version}.jar")
         shadowJarFile.copyTo(releaseJar, overwrite = true)
 
         println("\n============================================================")
         println("  BUILD SUCCESSFUL - RELEASE ASSETS PREPARED")
         println("============================================================")
         println("  Assets in: ${releasesDir.absolutePath}")
         println("    1. ${portableZip.name} (Portable EXE Bundle ZIP)")
         println("    2. ${releaseJar.name} (Standalone Raw JAR)")
         println("============================================================\n")
     }
 }