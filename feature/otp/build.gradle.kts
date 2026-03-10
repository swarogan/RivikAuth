plugins {
    id("rivikauth.android.compose")
    id("rivikauth.hilt")
}
android { namespace = "dev.rivikauth.feature.otp" }
dependencies {
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.material.icons)
    implementation(project(":core:model"))
    implementation(project(":core:crypto"))
    implementation(project(":core:database"))
}
