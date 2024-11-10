import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.compose.internal.utils.getLocalProperty

@Suppress("DSL_SCOPE_VIOLATION") //https://github.com/gradle/gradle/issues/22797
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.atomicfu)
    id("kotlin-parcelize")
    id("maven-publish")
    id("signing")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.kotlin.native.cocoapods")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.16.3"
    id("com.google.cloud.artifactregistry.gradle-plugin") version "2.2.1"
}

version = "0.0.1-SNAPSHOT"

kotlin {
    withSourcesJar(publish = true)
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.jvmTarget.get()
                freeCompilerArgs += "-Xexpect-actual-classes"
            }
        }
        isSourcesPublishable = true
        publishLibraryVariants("release")
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.isSourcesPublishable = true
        it.binaries.framework {
            isStatic = true
        }
        it.binaries.staticLib {
            optimized = true
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.experimental.ExperimentalObjCName")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")

            }
        }
        applyDefaultHierarchyTemplate()
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.runtimeSaveable)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.maplibre.android)
                implementation(libs.android.plugin.annotation.v9)
                implementation(libs.kotlinx.serialization.json)
                implementation(compose.components.resources)
                implementation(compose.ui)
                implementation(libs.lifecycle.extensions)

            }
        }
        val androidUnitTest by getting {
            dependencies {
            }
        }

        val iosMain by getting {
            dependencies {
                api(compose.material)
            }
        }

        val iosTest by getting {
            dependencies {
            }
        }
        cocoapods {
            ios.deploymentTarget = "13.5"
            // make sure sample iOS target matches this version
            version = "6.7.0"
            specRepos {
                url("https://github.com/CocoaPods/Specs.git")
            }
            pod("MapLibre") {
                version = "6.7.0"
                moduleName = "MapLibre"
            }
        }
    }
}

android {
    namespace = "sk.amir.maps"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard.pro"
            )
        }
    }
}
mavenPublishing {
    coordinates(
        groupId = "com.composemap",
        artifactId = "composemap-maplibre",
        version = "0.0.1-SNAPSHOT"
    )
    pom {
        name = project.name
        val pom = this
        inceptionYear.set("2024")
        version = project.version as String
        project.afterEvaluate {
            // description seems to be only available after evaluation
            pom.description.set(project.description)
        }
        url.set("https://github.com/skamirmaps/skamirmaps")
        licenses {
            license {
                name.set("Mozilla Public License Version 2.0")
                url.set("https://www.mozilla.org/en-US/MPL/2.0/")
            }
        }
        developers {
            developer {
                id.set("amirhammad")
                name.set("Amir Hammad")
                email.set("hi@amir.sk")
            }
        }
        scm {
            connection.set("scm:git:github.com/skamirmaps/skamirmaps.git")
            developerConnection.set("scm:git:ssh://github.com/skamirmaps/skamirmaps.git")
            url.set("https://github.com/skamirmaps/skamirmaps/tree/main")
        }
    }
    val sonatypeHost = if ((project.version as String).endsWith("SNAPSHOT")) {
        SonatypeHost.S01
    } else {
        SonatypeHost.CENTRAL_PORTAL
    }
    publishToMavenCentral(sonatypeHost)
    signAllPublications()
}
