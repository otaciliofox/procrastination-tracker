plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.foxlab.procrastinationtracker.trackerdata"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Robolectric runs the real Room database and the real DataMap codec on the JVM, so the sync
    // tests need Android resources available to a plain `test` source set -- no device, no emulator.
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

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    // `api`, not `implementation`: TrackerDatabase is part of this module's public surface and it
    // extends RoomDatabase, so consumers need that type on their compile classpath. With Hilt in
    // the picture this stopped being theoretical -- the generated provider factory failed to
    // compile in :wear with "class file for androidx.room.RoomDatabase not found".
    api(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Wearable Data Layer API is used by the repository's sync-merge helpers.
    implementation(libs.play.services.wearable)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
}
