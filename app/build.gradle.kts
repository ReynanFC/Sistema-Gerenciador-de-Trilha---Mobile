import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Reads the Google Maps key from local.properties (not tracked by Git),
// so the key never ends up in version control.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.reynanfc.sistema_gerenciador_trilha"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.reynanfc.sistema_gerenciador_trilha"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Injects the key into AndroidManifest via ${MAPS_API_KEY}
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)

    // Google Maps SDK (map) and Fused Location Provider (GPS)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}