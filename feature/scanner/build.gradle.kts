plugins {
    id("rivikauth.android.compose")
    id("rivikauth.hilt")
}
android { namespace = "dev.rivikauth.feature.scanner" }
dependencies {
    implementation(libs.hilt.navigation.compose)
    implementation(libs.activity.compose)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.zxing.core)
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
}
