plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "inc.trilokia.publishly.kit.promo"
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
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // The theme, the top bar, the network check and the strings the promo screens share with the kit's other screens
    api(project(":kit"))
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    // Real org.json for JVM unit tests (Android's copy only works on a device)
    testImplementation(libs.org.json)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "kit-promo"
            version = project.version.toString()
            afterEvaluate { from(components["release"]) }
            // JitPack otherwise fills <name> with "Trilokia/publishly-android" for every module, so licence
            // screens built from POMs list the same name twice
            pom {
                name.set("Publishly Kit Promo")
                description.set("A \"More apps\" feed of your own apps, with a plain-View list and dialog. Adds launcher-app package visibility.")
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
