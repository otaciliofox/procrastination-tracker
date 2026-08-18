// Top-level build file. Individual module build files apply the plugins they need.
// Versions live in gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
