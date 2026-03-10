plugins {
    id("rivikauth.android.library")
    id("rivikauth.hilt")
    id("rivikauth.room")
}
android { namespace = "dev.rivikauth.core.database" }
dependencies {
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":core:model"))
    implementation("dev.rivik.fido:cable")
}
