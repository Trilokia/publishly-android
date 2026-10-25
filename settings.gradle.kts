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
// The open-source licence list (AboutLibraries), on its own so apps with their own licence screen don't carry it.
// No Compose, so a View app's list doesn't name Compose libraries
include(":kit-oss")
// OpenSourceScreen, the licence list for Compose apps; depends on kit-oss
include(":kit-oss-compose")
// "Sample App": every feature behind a button, with sample update and release-notes text
include(":sample")
