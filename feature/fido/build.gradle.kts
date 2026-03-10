plugins {
    id("rivikauth.android.compose")
    id("rivikauth.hilt")
}
android { namespace = "dev.rivikauth.feature.fido" }
dependencies {
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.material.icons)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.zxing.core)
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:crypto"))
    implementation("dev.rivik.fido:cable")
    implementation(project(":service:ble"))
}
