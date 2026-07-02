import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

// --- Secrets (constitution §2: no secrets in source control) -------------------------------
// config/secrets.env is gitignored. Read it here at build time only; never write its value back
// into a committed file. Falls back to an empty string if the file is absent (e.g. CI) so the
// build never fails for missing secrets — the app still works via the uiTestMode `apiKey` extra.
val secretsEnvFile = rootProject.file("config/secrets.env")
val secretsProps = Properties().apply {
  if (secretsEnvFile.exists()) {
    secretsEnvFile.forEachLine { line ->
      val trimmed = line.trim()
      if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
        val (k, v) = trimmed.split("=", limit = 2)
        setProperty(k.trim(), v.trim())
      }
    }
  }
}
val youTubeApiKey: String = secretsProps.getProperty("YOUTUBE_API_KEY", "")
// Optional: an OAuth Web client id, needed for real (non-uiTestMode) Google Sign-In via Credential
// Manager. Not present in this workspace's config/secrets.env (no google-services.json either —
// see BUILD-REPORT.md); real sign-in surfaces a clear error rather than crashing when absent.
val googleServerClientId: String = secretsProps.getProperty("GOOGLE_SERVER_CLIENT_ID", "")

// --- Channel config sync ---------------------------------------------------------------------
// config/channels.json (repo root) is the single source of truth for source channels. Copy it
// into assets so the app reads it at RUNTIME (never hardcode channel ids in Kotlin).
val copyChannelsConfig = tasks.register<Copy>("copyChannelsConfig") {
  from(rootProject.file("config/channels.json"))
  into(layout.projectDirectory.dir("src/main/assets"))
}
tasks.named("preBuild") { dependsOn(copyChannelsConfig) }

android {
    namespace = "com.example.ytdash"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.ytdash"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youTubeApiKey\"")
        buildConfigField("String", "DEFAULT_API_BASE_URL", "\"https://www.googleapis.com\"")
        // Real-mode whitelist (constitution run config); overridable at runtime via the
        // `authorizedEmails` uiTestMode extra.
        buildConfigField(
            "String",
            "DEFAULT_AUTHORIZED_EMAILS",
            "\"elaine.batista1105@gmail.com,edbpmc@gmail.com\""
        )
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
    }

    buildTypes {
        debug {
            // Physical/emulator devices under Maestro talk to a plaintext local mock server.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
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
        excludes += "/META-INF/DEPENDENCIES"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

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
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // DI — Hilt
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)

  // Persistence — Room
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Networking — Retrofit + OkHttp + kotlinx.serialization
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.kotlinx.serialization)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.kotlinx.serialization.json)

  // Images
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)

  // Map
  implementation(libs.osmdroid.android)

  // Auth — Credential Manager + Google ID
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
  implementation(libs.play.services.auth)
}
