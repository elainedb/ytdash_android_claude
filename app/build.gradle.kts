import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// Build-time secrets/config injected from a gitignored file (constitution §2 — the API key and the
// authorized-email list are NEVER committed to source). A missing file yields empty defaults so CI
// can build without secrets; at runtime UI-test-mode extras override both anyway.
val secrets = Properties().apply {
    val f = rootProject.file("config/secrets.env")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String): String = (secrets.getProperty(key) ?: "").trim()

android {
    namespace = "com.example.ytdash"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.ytdash"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "YOUTUBE_API_KEY", "\"${secret("YOUTUBE_API_KEY")}\"")
        buildConfigField("String", "AUTHORIZED_EMAILS", "\"${secret("AUTHORIZED_EMAILS")}\"")
    }

    buildTypes {
        release {
            // Debug-signed so the release APK is runnable under Maestro without a keystore.
            signingConfig = signingConfigs.getByName("debug")
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
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

// Keep the bundled channel config in sync with the workspace config (no hardcoding of channels in
// source). If config/channels.json changes and the app is rebuilt, the asset is refreshed.
val syncChannels by tasks.registering(Copy::class) {
    val src = rootProject.file("config/channels.json")
    onlyIf { src.exists() }
    from(src)
    into(layout.projectDirectory.dir("src/main/assets"))
}
tasks.named("preBuild") { dependsOn(syncChannels) }

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  debugImplementation(libs.androidx.compose.ui.tooling)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Coroutines
  implementation(libs.kotlinx.coroutines.android)

  // Networking + JSON
  implementation(libs.okhttp)
  implementation(libs.kotlinx.serialization.json)

  // Image loading
  implementation(libs.coil.compose)

  // Map (OpenStreetMap)
  implementation(libs.osmdroid.android)

  // Real-mode Google Sign-In
  implementation(libs.play.services.auth)

  // Local tests
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
