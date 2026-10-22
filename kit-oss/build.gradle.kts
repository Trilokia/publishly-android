plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "inc.trilokia.publishly.kit.oss"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // The theme, the top bar and the strings the View screen shares with the kit's other screens
    api(project(":kit"))
    // The licence list reader: `api`, because OpenSource.libraries() hands out its types. The app
    // applies the matching Gradle plugin (see README)
    api(libs.aboutlibraries.core)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    // AboutLibraries' own Material 3 list, for OpenSourceScreen's List layout
    implementation(libs.aboutlibraries.compose.m3)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "kit-oss"
            version = project.version.toString()
            afterEvaluate { from(components["release"]) }
            // JitPack otherwise fills <name> with "Trilokia/publishly-android" for every module, so licence
            // screens built from POMs list the same name twice
            pom {
                name.set("Publishly Kit OSS")
                description.set("Open-source licence list screen for apps that use Publishly Kit.")
                url.set("https://github.com/Trilokia/publishly-android")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/Trilokia/publishly-android")
                    connection.set("scm:git:https://github.com/Trilokia/publishly-android.git")
                    developerConnection.set("scm:git:ssh://git@github.com/Trilokia/publishly-android.git")
                }
            }
        }
    }
}
