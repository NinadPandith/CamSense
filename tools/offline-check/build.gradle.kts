// Compiles the whole app against the real Android framework API (Robolectric's
// android-all), JetBrains Compose for desktop, OpenCV and TFLite, with thin stubs
// (stubs/) for libraries only on Google Maven, then runs the JVM unit tests.
// For machines that can't reach Google Maven; Android Studio doesn't need this.
plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.compose") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
}
val app = rootDir.resolve("../../app/src")
sourceSets {
    main { kotlin.setSrcDirs(listOf(app.resolve("main/java"), file("stubs"), layout.buildDirectory.dir("generated/r"))) }
    test { kotlin.setSrcDirs(listOf(app.resolve("test/java"))) }
}
val cmp = "1.7.3" // later versions redirect to Google Maven artifacts
dependencies {
    compileOnly(files("libs/android-all.jar", "libs/opencv.jar", "libs/tflite.jar", "libs/tflite-api.jar"))
    implementation("org.jetbrains.compose.runtime:runtime-desktop:$cmp")
    implementation("org.jetbrains.compose.ui:ui-desktop:$cmp")
    implementation("org.jetbrains.compose.foundation:foundation-desktop:$cmp")
    implementation("org.jetbrains.compose.material3:material3-desktop:$cmp")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:$cmp")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
configurations.all {
    exclude(group = "androidx.annotation")
    exclude(group = "androidx.collection")
    exclude(group = "androidx.lifecycle")
    exclude(group = "androidx.savedstate")
    exclude(group = "org.jetbrains.androidx.lifecycle")
    exclude(group = "org.jetbrains.androidx.savedstate")
}
kotlin { jvmToolchain(21) }
tasks.test {
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

// R and BuildConfig stand-ins, generated from res/values/strings.xml.
val generateR by tasks.registering {
    val strings = app.resolve("main/res/values/strings.xml")
    val out = layout.buildDirectory.file("generated/r/R.kt")
    inputs.file(strings)
    outputs.file(out)
    doLast {
        val names = Regex("<string name=\"([a-z0-9_]+)\"").findAll(strings.readText()).map { it.groupValues[1] }.toList()
        val plurals = Regex("<plurals name=\"([a-z0-9_]+)\"").findAll(strings.readText()).map { it.groupValues[1] }.toList()
        val fonts = app.resolve("main/res/font").listFiles().orEmpty().map { it.nameWithoutExtension }
        val drawables = app.resolve("main/res/drawable").listFiles().orEmpty().map { it.nameWithoutExtension }
        fun block(name: String, ids: List<String>, base: Int) =
            "    object $name {\n" + ids.mapIndexed { i, n -> "        const val $n = ${base + i}\n" }.joinToString("") + "    }\n"
        out.get().asFile.apply { parentFile.mkdirs() }.writeText(
            "package com.camsense\n\nobject R {\n" + block("string", names, 1) + block("plurals", plurals, 4000) + block("font", fonts, 5000) +
                block("drawable", drawables, 6000) + "}\n\nobject BuildConfig {\n    const val DEBUG = true\n" +
                "    const val VERSION_NAME = \"1.0.0\"\n}\n"
        )
    }
}
tasks.named("compileKotlin") { dependsOn(generateR) }
