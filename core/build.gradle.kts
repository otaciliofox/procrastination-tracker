plugins {
    id("org.jetbrains.kotlin.jvm")
}

// Pure Kotlin module (no Android dependency) so it can be shared byte-for-byte
// between the phone app and the Wear OS app.
dependencies {
    testImplementation(kotlin("test"))
}

// Using explicit compiler-target flags instead of kotlin.jvmToolchain(17): the toolchain
// approach makes Gradle search for (or try to download) a registered JDK 17 toolchain, which
// fails on machines without one and without toolchain auto-provisioning enabled. This just
// targets bytecode 17 with whatever JDK is already running the build (works fine from JDK 17
// up to 21+, since compilers can target older bytecode than they run on).
//
// Both blocks are needed: the `kotlin("jvm")` plugin auto-applies the Java plugin (so a
// `compileJava` task exists even with zero .java files), and Kotlin's compiler refuses to run
// if compileJava's target and compileKotlin's target disagree -- which they will unless we pin
// both explicitly, since compileJava otherwise defaults to whatever JDK is running Gradle.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
