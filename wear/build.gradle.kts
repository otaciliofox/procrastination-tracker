import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Same keystore.properties the phone module reads, deliberately: see the note on the release
// build type below for why the two apps cannot be signed with different keys.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "com.foxlab.procrastinationtracker.watch"
    compileSdk = 34

    defaultConfig {
        // MUST match the phone app's applicationId. The Wearable Data Layer only delivers data
        // items between apps that share a package name *and* a signing key -- with two different
        // ids each app was writing into its own private namespace and neither ever saw the
        // other's payload, which is exactly why sync never worked on any device. The Kotlin
        // namespace above stays `.watch`; only the install id has to line up.
        applicationId = "com.foxlab.procrastinationtracker"
        minSdk = 30 // Wear OS 3+ (Galaxy Watch 4 and newer)
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
            // Resolves exactly like :app, and must. The Data Layer refuses to deliver between apps
            // signed with different keys, and the failure is silent -- sync simply never happens --
            // so the two modules must never drift onto separate keystores.
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

    // Round-screen Compose renders on the JVM for the screenshot tests, which needs the watch
    // app's own resources and theme available to the unit test source set.
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
    implementation(project(":trackerdata"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    // Icon vector set (androidx.compose.material.icons.Icons) -- not tied to Material2/3, works
    // fine alongside Wear Compose Material's own Icon composable. Needs the EXTENDED artifact,
    // not just -core: -core only ships a curated ~40-icon subset (ArrowBack, Add, MoreVert,
    // PlayArrow, etc.) and does NOT include Pause, History, or Settings, which is exactly the
    // "unresolved reference: Pause" build error this caused. The phone app already depends on
    // material-icons-extended for the same reason (see app/build.gradle.kts).
    implementation(libs.compose.material.icons.extended)

    // Compose for Wear OS (different artifact from phone Material3).
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear)
    // Starts an activity on the paired phone ("ver/editar no celular").
    implementation(libs.wear.remote.interactions)
    implementation(libs.play.services.wearable)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(composeBom)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}

// Same reason as the phone module: Roborazzi's Gradle plugin cannot load on AGP 9, and this flag
// is the only thing it would have set for us.
tasks.withType<Test>().configureEach {
    systemProperty("roborazzi.test.record", "true")
}
