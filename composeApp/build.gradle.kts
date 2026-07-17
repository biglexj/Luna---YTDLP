import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val appVersion = providers.gradleProperty("lunafetch.versionName").get()
val appVersionCode = providers.gradleProperty("lunafetch.versionCode").get().toInt()
val signingProperties = rootProject.file("keystore.properties")
    .takeIf { it.isFile }
    ?.readLines()
    ?.mapNotNull { line ->
        line.takeUnless { it.isBlank() || it.trimStart().startsWith("#") }
            ?.split('=', limit = 2)
            ?.takeIf { it.size == 2 }
            ?.let { it[0].trim() to it[1].trim() }
    }
    ?.toMap()
    .orEmpty()

fun signingValue(propertyName: String, environmentName: String): String? =
    signingProperties[propertyName]?.takeIf(String::isNotBlank)
        ?: System.getenv(environmentName)?.takeIf(String::isNotBlank)

val releaseStorePath = signingValue("storeFile", "LUNAFETCH_ANDROID_KEYSTORE")
val releaseStorePassword = signingValue("storePassword", "LUNAFETCH_ANDROID_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "LUNAFETCH_ANDROID_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "LUNAFETCH_ANDROID_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
                implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
                implementation("org.jetbrains.compose.material3:material3:1.11.0-alpha07")
                implementation("org.jetbrains.compose.ui:ui:1.11.1")
                implementation("org.jetbrains.compose.components:components-resources:1.11.1")
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor3)
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
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.youtubedl.android.library)
                implementation(libs.youtubedl.android.ffmpeg)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.cio)
            }
        }
    }
}

android {
    namespace = "com.biglexj.lunafetch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.biglexj.lunafetch"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs.useLegacyPackaging = true
        resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")
    }

    val permanentReleaseSigning = if (hasReleaseSigning) {
        signingConfigs.create("permanentRelease") {
            storeFile = rootProject.file(requireNotNull(releaseStorePath))
            storePassword = requireNotNull(releaseStorePassword)
            keyAlias = requireNotNull(releaseKeyAlias)
            keyPassword = requireNotNull(releaseKeyPassword)
        }
    } else {
        null
    }

    buildTypes.getByName("release") {
        signingConfig = permanentReleaseSigning
        isMinifyEnabled = false
    }
}

compose.desktop {
    application {
        mainClass = "com.biglexj.lunafetch.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "LunaFetch"
            packageVersion = appVersion
            vendor = "biglexj"
            description = "Descarga videos y audio con una interfaz multiplataforma"

            windows {
                iconFile.set(rootProject.file("icon/icon.ico"))
                msiPackageVersion = appVersion
                exePackageVersion = appVersion
                upgradeUuid = "ea999172-1299-4930-93c9-90ea30707c17"
                shortcut = true
                menu = true
                menuGroup = "Luna Fetch"
                dirChooser = true
                perUserInstall = true
            }

            linux {
                shortcut = true
                menuGroup = "Utility"
                appCategory = "AudioVideo"
            }
        }
    }
}
