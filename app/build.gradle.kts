import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Read a production API key from a gitignored secrets file or gradle property, never committed.
// Falls back to empty: the UI-test harness supplies `apiKey` at runtime via launch extras.
val secretsKey: String = run {
    val fromProp = (project.findProperty("YOUTUBE_API_KEY") as String?)
    val fromEnv = System.getenv("YOUTUBE_API_KEY")
    val fromFile = rootProject.file("config/secrets.env").let { f ->
        if (f.exists()) {
            f.readLines().firstOrNull { it.startsWith("YOUTUBE_API_KEY=") }
                ?.substringAfter("=")?.trim()
        } else null
    }
    (fromProp ?: fromEnv ?: fromFile ?: "")
}

android {
    namespace = "com.example.ytdash"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ytdash"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "YOUTUBE_API_KEY", "\"$secretsKey\"")
        // Default production base host; overridable at runtime via the apiBaseUrl launch extra.
        buildConfigField("String", "DEFAULT_API_BASE", "\"https://www.googleapis.com\"")
        // Default authorized whitelist; overridable at runtime via the authorizedEmails launch extra.
        buildConfigField(
            "String",
            "DEFAULT_AUTHORIZED_EMAILS",
            "\"elaine.batista1105@gmail.com,edbpmc@gmail.com\""
        )
    }

    signingConfigs {
        // Sign release with the debug key so the harness can install a standalone release APK.
        getByName("debug")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.osmdroid)
    implementation(libs.play.services.auth)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
