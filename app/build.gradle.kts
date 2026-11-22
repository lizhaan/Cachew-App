import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing: the key and its passwords live outside git, in keystore.properties next to
// the project (see keystore.properties.example). Without it, release builds stop with a hint.
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val hasReleaseKey = keystoreProps.getProperty("storeFile") != null

// Bump BOTH before every new APK, or phones won't install it over the old one.
val cachewVersionCode = 3
val cachewVersionName = "1.1"

android {
    namespace = "app.cachew"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.cachew"
        minSdk = 26
        targetSdk = 37
        versionCode = cachewVersionCode
        versionName = cachewVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
            // R8 stays off: the spec asks not to minify (Accessibility service, Share, WorkManager).
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true // BuildConfig.DEBUG decides whether Chrome is offered as a test shop
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx) // reminders
    implementation(libs.coil.compose)              // product photos
    implementation(libs.jsoup)                     // product page parsing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// gradlew.bat cachewApk → builds the signed release APK and copies it to release/Cachew-<version>.apk
// (the file people download). AGP 9 has no outputFileName any more, so it's a plain copy.
tasks.register<Copy>("cachewApk") {
    dependsOn("assembleRelease")
    // Plain local copy: the configuration cache can't store a reference to the build script itself.
    val keyPresent = hasReleaseKey
    val apkName = "Cachew-$cachewVersionName.apk"
    doFirst {
        check(keyPresent) { "No keystore.properties: create the release key first (README, section about releases)." }
    }
    from(layout.buildDirectory.dir("outputs/apk/release")) { include("*-release.apk") }
    rename { apkName }
    into(rootProject.layout.projectDirectory.dir("release"))
}
