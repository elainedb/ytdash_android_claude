import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// Load the real API key from config/secrets.env if present (gitignored); otherwise a dummy.
// Never commit the real key — it is read at RUNTIME from the `apiKey` launch extra for the harness,
// and only falls back to this build-time value for production use.
val secretsApiKey: String = run {
  val f = rootProject.file("config/secrets.env")
  if (f.exists()) {
    val p = Properties().apply { f.inputStream().use { load(it) } }
    p.getProperty("YOUTUBE_API_KEY") ?: "DUMMY_API_KEY"
  } else "DUMMY_API_KEY"
}

// Copy the source-channel config into assets so the app reads the same list the harness configures.
val syncChannels = tasks.register<Copy>("syncChannelsConfig") {
  from(rootProject.file("config/channels.json"))
  into(layout.projectDirectory.dir("src/main/assets"))
}
tasks.named("preBuild") { dependsOn(syncChannels) }

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

        buildConfigField("String", "YOUTUBE_API_KEY", "\"$secretsApiKey\"")
        // Default production base URL (host root; the app appends /youtube/v3/...). Overridable at
        // runtime via the apiBaseUrl launch extra (constitution §4).
        buildConfigField("String", "DEFAULT_API_BASE_URL", "\"https://www.googleapis.com\"")
    }

    buildTypes {
        release {
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

  // Coroutines
  implementation(libs.kotlinx.coroutines.android)

  // Networking + JSON
  implementation(libs.okhttp)
  implementation(libs.kotlinx.serialization.json)

  // Image loading
  implementation(libs.coil.compose)

  // Map (OpenStreetMap)
  implementation(libs.osmdroid.android)

  // Real Google Sign-In (production path)
  implementation(libs.play.services.auth)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
