import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Gemini API key lives in local.properties (never committed) so each dev/install
// can plug its own key without touching the repo. Falls back to empty string,
// which the parser stub interprets as "no key configured, show a friendly hint."
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val geminiKey: String = localProps.getProperty("geminiApiKey", "")

android {
    namespace = "com.subramanya.artha"
    // compileSdk = 35 to satisfy AndroidX core ≥ 1.15. targetSdk stays at 34
    // per CLAUDE.md (runtime behavior); compileSdk only affects which APIs
    // the compiler can see.
    compileSdk = 35

    defaultConfig {
        applicationId = "com.subramanya.artha"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Surface the API key to runtime code via BuildConfig — never logged.
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
}

dependencies {
    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM + UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room (entities arrive in Session 2; plugin wired now so the catalogue is ready)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (Session 4 onboarding persists userName here)
    implementation(libs.androidx.datastore.preferences)

    // kotlinx
    implementation(libs.kotlinx.datetime)

    // Charts
    implementation(libs.vico.compose.m3)

    // Google Generative AI — backs Phase 3's AI Quick Entry. Key from local.properties.
    implementation(libs.generative.ai)

    // Phase 5 hardening — biometric prompt + encrypted SharedPreferences + Fragment
    // (BiometricPrompt requires a FragmentActivity host).
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.fragment.ktx)

    // Unit testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    // Android stubs out org.json in unit tests (every method throws). Pull in the
    // real lib so the RuleSpec JSON codec can round-trip in plain JVM tests.
    testImplementation("org.json:json:20231013")

    // Instrumentation testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug-only tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
