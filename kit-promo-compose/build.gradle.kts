plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "inc.trilokia.publishly.kit.promo.compose"
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
    // Apps that add kit-promo-compose get kit-promo, kit-compose and kit with it. kit-compose for
    // PromptStyle, which FeaturedPromoDialog takes
    api(project(":kit-promo"))
    api(project(":kit-compose"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    // The check mark on the "Installed" label (the core set only, a few dozen icons)
    implementation(libs.compose.material.icons.core)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "kit-promo-compose"
            version = project.version.toString()
            afterEvaluate { from(components["release"]) }
            // JitPack otherwise fills <name> with "Trilokia/publishly-android" for every module, so licence
            // screens built from POMs list the same name twice
            pom {
                name.set("Publishly Kit Promo Compose")
                description.set("Jetpack Compose \"More apps\" row, list and featured-app dialog for apps that use Publishly Kit.")
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
