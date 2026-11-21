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
// The library: in-app updates, in-app review and what's new, with no Compose dependency
include(":kit")
// Compose wrappers for apps written in Jetpack Compose
include(":kit-compose")
// The open-source licence list (AboutLibraries), on its own so apps with their own licence screen don't carry it.
// No Compose, so a View app's list doesn't name Compose libraries
include(":kit-oss")
// OpenSourceScreen, the licence list for Compose apps; depends on kit-oss
include(":kit-oss-compose")
// "More apps": your own apps' promo feed and its plain-View screen. On its own because it adds a
// <queries> block for launcher apps (to see which of your apps are installed), and apps without
// promos shouldn't be able to see the phone's apps
include(":kit-promo")
// MoreAppsRow, MoreAppsList and FeaturedPromoDialog for Compose apps; depends on kit-promo and kit-compose
include(":kit-promo-compose")
// "Sample App": every feature behind a button, with sample update and release-notes text
include(":sample")
