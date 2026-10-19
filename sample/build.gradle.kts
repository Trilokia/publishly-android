plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Writes res/raw/aboutlibraries.json from this module's dependencies at build time
    alias(libs.plugins.aboutlibraries.android)
}

android {
    namespace = "inc.trilokia.publishly.kit.sample"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "inc.trilokia.publishly.kit.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    // The sample ships the template feed (promo/apps.json) as an asset, for when the server has none yet
    sourceSets["main"].assets.directories.add("../promo")
}

dependencies {
    implementation(project(":kit-compose"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
