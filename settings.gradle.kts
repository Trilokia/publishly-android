pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PublishlyKit"
// The library: in-app updates, in-app review, open-source licences and own-app promos, with no Compose dependency
include(":kit")
// Compose wrappers for apps written in Jetpack Compose
include(":kit-compose")
// A small app that uses all of them, with Google's test ad units
include(":sample")
