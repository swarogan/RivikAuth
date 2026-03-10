plugins {
    id("rivikauth.android.library")
    id("rivikauth.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
}
android { namespace = "dev.rivikauth.core.datastore" }
dependencies {
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":core:model"))
}
