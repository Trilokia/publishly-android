plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "inc.trilokia.publishly.kit"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // What JitPack / mavenLocal get: the release AAR plus a sources jar so IDEs show the comments
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // `api`, not `implementation`: apps use these SDKs' own types (AdView, AdSize, RewardItem...)
    api(libs.play.review)
    api(libs.play.app.update)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.kotlinx.coroutines.android)
    constraints {
        // The Play libraries pull in Fragment 1.1.0, and lintVitalRelease fails an app's release build
        // (PlayUpdates' registerForActivityResult) with any Fragment below 1.3.0
        implementation(libs.androidx.fragment)
    }
    testImplementation(libs.junit)
    // Real org.json for JVM unit tests (Android's copy only works on a device)
    testImplementation(libs.org.json)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "kit"
            version = project.version.toString()
            afterEvaluate { from(components["release"]) }
        }
    }
}
