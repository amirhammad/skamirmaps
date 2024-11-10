
@Suppress("DSL_SCOPE_VIOLATION") //https://github.com/gradle/gradle/issues/22797
plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.library").version(libs.versions.android).apply(false)
    kotlin("multiplatform").version(libs.versions.kotlin).apply(false)
    id("com.android.application") version libs.versions.android apply false
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin apply false
    alias(libs.plugins.jetbrainsCompose) apply false

    // Existing plugins
    alias(libs.plugins.compose.compiler) apply false
    id("org.jetbrains.kotlin.native.cocoapods") version libs.versions.kotlin apply false
    id("project-report")
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

allprojects {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
