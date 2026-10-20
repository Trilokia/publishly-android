// Top-level build file: the plugins the modules use, applied in each module's own build file.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
}

// One version for both library modules. Tag the repo with the same text (v1.1.0) for JitPack.
allprojects {
    group = "inc.trilokia"
    version = "1.1.0"
}
