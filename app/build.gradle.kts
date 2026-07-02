import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

// Secrets are supplied at build time from a gitignored env file, never committed
// (constitution §2: "No secrets in source control").
val secretsFile = rootProject.file("config/secrets.env")
val secrets = Properties().apply {
  if (secretsFile.exists()) {
    secretsFile.reader().use { load(it) }
  }
}
val youtubeApiKey: String = (secrets.getProperty("YOUTUBE_API_KEY") ?: "").trim()
val authorizedEmails: String = (secrets.getProperty("AUTHORIZED_EMAILS") ?: "").trim()

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

        // Runtime-overridable via UI-test-mode intent extras (constitution §4); these are only
        // the production defaults used outside test mode.
        buildConfigField("String", "YOUTUBE_API_BASE_URL", "\"https://www.googleapis.com\"")
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
        buildConfigField("String", "AUTHORIZED_EMAILS", "\"$authorizedEmails\"")
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
        excludes += "/META-INF/DEPENDENCIES"
        excludes += "/META-INF/LICENSE*"
        excludes += "/META-INF/NOTICE*"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

// Source channels are read at runtime from bundled assets, never hardcoded in Kotlin
// (spec.md: "no catch-all endpoint" — the app must iterate config/channels.json).
val copyChannelsConfig by tasks.registering(Copy::class) {
  from(rootProject.file("config/channels.json"))
  into(layout.projectDirectory.dir("src/main/assets"))
}
tasks.named("preBuild") {
  dependsOn(copyChannelsConfig)
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
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.androidx.test.core)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.androidx.room.testing)

  // DI
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)

  // Persistence
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Network
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlinx.serialization.converter)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.kotlinx.serialization.json)

  // Map
  implementation(libs.osmdroid.android)

  // Images
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)

  // Auth
  implementation(libs.play.services.auth)
}
