import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
}

// Signing credentials live in keystore.properties at the repo root: git-ignored, never committed,
// never part of the build script itself. The file is optional on purpose -- without it the project
// still builds (CI only ever assembles debug), it just produces an unsigned release.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "com.foxlab.procrastinationtracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.foxlab.procrastinationtracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Falls back to the debug key when no keystore.properties is present, so a release
            // build is always installable without any secret being committed or typed. That is
            // enough for sideloading; publishing to Play needs a real key, at which point adding
            // keystore.properties switches this over with no further change.
            //
            // The watch app resolves the same way, and must: the Wearable Data Layer only delivers
            // between apps sharing an applicationId *and* a signature, so two different release
            // keys would silently kill phone/watch sync.
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Screenshot tests render real Compose screens, so the unit test JVM needs the app's
    // resources -- strings, themes and drawables all take part in what ends up in the PNG.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Roborazzi ships a Gradle plugin, but it still reads AGP's removed `TestedExtension` and so
// cannot be applied on AGP 9. The plugin only exists to wire record/verify tasks and set this
// flag -- `captureRoboImage` writes the PNG on its own once the flag is on -- so setting it here
// keeps the screenshots working and leaves one command (`test`) running the whole suite.
tasks.withType<Test>().configureEach {
    systemProperty("roborazzi.test.record", "true")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":trackerdata"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Wearable Data Layer API - lets the phone receive synced sessions from the watch.
    implementation(libs.play.services.wearable)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(composeBom)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
