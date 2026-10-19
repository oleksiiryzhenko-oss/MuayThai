import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Release signing is read from keystore.properties (or environment variables in CI).
// Without either, bundleRelease still produces an unsigned .aab you can sign later.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? =
    (keystoreProps.getProperty(key) ?: System.getenv(env))?.takeIf { it.isNotBlank() }

val cmKeystorePath = System.getenv("CM_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
val releaseStoreFile = cmKeystorePath ?: signingValue("storeFile", "MT_STORE_FILE")
val hasReleaseSigning = releaseStoreFile?.let { rootProject.file(it).exists() } == true
require(cmKeystorePath == null || hasReleaseSigning) { "Codemagic keystore file is unavailable" }

android {
    namespace = "com.muaythai.arena"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.muaythai.arena"
        minSdk = 24
        targetSdk = 36
        val appsFlyerKey = providers.environmentVariable("APPSFLYER_DEV_KEY").orElse("").get()
        require(appsFlyerKey.matches(Regex("[A-Za-z0-9_-]*")))
        buildConfigField("String", "APPSFLYER_DEV_KEY", "\"$appsFlyerKey\"")
        val handoffKey = providers.environmentVariable("AF_HANDOFF_CONFIG_KEY").orElse("p9d6c24a81f037be5a602").get()
        require(handoffKey.matches(Regex("[A-Za-z0-9_]+")))
        buildConfigField("String", "AF_HANDOFF_CONFIG_KEY", "\"$handoffKey\"")
        val returnHost = providers.gradleProperty("returnHost").orElse("not-configured.invalid").get()
        require(returnHost.matches(Regex("[A-Za-z0-9.-]+")))
        buildConfigField("String", "RETURN_HOST", "\"$returnHost\"")
        manifestPlaceholders["returnHost"] = returnHost
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile?.let { rootProject.file(it) }
                storePassword = if (cmKeystorePath != null) System.getenv("CM_KEYSTORE_PASSWORD") else signingValue("storePassword", "MT_STORE_PASSWORD")
                keyAlias = if (cmKeystorePath != null) System.getenv("CM_KEY_ALIAS") else signingValue("keyAlias", "MT_KEY_ALIAS")
                keyPassword = if (cmKeystorePath != null) System.getenv("CM_KEY_PASSWORD") else signingValue("keyPassword", "MT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        // java.time is used throughout; desugaring makes it available on Android 7.x (API 24-25).
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    lint { abortOnError = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation("com.appsflyer:af-android-sdk:6.17.1")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.webkit:webkit:1.14.0")
    testImplementation("org.json:json:20240303")
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    testImplementation("junit:junit:4.13.2")
}
